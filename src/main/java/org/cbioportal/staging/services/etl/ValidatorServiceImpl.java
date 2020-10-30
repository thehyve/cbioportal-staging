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
package org.cbioportal.staging.services.etl;

import java.io.BufferedReader;
import java.io.IOException;

import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import org.cbioportal.staging.etl.Validator;
import org.cbioportal.staging.exceptions.CommandBuilderException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.exceptions.ValidatorException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.command.ICommandBuilder;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class ValidatorServiceImpl implements IValidatorService {
    private static final Logger logger = LoggerFactory.getLogger(Validator.class);

    @Autowired
    private ICommandBuilder commandBuilder;

    @Autowired
    private ResourceUtils utils;

	@Override
	public ExitStatus validate(Resource studyPath, Resource reportFile, Resource logFile) throws ValidatorException {
		try {

			Resource portalInfoFolder = utils.createDirResource(studyPath, "portalInfo");

			ProcessBuilder portalInfoCmd = commandBuilder.buildPortalInfoCommand(portalInfoFolder);
			if (portalInfoCmd != null) {
			    logger.info("Dumping portalInfo...");
                logger.info("Executing command: " + String.join(" ", portalInfoCmd.command()));
                Process pInfo = portalInfoCmd.start();
                BufferedReader reader = new BufferedReader(new InputStreamReader(pInfo.getErrorStream()));
                String line = null;
                while ((line = reader.readLine()) != null) {
                    logger.warn(line);// TODO warn, since this is error stream output ^ ?
                }
                pInfo.waitFor();
                if (pInfo.exitValue() != 0) {
                    throw new ValidatorException("Dump portalInfo step failed");
                }
			    logger.info("Dump portalInfo finished. Continuing validation...");
            } else {
                logger.info("Dump portalInfo is not needed. Accessing live portal. Continuing validation...");
            }

			// Apply validation command
			ProcessBuilder validationCmd = commandBuilder.buildValidatorCommand(studyPath, portalInfoFolder, reportFile);
			logger.info(
					"Starting validation. Report will be stored in: " + utils.getFile(reportFile).getAbsolutePath());
			logger.info("Executing command: " + String.join(" ", validationCmd.command()));
			validationCmd.redirectErrorStream(true);
			validationCmd.redirectOutput(Redirect.appendTo(utils.getFile(logFile)));
			Process validateProcess = validationCmd.start();
			validateProcess.waitFor(); // Wait until validation is finished

			// Interprete exit status of the process and return it
			ExitStatus exitStatus = null;
			if (validateProcess.exitValue() == 0) {
				exitStatus = ExitStatus.SUCCESS;
			} else if (validateProcess.exitValue() == 3) {
				exitStatus = ExitStatus.WARNING;
			} else {
				exitStatus = ExitStatus.ERROR;
			}
			return exitStatus;

		} catch (IOException e) {
			throw new ValidatorException("Error during validation execution: check if Docker is installed, check whether the current"
					+ " user has sufficient rights to run Docker, and if the configured working directory is accessible to Docker.", e);
		} catch (InterruptedException e) {
            throw new ValidatorException("The validation process has been interrupted by another process.", e);
        } catch (ResourceUtilsException e) {
            throw new ValidatorException("ResourceUtilsException", e);
        } catch (CommandBuilderException e) {
            throw new ValidatorException("CommandBuilderException", e);
        }
	}

}
