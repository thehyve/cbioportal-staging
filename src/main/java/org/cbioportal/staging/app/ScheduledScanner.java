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
package org.cbioportal.staging.app;

import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cbioportal.staging.etl.ETLProcessRunner;
import org.cbioportal.staging.services.EmailService;
import org.cbioportal.staging.services.ScheduledScannerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import com.amazonaws.services.s3.model.AmazonS3Exception;

/**
 * Main app class to run the staging and loading process as a background service
 *
 */


@Component
public class ScheduledScanner 
{
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

	@Value("${scan.location}")
	private String scanLocation;
	
	@Value("${scan.extract.folders: *}")
	private String scanExtractFolders;

	@Value("${scan.cron.iterations:-1}")
	private Integer scanIterations;
	private int nrIterations = 0;

	@Autowired
	private ResourcePatternResolver resourcePatternResolver;
	@Autowired
	private ETLProcessRunner etlProcessRunner;
	@Autowired
	EmailService emailService;
	
	@Autowired
	ScheduledScannerService scheduledScannerService;
	
	private String S3PREFIX = "s3:";
	
	@Scheduled(cron = "${scan.cron}")
	public boolean scan() {
		try {
			logger.info("Fixed Rate Task :: Execution Time - {}", dateTimeFormatter.format(LocalDateTime.now()) );
			nrIterations++;
			if (scanLocation.startsWith(S3PREFIX)) {
				logger.info("Scanning location for the newest yaml file: " + scanLocation);
				Resource[] allFilesInFolder =  this.resourcePatternResolver.getResources(scanLocation + "/list_of_studies*.yaml");
				logger.info("Found "+ allFilesInFolder.length + " index files");
		
				if (allFilesInFolder.length == 0) {
					checkIfShouldExit(nrIterations, scanIterations);
					return false;
				}
		
				Resource mostRecentFile = allFilesInFolder[0];
				for (Resource resource : allFilesInFolder) {
		
					if (resource.lastModified() > mostRecentFile.lastModified()) {
						mostRecentFile = resource;
					}
				}
				logger.info("Selected most recent one: "+ mostRecentFile.getFilename());
		
				// trigger ETL process:
				etlProcessRunner.run(mostRecentFile);
			} else {
				logger.info("Scanning location to find folders: " + scanLocation);
				List<String> subsetToExtract = Arrays.asList(scanExtractFolders.split(","));
				Resource scanLocationResource = resourcePatternResolver.getResource(scanLocation);
				File scanLocationPath = scanLocationResource.getFile();
				ArrayList<File> directories = new ArrayList<File>();
				for (File file : scanLocationPath.listFiles()) {
					if (file.isDirectory()) {
						if (scanExtractFolders.trim().equals("*") || subsetToExtract.contains(file.getName())) {
							logger.info("Folder found: "+file.getName());
							directories.add(file);
						} else {
							logger.info("Folder skipped: "+file.getName());
						}
					}
				}
				if (directories.size() == 0) {
					checkIfShouldExit(nrIterations, scanIterations);
					return false;
				}

				// trigger ETL process:
				etlProcessRunner.run(directories);
			}
	
			checkIfShouldExit(nrIterations, scanIterations);
			
		} catch (AmazonS3Exception e) {
			try {
				logger.error("The bucket cannot be reached. Please check the scan.location provided in the application.properties.");
				emailService.emailGenericError("The bucket cannot be reached. Please check the scan.location provided in the application.properties.", e);
			} catch (Exception e1) {
				logger.error("The email could not be sent due to the error specified below.");
				e1.printStackTrace();
			} finally {
				e.printStackTrace();
				scheduledScannerService.stopApp();
			}
		} catch (Exception e) {
			try {
				logger.error("An error not expected occurred. Stopping process... Error found: " + e);
				emailService.emailGenericError("An error not expected occurred. Stopping process... Error found: ", e);
			} catch (Exception e1) {
				logger.error("The email could not be sent due to the error specified below.");
				e1.printStackTrace();
			} finally {
				e.printStackTrace();
				scheduledScannerService.stopApp();
			}
		}
		//return true if a process was triggered
		return true;
	}

	/**
	 * Checks if nrIterations reached the configured max and
	 * triggers System.exit if this is the case.
	 * 
	 * @param nrIterations
	 * @param max
	 */
	private void checkIfShouldExit(int nrIterations, int max) {
		if (max != -1 && nrIterations >= max) {
			logger.info("==>>>>> Reached configured number of iterations (" + max + "). Exiting... <<<<<==");
			System.exit(0);
		}
	}

}
