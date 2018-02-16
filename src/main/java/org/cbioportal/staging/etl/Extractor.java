/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
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

	@Value("${etl.working.dir:file:///tmp}")
	private File etlWorkingDir;
	
	@Autowired
    private ResourcePatternResolver resourcePatternResolver;

	@Value("${scan.location}")
	private String scanLocation;
	
	private void copyResource(String resourceName, String resourcePath, File destinationPath) throws IOException {
		InputStream is = resourcePatternResolver.getResource(resourcePath+"/"+resourceName).getInputStream();
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

	Pair<Integer, List<String>> run(Resource indexFile) throws InterruptedException {
		logger.info("Extract step: downloading files to " + etlWorkingDir.getAbsolutePath());
		//Parse the indexFile and download all referred files to the working directory.
		Pair<Integer, List<String>> data;
		List<String> studiesLoaded = new ArrayList<String>();
		List<String> studiesWithErrors = new ArrayList<String>();
		Map<String, ArrayList<String>> filesNotFound = new HashMap<String, ArrayList<String>>();
		Map<String, Integer> errors = new HashMap<String, Integer>();
		Integer id = 0;
		File idPath = new File(etlWorkingDir.toPath()+"/"+id);
		while (idPath.exists()) {
			id ++;
			idPath = new File(etlWorkingDir.toPath()+"/"+id);
		}
		if (!idPath.exists()) {
			idPath.mkdirs();
		}
		try {
			Map<String, List<String>> parsedYaml = parseYaml(indexFile);
			for (Entry<String, List<String>>entry : parsedYaml.entrySet()) {
				File destinationPath = new File(idPath+"/"+entry.getKey());
				if (!destinationPath.exists()) {
					destinationPath.mkdirs();
				}
				errors.put(entry.getKey(), 0); //Add error counter for the current study
				for (String file : entry.getValue() ) {
					logger.info("Copying file "+file+" from study "+entry.getKey());
					String originPath = scanLocation+"/"+entry.getKey();
					try {
						copyResource(file, originPath, destinationPath);
					}
					catch (IOException e) {
						Integer attempt = 1;
						logger.error("The file "+file+" from the study "+entry.getKey()+" does not exist in "+originPath+"/"+file+". Tried "+attempt+" times.");
						attempt ++;
						while (attempt <= 5) {
							TimeUnit.SECONDS.sleep(1); //TODO: Change to: TimeUnit.MINUTES.sleep(5); //Wait 5 minutes
							try {
								copyResource(file, originPath, destinationPath);
							}
							catch (IOException f) {
								logger.error("The file "+file+" from the study "+entry.getKey()+" does not exist in "+originPath+"/"+file+". Tried "+attempt+" times.");
							}
							attempt ++;
							if (attempt == 5) {
								errors.put(entry.getKey(), errors.get(entry.getKey())+1);
								if (filesNotFound.get(entry.getKey()) == null) {
									ArrayList<String> newList = new ArrayList<String>();
									newList.add(file);
									filesNotFound.put(entry.getKey(), newList);
								} else {
									filesNotFound.get(entry.getKey()).add(file);
								}
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
					emailService.emailStudyFileNotFound(filesNotFound);
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
			logger.error("The yaml file was not found.");
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
