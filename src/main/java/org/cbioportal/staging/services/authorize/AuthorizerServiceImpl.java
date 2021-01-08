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
package org.cbioportal.staging.services.authorize;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class AuthorizerServiceImpl implements IAuthorizerService {

	private static final Logger logger = LoggerFactory.getLogger(AuthorizerServiceImpl.class);

    @Autowired
    private ResourceUtils utils;

	@Value("${study.authorize.command_prefix:null}")
	private String studyAuthorizeCommandPrefix;

	@Value("${study.curator.emails:}")
	private String studyCuratorEmails;

	// base dir of staging app inside docker container
    @Value("${cbioportal.compose.context:/cbioportal-staging/}")
    private String composeContext;

	public void authorizeStudies(Set<String> studyIds) throws InterruptedException, IOException, ConfigurationException {

        if (studyCuratorEmails.equals("")) {
            logger.info("No curator emails defined with study.curator.emails property. Skipping Authorization of studies ...");
            return;
        }

		if (!studyAuthorizeCommandPrefix.equals("null")) {
			for (String studyId : studyIds) {
                for (String studyCuratorEmail : studyCuratorEmails.split(",")) {

                    List<String> commands = new ArrayList<>();
                    commands.addAll(Arrays.asList(studyAuthorizeCommandPrefix.split("\\s+")));
                    commands.add(studyId);
                    commands.add(studyCuratorEmail);
                    ProcessBuilder authCmd = new ProcessBuilder(commands);
                    authCmd.directory(new File(composeContext));
                    authCmd.redirectErrorStream(true);

                    logger.info("Executing command: " + String.join(" ", authCmd.command()));
                    final Process authProcess = authCmd.start();

                    String line = null;
                    BufferedReader infoReader = new BufferedReader(new InputStreamReader(authProcess.getInputStream()));
                    while ((line = infoReader.readLine()) != null)
                        logger.info(line);
                    BufferedReader errorReader = new BufferedReader(new InputStreamReader(authProcess.getErrorStream()));
                    while ((line = errorReader.readLine()) != null)
                        logger.warn(line);

                    authProcess.waitFor();

                    if (authProcess.exitValue() != 0) {
                        throw new ConfigurationException("The command "+authCmd.command()+" has failed. Please check your configuration.");
                    }
                }
            }
		}
	}
}
