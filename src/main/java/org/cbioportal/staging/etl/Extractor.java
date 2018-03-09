/*
 * Copyright (c) 2018 The Hyve B.V.
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.cbioportal.staging.etl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.services.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Component
class Extractor {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);

	@Autowired
	EmailService emailService;

	@Value("${etl.working.dir:java.io.tmpdir}")
	private String etlWorkingDir;

	@Value("${time.attempt:5}")
	private Integer timeAttempt;

	@Autowired
	private ResourcePatternResolver resourcePatternResolver;

	@Value("${scan.location}")
	private String scanLocation;

	private void copyResource(String resourceName, String resourcePath, File destinationPath) throws IOException {
		logger.info("Copying resource "+resourceName);
		InputStream is = resourcePatternResolver.getResource(resourcePath).getInputStream();
		Files.copy(is, Paths.get(destinationPath+"/"+resourceName));
		logger.info("File "+resourceName+" has been copied successfully!");
		is.close();
	}

	private Map<String, List<String>> parseYaml(Resource resource) throws IOException {
		InputStream is = resource.getInputStream();
		Yaml yaml = new Yaml();
		@SuppressWarnings("unchecked")
		Map<String, List<String>> result = (Map<String, List<String>>) yaml.load(is);
		return result;
	}

	/**
	 * Function that parses the yaml file and copies the files specified in the yaml to a folder in the etlWorkingDir
	 * with the job id. Inside this job id folder, files will be grouped in different folders by study, each of these
	 * folders will be named by the study name. If a file specified in the yaml is not found, it will try it 5 times
	 * (time between attempts is configurable). If after 5 times, the file is still not found, an email will be sent
	 * to the administrator.
	 * 
	 * @param indexFile: yaml file
	 * @return A pair with an integer (job id) and a list of strings (names of studies successfully copied)
	 * @throws InterruptedException
	 * @throws IOException 
	 */
	Pair<Integer, List<String>> run(Resource indexFile) throws InterruptedException, IOException {
		logger.info("Extract step: downloading files to " + etlWorkingDir);
		//Parse the indexFile and download all referred files to the working directory.
		Pair<Integer, List<String>> data;
		List<String> studiesLoaded = new ArrayList<String>();
		List<String> studiesWithErrors = new ArrayList<String>();
		Map<String, ArrayList<String>> filesNotFound = new HashMap<String, ArrayList<String>>();
		Map<String, Integer> errors = new HashMap<String, Integer>();
		Integer id = 0;
		File idPath = new File(etlWorkingDir+"/"+id);
		while (idPath.exists()) {
			id ++;
			idPath = new File(etlWorkingDir+"/"+id);
		}
		if (!idPath.exists()) {
			idPath.mkdirs();
		}
		try {
			Map<String, List<String>> parsedYaml = parseYaml(indexFile);
			if (parsedYaml == null) {
				throw new IOException("Yaml file found to be empty");
			}
			for (Entry<String, List<String>> entry : parsedYaml.entrySet()) {
				File destinationPath = new File(idPath+"/"+entry.getKey());
				if (!destinationPath.exists()) {
					destinationPath.mkdirs();
				}
				errors.put(entry.getKey(), 0); //Add error counter for the current study
				for (String filePath : entry.getValue() ) {
					String[] fullFilePath = filePath.split("/");
					//logger.info("FULL FILE PATH: "+scanLocation);
					String file = fullFilePath[fullFilePath.length-1];
					filePath = scanLocation+"/"+filePath;
					int attempt = 1;
					while (attempt <= 5) {
						try {
							copyResource(file, filePath, destinationPath);
							break;
						}
						catch (IOException f) {
							logger.error("File "+file+" of study "+entry.getKey()+" does not exist in path: "+filePath+". Tried "+attempt+" times.");
							attempt ++;
							if (attempt == 5) {
								logger.error("Tried 5 times. File "+file+" not found, quitting...");
								errors.put(entry.getKey(), errors.get(entry.getKey())+1);
								if (filesNotFound.get(entry.getKey()) == null) {
									ArrayList<String> newList = new ArrayList<String>();
									newList.add(file);
									filesNotFound.put(entry.getKey(), newList);
								} else {
									filesNotFound.get(entry.getKey()).add(file);
								}
							} else {
								TimeUnit.MINUTES.sleep(timeAttempt);
							}
						}
					}
				}
			}
			for (String study : errors.keySet()) {
				if (errors.get(study) == 0) {
					studiesLoaded.add(study);
				} else {
					studiesWithErrors.add(study);
				}
			}
			if (!studiesWithErrors.isEmpty()) {
				try {
					emailService.emailStudyFileNotFound(filesNotFound, timeAttempt);
				} catch (Exception e) {
					logger.error("The email could not be sent due to the error specified below.");
					e.printStackTrace();
				}
			}
		}
		catch (ClassCastException e) {
			logger.error("The yaml file given has an incorrect format. Please check the file.", e);
			try {
				emailService.emailGenericError("The yaml file given has an incorrect format. Please, check the file.", e);
			} catch (Exception e1) {
				logger.error("The email could not be sent due to the error specified below.");
				e1.printStackTrace();
			}
		}
		catch (IOException e) {
			logger.error("The yaml file was empty or not found at "+indexFile);
			try {
				emailService.emailGenericError("The yaml file was not found.", e);
			} catch (Exception e1) {
				logger.error("The email could not be sent due to the error specified below.");
				e1.printStackTrace();
			}
		}
		data = Pair.of(id, studiesLoaded);
		logger.info("Extractor step finished");
		return data;
	}
}
