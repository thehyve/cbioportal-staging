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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.services.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class LocalExtractor {
	private static final Logger logger = LoggerFactory.getLogger(LocalExtractor.class);

	@Autowired
	private EmailService emailService;

	@Value("${etl.working.dir:false}")
	private String etlWorkingDir;

	@Value("${scan.retry.time:5}")
	private Integer timeRetry;

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
	Map<String, File> extractInWorkingDir(ArrayList<File> directories, Integer id) throws InterruptedException, IOException, ConfigurationException {
        //validate:
		if (etlWorkingDir.startsWith("file:") || (etlWorkingDir.startsWith("s3:"))) {
			throw new ConfigurationException("Invalid configuration: configuration option `etl.working.dir` should point to "
					+ "a local folder and not to a location (so *not* starting with file:/ or s3:/). "
					+ "Configuration found: etl.working.dir=" + etlWorkingDir);
		}
		logger.info("Extract step: downloading files to " + etlWorkingDir);
		//Parse the indexFile and download all referred files to the working directory.
		Map<String, File> studiesLoadedPaths = new HashMap<String, File>();
		List<String> studiesWithErrors = new ArrayList<String>();
        Map<String, ArrayList<String>> filesNotFound = new HashMap<String, ArrayList<String>>();
		File idPath = new File(etlWorkingDir+"/"+id);
		//make new working sub-dir for this new iteration of the extraction step:
		ensureDirs(idPath);
		try {
			for (File directory : directories) {
				String studyName = directory.getName();
				File originDir = directory;
				File destinationDir = new File(idPath+"/"+studyName);
				try {
				    FileUtils.copyDirectory(originDir, destinationDir);
				    studiesLoadedPaths.put(studyName, destinationDir);
				} catch (IOException e) {
					studiesWithErrors.add(studyName);
					logger.error("There has been an error when copying the directory "+directory.getAbsolutePath(), e);
				    e.printStackTrace();
				}
			}
			
			if (!studiesWithErrors.isEmpty()) {
				try {
					emailService.emailStudyFileNotFound(filesNotFound, timeRetry);
				} catch (Exception e) {
					logger.error("The email could not be sent due to the error specified below.", e);
					e.printStackTrace();
				}
			}
		}
		catch (Exception e) {
			logger.error("There has been an error when extracting the files.", e);
			try {
				emailService.emailGenericError("There has been an error when extracting the files.", e);
			} catch (Exception e1) {
				logger.error("The email could not be sent due to the error specified below.");
				e1.printStackTrace();
			}
		}

		logger.info("Extractor step finished");
		return studiesLoadedPaths;
    }
    
    Map<String, File> extractWithoutWorkingDir(ArrayList<File> directories) throws InterruptedException, IOException, ConfigurationException {
		//Parse the indexFile and download all referred files to the working directory.
		Map<String, File> studiesLoadedPaths = new HashMap<String, File>();
        for (File directory : directories) {
            String studyName = directory.getName();
            studiesLoadedPaths.put(studyName, directory);
        }
		return studiesLoadedPaths;
	}

	private void ensureDirs(File path) {
		if (!path.exists()) {
			path.mkdirs();
		}
    }
    
    Integer getNewId(File folderPath) {
        Integer id = 0;
        File idPath = new File(folderPath+"/"+id);
		while (idPath.exists()) {
            id ++;
            idPath = new File(folderPath+"/"+id);
        }
        return id;
    }

}
