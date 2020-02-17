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

import org.cbioportal.staging.exceptions.RestarterException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConditionalOnProperty(value="cbioportal.mode", havingValue = "local")
public class LocalRestarter implements IRestarter {

	private static final Logger logger = LoggerFactory.getLogger(LocalRestarter.class);

	@Value("${portal.source:.}")
	private String portalSource;

	public void restart() throws RestarterException {
		try {
			logger.info("Stopping Tomcat...");
			ProcessBuilder stopCmd = new ProcessBuilder("catalina", "stop", "-force");
			stopCmd.directory(new File(portalSource));
			logger.info("Executing command: "+String.join(" ", stopCmd.command()));
			Process stopProcess = stopCmd.start();
			stopProcess.waitFor();
			logger.info("Tomcat successfully stopped. Restarting Tomcat...");
			ProcessBuilder startCmd = new ProcessBuilder("catalina", "start");
			startCmd.directory(new File(portalSource));
			Process startProcess = startCmd.start();
			startProcess.waitFor();
			logger.info("Tomcat successfully restarted.");
		} catch (IOException e) {
            throw new RestarterException("The cBioPortal container specified in the command do not exist, "+
            "or you do not have permissions to execute the restarter command.", e);
        } catch (InterruptedException e) {
             throw new RestarterException("The loading process has been interrupted by another process.", e);
        }
	}

}
