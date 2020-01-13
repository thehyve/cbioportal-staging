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
import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.exceptions.ValidatorException;
import org.cbioportal.staging.services.EmailService;
import org.cbioportal.staging.services.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Validator {
	private static final Logger logger = LoggerFactory.getLogger(Validator.class);
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
    private ValidationService validationService;
    	
	@Value("${etl.working.dir:false}")
	private String etlWorkingDir;
	
	@Value("${central.share.location}")
	private String centralShareLocation;
	
	@Value("${central.share.location.portal:}")
	private String centralShareLocationPortal;
	
	@Value("${validation.level:ERROR}")
	private String validationLevel;
	
	boolean hasStudyPassed(String study, String validationLevel, int exitStatus) {
		if (validationLevel.equals("WARNING")) { //Load studies with no warnings and no errors
			if (exitStatus == 0) {
				return true;
			}
			return false;
		} else if (validationLevel.equals("ERROR")) { //Load studies with only no errors
			if (exitStatus == 0 || exitStatus == 3) {
				return true;
			}
			return false;
		} else {
			throw new IllegalArgumentException("Validation level should be WARNING or ERROR. Please check the application.properties.");
		}
	}
	
	Map<String, File> validate(String date, Map<String, File> studyPaths, Map<String, String> filesPaths) throws IllegalArgumentException, Exception {
		Map<String, File> studiesPassed = new HashMap<String, File>();
		if (centralShareLocationPortal.equals("")) {
			centralShareLocationPortal = centralShareLocation;
		}
		try {
			//Get studies from appropriate staging folder
			Map<String,Integer> validatedStudies = new HashMap<String,Integer>();
			for (String study : studyPaths.keySet()) {
				logger.info("Starting validation of study "+study);
				//Get the paths for the study and validate it
				String reportName = study+"_validation_report.html";
				String reportPath = studyPaths.get(study).getAbsolutePath()+"/"+reportName;
				File logFile = new File(studyPaths.get(study)+"/"+study+"_validation_log.txt");
				int exitStatus = validationService.validate(study, studyPaths.get(study).getAbsolutePath()+"/staging", reportPath, logFile, date);
				filesPaths.put(study+" validation log", centralShareLocationPortal+"/"+date+"/"+logFile.getName());
                filesPaths.put(study+" validation report", centralShareLocationPortal+"/"+date+"/"+reportName);
				
				//Put report and log file in the share location
                //First, make the "id" dir in the share location if it is local
				String centralShareLocationPath = centralShareLocation+"/"+date;
				if (!centralShareLocationPath.startsWith("s3:")) {
					File cslPath = new File(centralShareLocation+"/"+date);
					if (centralShareLocationPath.startsWith("file:")) {
						cslPath = new File(centralShareLocationPath.replace("file:", ""));
					}
					logger.info("PATH TO BE CREATED: "+cslPath.getAbsolutePath());
					if (!cslPath.exists()) {
						cslPath.mkdirs();
					}
				}
				validationService.copyToResource(new File(reportPath), centralShareLocationPath);
				validationService.copyToResource(logFile, centralShareLocationPath);
				
				//Check if study has passed the validation threshold
				if (hasStudyPassed(study, validationLevel, exitStatus)) {
					logger.info("Study " + study + " has PASSED validation");
					studiesPassed.put(study, studyPaths.get(study));
				} else {
					logger.warn("Study " + study + " has FAILED validation");
				}

				//Add validation result for the email validation report
				validatedStudies.put(study, exitStatus);

				logger.info("Validation of study "+study+" finished.");
			}
			emailService.emailValidationReport(validatedStudies, validationLevel, filesPaths);
		} catch (ValidatorException e) {
			//tell about error, continue with next study
			logger.error(e.getMessage()+". The app will skip this study.");
			try {
				emailService.emailGenericError(e.getMessage()+". The app will skip this study.", e);
			} catch (Exception e1) {
				logger.error("The email could not be sent due to the error specified below.");
				e1.printStackTrace();
			}
		}
		return studiesPassed;
	}
}
