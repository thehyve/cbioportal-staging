/*
 * Copyright (c) 2018 The Hyve B.V.
 * This code is licensed under the GNU Affero General Public License,
 * version 3, or (at your option) any later version.
 */
package org.cbioportal.staging.app;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

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
	
	@Scheduled(cron = "${scan.cron}")
	public boolean scan() {
		try {
			logger.info("Fixed Rate Task :: Execution Time - {}", dateTimeFormatter.format(LocalDateTime.now()) );
			nrIterations++;
			logger.info("Scanning location for new staging files: " + scanLocation);
			Resource[] allFilesInFolder =  this.resourcePatternResolver.getResources(scanLocation + "/list_of_studies*.yaml");
			logger.info("Found "+ allFilesInFolder.length + " index files");
	
			if (allFilesInFolder.length == 0)
				return false;
	
			Resource mostRecentFile = allFilesInFolder[0];
			for (Resource resource : allFilesInFolder) {
	
				if (resource.lastModified() > mostRecentFile.lastModified()) {
					mostRecentFile = resource;
				}
			}
			logger.info("Selected most recent one: "+ mostRecentFile.getFilename());
	
			// trigger ETL process:
			etlProcessRunner.run(mostRecentFile);
	
	
			//check if nrRepeats reached the configured max: 
			if (scanIterations != -1 && nrIterations >= scanIterations) {
				logger.info("==>>>>> Reached configured number of iterations (" + scanIterations + "). Exiting... <<<<<==");
				System.exit(0);
			}
			
		} catch (AmazonS3Exception e) {
			logger.error("The bucket cannot be reached. Please check the scan.location provided in the application.properties.");
			scheduledScannerService.stopApp();
		} catch (IOException e) {
			scheduledScannerService.stopApp();
		} catch (Exception e) {
			logger.error("An error not expected occurred. Stopping process...");
			try {
				emailService.emailGenericError("An error not expected occurred. Stopping process...", e);
				scheduledScannerService.stopApp();
			} catch (Exception e1) {
				logger.error("The email could not be sent due to the error specified below.");
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		//return true if a process was triggered
		return true;
	}

}
