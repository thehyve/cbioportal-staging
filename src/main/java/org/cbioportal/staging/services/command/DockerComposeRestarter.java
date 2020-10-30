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
package org.cbioportal.staging.services.command;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.cbioportal.staging.exceptions.RestarterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConditionalOnProperty(value="cbioportal.mode", havingValue = "compose")
public class DockerComposeRestarter implements IRestarter {

	private static final Logger logger = LoggerFactory.getLogger(DockerComposeRestarter.class);

	@Value("${cbioportal.compose.service}")
	private String cbioService;

    @Value("${cbioportal.compose.cbioportal.extensions:}")
    private String[] composeExtensions;

    // path inside staging app container where compose files are located
    @Value("${cbioportal.compose.context}")
    private String composeContext;

	public void restart() throws RestarterException {
        try {
            logger.info("Restarting cBioPortal...");
            List<String> commands = new ArrayList<>();
            commands.addAll(
                    Arrays.asList(new String[]{
                            cbioService, "restart"
                    })
            );
            ProcessBuilder restarterCmd = dockerComposeProcessBuilder(commands);
            logger.info("Executing command: "+String.join(" ", restarterCmd.command()));
            Process restartProcess = restarterCmd.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(restartProcess.getErrorStream()));
            String line = null;
            while ((line = reader.readLine()) != null)
            {
                logger.info(line); //Print output if an error occurs
            }
            restartProcess.waitFor();
            logger.info("cBioPortal successfully restarted.");
        }  catch (IOException e) {
            throw new RestarterException("The cBioPortal container specified in the command do not exist, "+
            "or you do not have permissions to execute the restarter command.", e);
        } catch (InterruptedException e) {
            throw new RestarterException("The loading process has been interrupted by another process.", e);
        }
	}

    private ProcessBuilder dockerComposeProcessBuilder(List<String> arguments) {
        List<String> commands = new ArrayList<>();
        commands.add("docker-compose");
        List<String> extensions = new ArrayList<>();
        Arrays.stream(composeExtensions)
                .forEach(e -> {
                    commands.add("-f");
                    commands.add(e);
                });
        commands.addAll(extensions);
        commands.addAll(arguments);
        ProcessBuilder processBuilder = new ProcessBuilder(commands);
        processBuilder.directory(new File(composeContext));
        return processBuilder;
    }

}
