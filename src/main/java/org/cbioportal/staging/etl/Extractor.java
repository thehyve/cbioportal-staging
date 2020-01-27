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
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.pivovarit.function.ThrowingFunction;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ExtractionException;
import org.cbioportal.staging.services.EmailService;
import org.cbioportal.staging.services.resource.ResourceUtils;
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

	@Autowired
    private ResourceUtils utils;

	@Value("${scan.location}")
	private String scanLocation;

	public Map<String,File> run(Map<String, Resource[]> resources) throws ExtractionException {

		Map<String,File> out = new HashMap<String,File>();

		try {

			if (! etlWorkingDir.exists()) {
				throw new ExtractionException("etl.working.dir does not exist on the local file system: " + etlWorkingDir);
			}
			if (etlWorkingDir.isFile()) {
				throw new ExtractionException("etl.working.dir points to a file on the local file system, but should point to a directory.: " + etlWorkingDir);
			}

			// TODO make abstraction of organization of local working dir
			String workingDir = etlWorkingDir.getAbsolutePath() + "/" + utils.getTimeStamp("yyyyMMdd-HHmmss") + "/";

			Map<String, ArrayList<String>> filesNotFound = new HashMap<String, ArrayList<String>>();

			for (Entry<String,Resource[]> studyResources: resources.entrySet()) {

				String studyId = studyResources.getKey();
				String studyDir = workingDir + studyId + "/";
				String remoteBasePath = getBasePathResources(studyResources.getValue());

				List<String> filesWithErrors = new ArrayList<>();
				for (Resource remoteResource: studyResources.getValue()) {

					String fullOriginalFilePath = remoteResource.getURI().toString();
					String remoteFilePath = fullOriginalFilePath.replaceFirst(remoteBasePath, "");

					Resource localResource = attemptCopyResource(studyDir, remoteResource, remoteFilePath);
					if (localResource == null) {
						filesWithErrors.add(fullOriginalFilePath);
					}
				}

				// register successfully extracted study
				if (filesWithErrors.isEmpty()) {
					out.put(studyId, new File(studyDir));
				}

			}

			// when there are errors send summary with email
			if (!filesNotFound.isEmpty()) {
				try {
					emailService.emailStudyFileNotFound(filesNotFound, timeRetry);
				} catch (Exception e) {
					logger.error("The email could not be sent due to the error specified below.");
					e.printStackTrace();
				}
			}

		} catch (IOException e) {
			throw new ExtractionException("Cannot access working ELT directory.", e);
		} catch (ConfigurationException e) {
			throw new ExtractionException(e.getMessage(), e);
		} catch (InterruptedException e) {
			throw new ExtractionException("Timeout for resource downloads was interrupted.", e);
		}

		logger.info("Extractor step finished");
        return out;
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
				String basePath = getBasePath(entry.getValue());

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
							copyResource(fullDestinationPath, fullOriginalFilePath, filePathInsideDestination);
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

	private Resource attemptCopyResource(String destination, Resource resource, String remoteFilePath) throws InterruptedException {
		int i = 1;
		int times = 5;
		Resource r = null;
		while (i++ <= times) {
			try {
				r = copyResource(destination, resource, remoteFilePath);
				break;
			} catch (IOException f) {
				if (i < times) {
					TimeUnit.MINUTES.sleep(timeRetry);
				}
			}
		}
		return r;
	}

	private Resource copyResource(String destination, String remoteFullPath, String remoteFilePath) throws IOException {
		return copyResource(destination, resourcePatternResolver.getResource(remoteFullPath), remoteFilePath);
	}

	private Resource copyResource(String destination, Resource resource, String remoteFilePath) throws IOException {
		logger.info("Copying resource " + resource.getURL() + " to "+ destination);
		InputStream inputStream = resource.getInputStream();
		String fullDestinationPath = destination + remoteFilePath;
		ensureDirs(fullDestinationPath.substring(0, fullDestinationPath.lastIndexOf("/")));
		Files.copy(inputStream, Paths.get(fullDestinationPath));
		logger.info("File has been copied successfully to "+ destination);
		inputStream.close();
		return resourcePatternResolver.getResource(fullDestinationPath);
	}

	// private Resource copyResource(Resource destination, Resource resource, String remoteFilePath) throws IOException {
	// 	String fullDestinationPath = destination.getURL().toString() + remoteFilePath;
	// 	logger.info("Copying resource " + resource.getURL() + " to "+ destination.getURL().toString());
	// 	ensureDirs(fullDestinationPath.substring(0, fullDestinationPath.lastIndexOf("/")));
	// 	InputStream inputStream = resource.getInputStream();
	// 	Files.copy(inputStream, Paths.get(destination.getURI()));
	// 	logger.info("File has been copied successfully to "+ destination.getURL().toString());
	// 	inputStream.close();
	// 	return resourcePatternResolver.getResource(fullDestinationPath);
	// }

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

	private void ensureDirs(String path) throws IOException {
		ensureDirs(new File(path));
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
	private String getBasePath(List<String> paths) throws ConfigurationException {
		int shortest = Integer.MAX_VALUE;
		String shortestPath = "";
		for (String filePath : paths) {
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
		for (String filePath : paths) {
			if (!filePath.contains(result)) {
				throw new ConfigurationException("Study configuration contains mixed locations. Not allowed. E.g. "
						+ "locations: "+ filePath + " and " + result + "/...");
			}
		}
		return result;
	}



	private String getBasePathResources(Resource[] resources) throws ConfigurationException {
		List<String> paths = Stream.of(resources)
			.map(ThrowingFunction.unchecked(e -> e.getURL().toString()))
			.collect(Collectors.toList());
		return getBasePath(paths);
	}

}
