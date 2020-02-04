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

import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.etl.Transformer.ExitStatus;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ValidatorException;
import org.cbioportal.staging.services.ValidatorService;
import org.cbioportal.staging.services.resource.ResourceUtils;
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
    private ValidatorService validatorService;

    @Autowired
    private ResourceUtils utils;

    @Value("${validation.level:ERROR}")
    private String validationLevel;

    private Map<String, Resource> logAndReportFiles = new HashMap<>();
    Map<String, Resource> dirsValidStudies = new HashMap<>();

    boolean hasStudyPassed(ExitStatus exitStatus) throws ValidatorException {
        if (validationLevel.equals("WARNING")) { // Load studies with no warnings and no errors
            if (exitStatus == ExitStatus.SUCCESS) {
                return true;
            }
            return false;
        } else if (validationLevel.equals("ERROR")) { // Load studies with only no errors
            if (exitStatus == ExitStatus.SUCCESS || exitStatus == ExitStatus.WARNINGS) {
                return true;
            }
            return false;
        } else {
            throw new ValidatorException(
                    "Validation level should be WARNING or ERROR. Please check the application.properties.");
        }
    }

    Map<String, ExitStatus> validate(Map<String, Resource> studyPaths) throws ValidatorException {
        Map<String, ExitStatus> validatedStudies = new HashMap<>();

        try {
            for (String studyId : studyPaths.keySet()) {
                logger.info("Starting validation of study " + studyId);
                Resource studyPath = studyPaths.get(studyId);

                Resource logFile = utils.createFileResource(studyPath, studyId + "_validation_log.txt");
                Resource reportFile = utils.createFileResource(studyPath, studyId + "_validation_report.txt");
                logAndReportFiles.put(studyId+" validation log", logFile);
                logAndReportFiles.put(studyId+" validation report", reportFile);

                ExitStatus exitStatus = validatorService.validate(studyPath, reportFile, logFile);
                validatedStudies.put(studyId, exitStatus);

                if (hasStudyPassed(exitStatus)) {
                    dirsValidStudies.put(studyId, studyPath);
                    logger.info("Transformation of study "+studyId+" finished successfully.");
                } else {
                    logger.info("Validation of study "+studyId+" finished unsuccessfully");
                }
            }
        } catch (ResourceCollectionException e) {
            throw new ValidatorException("Error occured while validating studies.", e);
        }
		return validatedStudies;
    }

    Map<String, Resource> getLogAndReportFiles() {
        return logAndReportFiles;
    }

    Map<String,Resource> getValidStudies() throws ValidatorException {
        return dirsValidStudies;
    }
}
