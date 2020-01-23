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
import org.cbioportal.staging.services.PublisherService;
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

    @Autowired
    private PublisherService publisherService;
    	
	@Value("${etl.working.dir:false}")
	private String etlWorkingDir;
	
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
        
		try {
			//Get studies from appropriate staging folder
			Map<String,Integer> validatedStudies = new HashMap<String,Integer>();
			for (String study : studyPaths.keySet()) {
				logger.info("Starting validation of study "+study);
				//Get the paths for the study and validate it
				File report = new File(studyPaths.get(study)+"/"+study+"_validation_report.html");
				File logFile = new File(studyPaths.get(study)+"/"+study+"_validation_log.txt");
				int exitStatus = validationService.validate(study, studyPaths.get(study).getAbsolutePath()+"/staging", report, logFile, date);
				
                //Put report and log file in the share location
                String validationLogPath = publisherService.publish(logFile, date);
                String reportPath = publisherService.publish(report, date);
                filesPaths.put(study+" validation log", validationLogPath);
                filesPaths.put(study+" validation report", reportPath);

				
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
