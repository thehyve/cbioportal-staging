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

import org.cbioportal.staging.etl.Transformer.ExitStatus;
import org.cbioportal.staging.exceptions.ValidatorException;
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
    private ValidationService validationService;

	@Value("${validation.level:ERROR}")
    private String validationLevel;

	boolean hasStudyPassed(String study, String validationLevel, ExitStatus exitStatus) throws ValidatorException {
		if (validationLevel.equals("WARNING")) { //Load studies with no warnings and no errors
			if (exitStatus == ExitStatus.SUCCESS) {
				return true;
			}
			return false;
		} else if (validationLevel.equals("ERROR")) { //Load studies with only no errors
			if (exitStatus == ExitStatus.SUCCESS || exitStatus == ExitStatus.WARNINGS) {
				return true;
			}
			return false;
		} else {
			throw new ValidatorException("Validation level should be WARNING or ERROR. Please check the application.properties.");
		}
	}

	Map<String, ExitStatus> validate(Map<String, File> studyPaths, String reportSuffix, String logSuffix) throws ValidatorException {
        Map<String,ExitStatus> validatedStudies = new HashMap<String,ExitStatus>();
        //Get studies from appropriate staging folder
        for (String studyId : studyPaths.keySet()) {
            logger.info("Starting validation of study "+studyId);
            File studyPath = studyPaths.get(studyId);

            String reportFileName = studyId + reportSuffix;
            String logFileName = studyId + logSuffix;

            ExitStatus exitStatus = validationService.validate(studyPath, reportFileName, logFileName);

            //Add validation result for the email validation report
            validatedStudies.put(studyId, exitStatus);

            logger.info("Validation of study "+studyId+" finished.");
        }
		return validatedStudies;
    }

    Map<String,File> getStudiesThatPassedValidation(Map<String, ExitStatus> validatedStudies, Map<String, File> studyPaths) throws ValidatorException {
        Map<String, File> studiesThatPassedValidation = new HashMap<String, File>();
        for (String studyId : validatedStudies.keySet()) {
            if (hasStudyPassed(studyId, validationLevel, validatedStudies.get(studyId))) {
                studiesThatPassedValidation.put(studyId, studyPaths.get(studyId));
            }
        }
        return studiesThatPassedValidation;
    }
}
