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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.exceptions.ValidatorException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.etl.IValidatorService;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.cbioportal.staging.services.resource.Study;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class Validator {
    private static final Logger logger = LoggerFactory.getLogger(Validator.class);

    @Autowired
    private IValidatorService validatorService;

    @Autowired
    private ResourceUtils utils;

    @Value("${validation.level:ERROR}")
    private String validationLevel;

    private Map<String, Resource> logAndReportFiles = new HashMap<>();;
    List<Study> validStudies = new ArrayList<>();

    private boolean hasStudyPassed(ExitStatus exitStatus) throws ValidatorException {
        if (validationLevel.equals("WARNING")) { // Load studies with no warnings and no errors
            if (exitStatus == ExitStatus.SUCCESS) {
                return true;
            }
            return false;
        } else if (validationLevel.equals("ERROR")) { // Load studies with only no errors
            if (exitStatus == ExitStatus.SUCCESS || exitStatus == ExitStatus.WARNING) {
                return true;
            }
            return false;
        } else {
            throw new ValidatorException(
                    "Validation level should be WARNING or ERROR. Please check the application.properties.");
        }
    }

    public Map<String, ExitStatus> validate(Study[] studies) throws ValidatorException {

        logAndReportFiles.clear();
        validStudies.clear();

        Map<String, ExitStatus> validatedStudies = new HashMap<>();
        logAndReportFiles = new HashMap<>();
        validStudies.clear();

        try {
            for (Study study : studies) {

                String studyId = study.getStudyId();

                logger.info("Starting validation of study " + studyId);
                Resource studyPath = study.getStudyDir();

                Resource logFile = utils.createFileResource(studyPath, studyId + "_validation_log.txt");
                Resource reportFile = utils.createFileResource(studyPath, studyId + "_validation_report.html");
                logAndReportFiles.put(studyId+" validation log", logFile);
                logAndReportFiles.put(studyId+" validation report", reportFile);

                ExitStatus exitStatus = validatorService.validate(studyPath, reportFile, logFile);
                validatedStudies.put(studyId, exitStatus);

                if (hasStudyPassed(exitStatus)) {
                    validStudies.add(study);
                    logger.info("Study "+studyId+" has passed validation.");
                } else {
                    logger.info("Study "+studyId+" has failed validation.");
                }
            }
        } catch (ResourceUtilsException e) {
            throw new ValidatorException("Error occured while validating studies.", e);
        }
		return validatedStudies;
    }

    public Map<String, Resource> getLogAndReportFiles() {
        return logAndReportFiles;
    }

    public Study[] getValidStudies() throws ValidatorException {
        return validStudies.toArray(new Study[0]);
    }
}
