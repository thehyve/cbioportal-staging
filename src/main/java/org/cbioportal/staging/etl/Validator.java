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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.exceptions.ValidatorException;
import org.cbioportal.staging.services.EmailService;
import org.cbioportal.staging.services.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class Validator {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);
	
	@Autowired
	EmailService emailService;
	
	@Autowired
	ValidationService validationService;
	
	@Value("${etl.working.dir:java.io.tmpdir}")
	private String etlWorkingDir;
	
	@Value("${central.share.location}")
	private Resource centralShareLocation;
	
	@Value("${validation.level:ERROR}")
	private String validationLevel;
	
	@Value("${portal.home}")
	private String portalHome;
	
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
	
	ArrayList<String> validate(Integer id, List<String> studies) throws Exception {
		ArrayList<String> studiesPassed = new ArrayList<String>();
		try {
			//Get studies from appropriate staging folder
			File originPath = new File(etlWorkingDir+"/"+id+"/staging");
			Map<Pair<String,String>,Integer> validatedStudies = new HashMap<Pair<String,String>,Integer>();
			for (String study : studies) {
				logger.info("Starting validation of study "+study);
				//Get the paths for the study and validate it
				File studyPath = new File(originPath+"/"+study);
				String timeStamp = new SimpleDateFormat("yyyy_MM_dd_HH.mm.ss").format(new Date());
				String reportName = study+"_validation_report_"+timeStamp+".html";
				String reportPath = etlWorkingDir+"/"+id+"/"+reportName;
				String logFileName = study+"_validation_log_"+timeStamp+".log";
				File logFile = new File(etlWorkingDir+"/"+id+"/"+logFileName);
				int exitStatus = validationService.validate(study, studyPath.getAbsolutePath(), reportPath, logFile);
				
				//Put report and log file in the share location
				validationService.copyToResource(reportName, reportPath, centralShareLocation);
				validationService.copyToResource(logFileName, logFile.getAbsolutePath(), centralShareLocation);
				
				//Check if study has passed the validation threshold
				if (hasStudyPassed(study, validationLevel, exitStatus)) {
					studiesPassed.add(study);
				}

				//Add validation result for the email validation report
				Pair<String, String> studyData = Pair.of(reportPath, logFile.getAbsolutePath());
				validatedStudies.put(studyData, exitStatus);

				logger.info("Validation of study "+study+" finished.");
			}
			emailService.emailValidationReport(validatedStudies, validationLevel);
		} catch (ValidatorException e) {
			//tell about error, continue with next study
			logger.error(e.getMessage()+". The app will skip this study.");
			emailService.emailGenericError(e.getMessage()+". The app will skip this study.", e);
			e.printStackTrace();
		}
		return studiesPassed;
	}
}
