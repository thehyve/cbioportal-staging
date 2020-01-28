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

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.cbioportal.staging.etl.Loader;
import org.cbioportal.staging.etl.Transformer.ExitStatus;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.LoaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoaderServiceImpl implements LoaderService {
	private static final Logger logger = LoggerFactory.getLogger(Loader.class);

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
	public ExitStatus load(File studyPath, File logFile) throws LoaderException {
        try {
            ProcessBuilder loaderCmd;
            if (cbioportalMode.equals("local")) {
                loaderCmd = new ProcessBuilder("./cbioportalImporter.py", "-s", studyPath.toString());
                loaderCmd.directory(new File(portalSource+"/core/src/main/scripts/importer"));
            } else if (cbioportalMode.equals("docker")) {
                if (!cbioportalDockerImage.equals("") && !cbioportalDockerNetwork.equals("")) {
                    loaderCmd = new ProcessBuilder ("docker", "run", "-i", "--rm", "--net", cbioportalDockerNetwork,
                            "-v", studyPath.toString()+":/study:ro",
                            "-v", cbioportalDockerProperties+":/cbioportal/portal.properties:ro", 
                            cbioportalDockerImage,
                            "cbioportalImporter.py", "-s", "/study");
                } else {
                    throw new LoaderException("cbioportal.mode is 'docker', but no Docker image or network has been specified in the application.properties.");
                }
            } else {
                throw new LoaderException("cbioportal.mode is not 'local' or 'docker'. Value encountered: "+cbioportalMode+
                        ". Please change the mode in the application.properties.");
            }

            //Apply loader command
            logger.info("Executing command: "+String.join(" ", loaderCmd.command()));
            loaderCmd.redirectErrorStream(true);
            loaderCmd.redirectOutput(Redirect.appendTo(logFile));
            Process loadProcess = loaderCmd.start();
            loadProcess.waitFor(); //Wait until loading is finished

            //Interprete exit status of the process and return it
            ExitStatus exitStatus = null;
            if (loadProcess.exitValue() == 0) {
                exitStatus = ExitStatus.SUCCESS;
            } else {
                exitStatus = ExitStatus.ERRORS;
            }
            return exitStatus;
        } catch (InterruptedException e) {
            throw new LoaderException("The loading process has been interrupted by another process.", e);
        } catch (IOException e) {
            throw new LoaderException("The working directory specified in the command or the transformation script file do not exist, "+
                "or you do not have permissions to work with the transformation script file.", e);
        }

	}
}
