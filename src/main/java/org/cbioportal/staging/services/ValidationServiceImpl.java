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
package org.cbioportal.staging.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;

import org.cbioportal.staging.etl.Validator;
import org.cbioportal.staging.etl.Transformer.ExitStatus;
import org.cbioportal.staging.exceptions.ValidatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ValidationServiceImpl implements ValidationService {
	private static final Logger logger = LoggerFactory.getLogger(Validator.class);

	@Value("${cbioportal.mode}")
	private String cbioportalMode;
	
	@Value("${cbioportal.docker.image}")
	private String cbioportalDockerImage;
	
	@Value("${cbioportal.docker.network}")
    private String cbioportalDockerNetwork;
    
    @Value("${cbioportal.docker.properties}")
    private String cbioportalDockerProperties;
	
	@Value("${portal.source:.}")
	private String portalSource;
		
	@Override
	public ExitStatus validate(File studyPath, File report, File logFile) throws ValidatorException {
		try {
            File portalInfoFolder = new File(studyPath+"/portalInfo");
			
			ProcessBuilder validationCmd;
			ProcessBuilder portalInfoCmd;
			if (cbioportalMode.equals("local")) {
				validationCmd = new ProcessBuilder("./validateData.py", "-s", studyPath.getAbsolutePath(), "-p", portalInfoFolder.toString(), "-html", report.getAbsolutePath(), "-v");
				portalInfoCmd = new ProcessBuilder("./dumpPortalInfo.pl", portalInfoFolder.toString());
				portalInfoCmd.directory(new File(portalSource+"/core/src/main/scripts"));
				validationCmd.directory(new File(portalSource+"/core/src/main/scripts/importer"));
			} else if (cbioportalMode.equals("docker")) {
				if (!cbioportalDockerImage.equals("") && !cbioportalDockerNetwork.equals("")) {
					//docker command:
					validationCmd = new ProcessBuilder ("docker", "run", "-i", "--rm",
							"-v", studyPath.getAbsolutePath()+":/study:ro", "-v", report+":/outreport.html",
                            "-v", portalInfoFolder.toString()+ ":/portalinfo:ro", 
                            "-v", cbioportalDockerProperties+":/cbioportal/portal.properties:ro", cbioportalDockerImage,
							"validateData.py", "-p", "/portalinfo", "-s", "/study", "--html=/outreport.html");
					portalInfoCmd = new ProcessBuilder("docker", "run", "--rm", "--net", cbioportalDockerNetwork,
                            "-v", portalInfoFolder.toString()+":/portalinfo",
                            "-v", cbioportalDockerProperties+":/cbioportal/portal.properties:ro",
                            "-w", "/cbioportal/core/src/main/scripts",
							cbioportalDockerImage, "./dumpPortalInfo.pl", "/portalinfo");
				} else {
					throw new ValidatorException("cbioportal.mode is 'docker', but no Docker image or network has been specified in the application.properties.");
				}
			} else {
				throw new ValidatorException("cbioportal.mode is not 'local' or 'docker'. Value encountered: "+cbioportalMode+
						". Please change the mode in the application.properties.");
			}
			
			logger.info("Dumping portalInfo...");
			logger.info("Executing command: "+String.join(" ", portalInfoCmd.command()));
			Process pInfo = portalInfoCmd.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(pInfo.getErrorStream()));
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				logger.warn(line);//TODO warn, since this is error stream output ^ ?
			}
			pInfo.waitFor();
			if (pInfo.exitValue() != 0) {
				throw new ValidatorException("Dump portalInfo step failed");
			}

			logger.info("Dump portalInfo finished. Continuing validation...");
		
			//Apply validation command
			logger.info("Starting validation. Report will be stored in: " + report.getAbsolutePath());
			logger.info("Executing command: "+String.join(" ", validationCmd.command()));
			validationCmd.redirectErrorStream(true);
			validationCmd.redirectOutput(Redirect.appendTo(logFile));
			Process validateProcess = validationCmd.start();
			validateProcess.waitFor(); //Wait until validation is finished
            
            //Interprete exit status of the process and return it
            ExitStatus exitStatus = null;
            if (validateProcess.exitValue() == 0) {
                exitStatus = ExitStatus.SUCCESS;
            } else if (validateProcess.exitValue() == 3) {
                exitStatus = ExitStatus.WARNINGS;
            } else {
                exitStatus = ExitStatus.ERRORS;
            }
            return exitStatus;
			
		} catch (InvalidPropertyException e) {
			throw new ValidatorException("Error during validation execution: property not valid, check the validation command. ", e);
		} catch (IOException e) {
			if (cbioportalMode.equals("docker")) {
				throw new ValidatorException("Error during validation execution: check if Docker is installed, check whether the current"
						+ " user has sufficient rights to run Docker, and if the configured working directory is accessible to Docker.", e);
			} else {
				throw new ValidatorException("Check if portal source is correctly set in application.properties. Configured portal source is: "+portalSource, e);
			}
		} catch (InterruptedException e) {
            throw new ValidatorException("The validation process has been interrupted by another process.", e);
        }
	}

}
