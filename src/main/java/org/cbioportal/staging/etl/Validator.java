/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.etl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ValidatorException;
import org.cbioportal.staging.services.EmailService;
import org.cbioportal.staging.services.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

@Component
public class Validator {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);
	
	@Autowired
	EmailService emailService;
	
	@Autowired
	ValidationService validationService;
	
	@Value("${etl.working.dir:file:///tmp}")
	private File etlWorkingDir;
	
	@Value("${central.share.location}")
	private File centralShareLocation;
	
	@Value("${validation.level:ERROR}")
	private String validationLevel;
	
	@Value("${portal.home}")
	private String portalHome;
	
	Map<String, Integer> getMessageCounter(File logFile) {
		
		try {
			Map<String, Integer> messageCounter = new HashMap<String, Integer>();
			messageCounter.put("ERROR", 0);
			messageCounter.put("WARNING", 0);
			BufferedReader validationReader;
			validationReader = new BufferedReader(new FileReader(logFile));
			String valReadLine = null;
			while ((valReadLine = validationReader.readLine()) != null) {
				if (valReadLine.indexOf("WARNING") != -1) {
					messageCounter.put("WARNING", messageCounter.get("WARNING")+1);
				}
				if (valReadLine.indexOf("ERROR") != -1) {
					messageCounter.put("ERROR", messageCounter.get("ERROR")+1);
				}
			}
			validationReader.close();
			return messageCounter;
			
		} catch (IOException e) {
			logger.error("The log file provided has not been found.");
			try {
				emailService.emailGenericError("The log file provided has not been found.", e);
			} catch (Exception e1) {
				logger.error("The email could not be sent due to the error specified below.");
				e1.printStackTrace();
			}
		}
		return null;
	}
	
	boolean hasStudyPassed(String study, String validationLevel, Map<String, Integer> messageCounter) {
		if (validationLevel.equals("WARNING")) { //Load studies with no warnings or errors
			if (messageCounter.get("WARNING").equals(0) && messageCounter.get("ERROR").equals(0)) {
				return true;
			}
			return false;
		} else if (validationLevel.equals("ERROR")) { //Load studies with only no errors
			if (messageCounter.get("ERROR").equals(0)) {
				return true;
			}
			return false;
		} else {
			throw new IllegalArgumentException("Validation level should be WARNING or ERROR. Please check the application.properties.");
		}
	}
	
	ArrayList<String> validate(Integer id, List<String> studies) throws ConfigurationException, TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		ArrayList<String> studiesPassed = new ArrayList<String>();
		if (!centralShareLocation.exists()) {
			throw new IOException("The central share location directory specified in application.properties do not exist: "+centralShareLocation.toString()+
					". Stopping process...");
		} else {
			try {
				//Get studies from appropriate staging folder
				File originPath = new File(etlWorkingDir.toPath()+"/"+id+"/staging");
				Map<Pair<String,String>,Map<String, Integer>> validatedStudies = new HashMap<Pair<String,String>,Map<String, Integer>>();
				for (String study : studies) {
					logger.info("Starting validation of study "+study);
					//Get the paths for the study and validate it
					File studyPath = new File(originPath+"/"+study);
					String reportTimeStamp = new SimpleDateFormat("yyyy_MM_dd_HH.mm.ss").format(new Date());
					String reportPath = centralShareLocation.toString()+"/"+study+"_validation_report_"+reportTimeStamp+".html";
					File logFile = validationService.validate(study, studyPath.getAbsolutePath(), reportPath);

					//Process validation output
					Map<String, Integer> messageCounter = getMessageCounter(logFile);

					//Check if study has passed the validation threshold
					if (hasStudyPassed(study, validationLevel, messageCounter)) {
						studiesPassed.add(study);
					}

					//Add validation result for the email validation report
					Pair<String, String> studyData = Pair.of(reportPath, logFile.getAbsolutePath());
					validatedStudies.put(studyData, messageCounter);

					logger.info("Validation of study "+study+" finished. Errors: "+messageCounter.get("ERROR")+
							", Warnings: "+messageCounter.get("WARNING"));
				}
				emailService.emailValidationReport(validatedStudies, validationLevel);
			} catch (ValidatorException e) {
				//tell about error, continue with next study
				logger.error(e.getMessage()+". The app will continue with the next study.");
				emailService.emailGenericError(e.getMessage()+". The app will continue with the next study.", e);
				e.printStackTrace();
			}
		}
		return studiesPassed;
	}
}
