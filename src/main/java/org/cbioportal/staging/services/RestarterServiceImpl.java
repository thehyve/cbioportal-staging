/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

import org.cbioportal.staging.app.ScheduledScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RestarterServiceImpl implements RestarterService {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);

	@Value("${cbioportal.mode}")
	private String cbioportalMode;
	
	@Value("${cbioportal.container.name}")
	private String cbioportalContainerName;
	
	@Value("${tomcat.command}")
	private String tomcatCommand;

	@Value("${portal.home}")
	private String portalHome;
	
	@Override
	public void restart() throws InterruptedException, IOException {
		if (cbioportalMode.equals("local")) {
			logger.info("Stopping Tomcat...");
			ProcessBuilder stopCmd = new ProcessBuilder(tomcatCommand, "stop", "-force");
			stopCmd.directory(new File(portalHome));
			Process stopProcess = stopCmd.start();
			stopProcess.waitFor();
			logger.info("Tomcat successfully stopped. Restarting Tomcat...");
			ProcessBuilder startCmd = new ProcessBuilder(tomcatCommand, "start");
			startCmd.directory(new File(portalHome));
			Process startProcess = startCmd.start();
			startProcess.waitFor();
			logger.info("Tomcat successfully restarted.");
		} else if (cbioportalMode.equals("docker")) {
			if (!cbioportalContainerName.equals("")) {
				logger.info("Restarting Tomcat...");
				ProcessBuilder restarterCmd = new ProcessBuilder ("docker", "restart", cbioportalContainerName);
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
				throw new IOException("cbioportal.mode is 'docker', but no container name has been specified in the application.properties.");
			}
		} else {
			throw new IOException("cbioportal.mode is not 'local' or 'docker'. Value encountered: "+cbioportalMode+
					". Please change the mode in the application.properties.");
		}
	}

}
