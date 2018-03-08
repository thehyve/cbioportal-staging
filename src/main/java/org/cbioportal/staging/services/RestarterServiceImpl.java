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

import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RestarterServiceImpl implements RestarterService {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);

	@Value("${cbioportal.mode}")
	private String cbioportalMode;
	
	@Value("${refresh.command}")
	private String refreshCommand;

	@Value("${portal.home:.}")
	private String portalHome;
	
	@Override
	public void restart() throws InterruptedException, IOException, ConfigurationException {
		if (!refreshCommand.equals("")) {
			if (cbioportalMode.equals("local")) {
				logger.info("Stopping Tomcat...");
				ProcessBuilder stopCmd = new ProcessBuilder(refreshCommand, "stop", "-force");
				stopCmd.directory(new File(portalHome));
				logger.info("Executing command: "+String.join(" ", stopCmd.command()));
				Process stopProcess = stopCmd.start();
				stopProcess.waitFor();
				logger.info("Tomcat successfully stopped. Restarting Tomcat...");
				ProcessBuilder startCmd = new ProcessBuilder(refreshCommand, "start");
				startCmd.directory(new File(portalHome));
				Process startProcess = startCmd.start();
				startProcess.waitFor();
				logger.info("Tomcat successfully restarted.");
			} else if (cbioportalMode.equals("docker")) {
				logger.info("Restarting Tomcat...");
				ProcessBuilder restarterCmd = new ProcessBuilder ("docker", "restart", refreshCommand);
				logger.info("Executing command: "+String.join(" ", restarterCmd.command()));
				Process restartProcess = restarterCmd.start();
				BufferedReader reader = new BufferedReader(new InputStreamReader(restartProcess.getErrorStream()));
				String line = null;
				while ((line = reader.readLine()) != null)
				{
					logger.info(line); //Print output if an error occurs
				}
				restartProcess.waitFor();
				logger.info("Tomcat successfully restarted.");
			} else {
				throw new ConfigurationException("cbioportal.mode is not 'local' or 'docker'. Value encountered: "+cbioportalMode+
						". Please change the mode in the application.properties.");
			}
		} else {
			throw new IOException("The refresh.command is empty. Please check your application.properties.");
		}
	}

}
