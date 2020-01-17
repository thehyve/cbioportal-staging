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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.services.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;


/* 
	Copies all files from scan.location to the etl.working.directory.
*/
// TODO: this class is only needed when files are copied from a remote
// source to be ETLed on the local server. This class may be removed in 
// certain cases.
// TODO: this class is only relevant ATM when working in S3 context since 
// the other environment 'local filesystem' does not require copying. 
@Component
class Extractor {
	private static final Logger logger = LoggerFactory.getLogger(Extractor.class);

	@Autowired
    private EmailService emailService;
    
	@Value("${etl.working.dir:${java.io.tmpdir}}")
	private File etlWorkingDir;

	@Value("${scan.retry.time:5}")
	private Integer timeRetry;

	@Autowired
	private ResourcePatternResolver resourcePatternResolver;

	@Value("${scan.location}")
	private String scanLocation;

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
	 * @throws ConfigurationException 
	 */
	Map<String, File> run(Resource indexFile) throws InterruptedException, IOException, ConfigurationException {
		//validate:
		if (etlWorkingDir.toString().startsWith("file:") || (etlWorkingDir.toString().startsWith("s3:"))) {
			throw new ConfigurationException("Invalid configuration: configuration option `etl.working.dir` should point to "
					+ "a local folder and not to a location (so *not* starting with file:/ or s3:/). "
					+ "Configuration found: etl.working.dir=" + etlWorkingDir);
		}
		logger.info("Extract step: downloading files to " + etlWorkingDir);

		//Parse the indexFile and download all referred files to the working directory.
		Map<String, File> studiesLoadedPath = new HashMap<String, File>();
		
		//List<String> studiesLoaded = new ArrayList<String>();
		List<String> studiesWithErrors = new ArrayList<String>();
		
		Map<String, ArrayList<String>> filesNotFound = new HashMap<String, ArrayList<String>>();
		String date = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
		File idPath = new File(etlWorkingDir+"/"+date);
		
		//make new working sub-dir for this new iteration of the extraction step:
		ensureDirs(idPath);
		
		try {
			Map<String, List<String>> parsedYaml = parseYaml(indexFile);
			if (parsedYaml == null) {
				throw new IOException("Yaml file found to be empty");
			}
			for (Entry<String, List<String>> entry : parsedYaml.entrySet()) {

				String studyDir = idPath+"/"+entry.getKey();
				//errors.put(entry.getKey(), 0); //Add error counter for the current study
				String basePath = getBasePath(entry);

				//extract each file and place it in jobid/studyid/ folder:
				for (String filePath : entry.getValue() ) {
					//this is what the relative file path should be inside local copy of the study folder:
					String filePathInsideDestination = filePath.substring(basePath.length(), filePath.length());
					String fullDestinationPath = studyDir + "/" + filePathInsideDestination;
					//make necessary directory structure:
					ensureDirs(new File(fullDestinationPath.substring(0, fullDestinationPath.lastIndexOf("/"))));
					String fullOriginalFilePath = scanLocation+"/"+filePath;
					int attempt = 1;
					while (attempt <= 5) {
						try {
							copyResource(fullDestinationPath, fullOriginalFilePath);
							break;
						}
						catch (IOException f) {
							logger.error("File "+filePathInsideDestination+" of study "+entry.getKey()+" does not exist in path: "+fullOriginalFilePath+". Tried "+attempt+" times.");
							logger.debug("Error details: ", f);
							attempt ++;
							if (attempt == 5) {
								logger.error("Tried 5 times. File "+fullOriginalFilePath+" not found, quitting...");
								studiesWithErrors.add(entry.getKey());
								if (filesNotFound.get(entry.getKey()) == null) {
									ArrayList<String> newList = new ArrayList<String>();
									newList.add(fullOriginalFilePath);
									filesNotFound.put(entry.getKey(), newList);
								} else {
									filesNotFound.get(entry.getKey()).add(fullOriginalFilePath);
								}
							} else {
								TimeUnit.MINUTES.sleep(timeRetry);
							}
						}
					}
				}
				
			}
			for (Entry<String, List<String>> entry : parsedYaml.entrySet()) {
				if (!studiesWithErrors.contains(entry.getKey())) {
					studiesLoadedPath.put(entry.getKey(), new File(idPath+"/"+entry.getKey()));
				}
			}
			if (!studiesWithErrors.isEmpty()) {
				try {
					emailService.emailStudyFileNotFound(filesNotFound, timeRetry);
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
		catch (ConfigurationException e) {
			logger.error("The yaml file given has an invalid contents. Please check the file.", e);
			try {
				emailService.emailGenericError("The yaml file given has an invalid contents. Please, check the file.", e);
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
		logger.info("Extractor step finished");
		return studiesLoadedPath;
	}

	private void copyResource(String destinationPath, String resourcePath) throws IOException {
		logger.info("Copying resource from " + resourcePath + " to "+ destinationPath);
		InputStream is = resourcePatternResolver.getResource(resourcePath).getInputStream();
		Files.copy(is, Paths.get(destinationPath));
		logger.info("File has been copied successfully to "+ destinationPath);
		is.close();
	}

	private Map<String, List<String>> parseYaml(Resource resource) throws IOException {
		InputStream is = resource.getInputStream();
		Yaml yaml = new Yaml();
		@SuppressWarnings("unchecked")
		Map<String, List<String>> result = (Map<String, List<String>>) yaml.load(is);
		return result;
	}

	private void ensureDirs(File path) {
		if (!path.exists()) {
			path.mkdirs();
		}
    }

	/**
	 * This method gets the "base path" of all entries. I.e. it assumes
	 * all entries share a common parent path on the S3 or other resource folder
	 * where they are originally shared. So for the following list of files configured in the 
	 * list of studies yaml as below:
	 *   study1:
     *    - folder/study1path/fileA.txt
     *    - folder/study1path/fileB.txt
     *    - folder/study1path/mafs/maf1.maf
     *    - folder/study1path/mafs/mafn.maf
     * 
     * this method will return "folder/study1path".
	 * @throws ConfigurationException 
	 */
	private String getBasePath(Entry<String, List<String>> entry) throws ConfigurationException {
		int shortest = Integer.MAX_VALUE;
		String shortestPath = "";
		for (String filePath : entry.getValue() ) {
			if (filePath.length() < shortest) {
				shortest = filePath.length();
				shortestPath = filePath;
			}
		}
		logger.debug("Shortest path: " + shortestPath);
		String result = "";
		if (shortestPath.indexOf("/") != -1) {
			result = shortestPath.substring(0, shortestPath.lastIndexOf("/"));
		}
		//validate if main assumption is correct (i.e. all paths contain the shortest path):
		for (String filePath : entry.getValue() ) {
			if (!filePath.contains(result)) {
				throw new ConfigurationException("Study configuration contains mixed locations. Not allowed. E.g. "
						+ "locations: "+ filePath + " and " + result + "/...");
			}
		}
		return result;
	}
}
