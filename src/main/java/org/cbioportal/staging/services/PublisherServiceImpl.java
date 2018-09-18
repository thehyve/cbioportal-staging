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
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.List;

import org.cbioportal.staging.etl.Restarter;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class PublisherServiceImpl implements PublisherService {
	private static final Logger logger = LoggerFactory.getLogger(Restarter.class);
	
	@Value("${study.publish.command_prefix}")
	private String studyPublishCommandPrefix;
	
	@Value("${study.curator.email}")
        private String studyCuratorEmail;
	
	public void publishStudies(List<String> studyIds) throws InterruptedException, IOException, ConfigurationException {
		
		for (String studyId : studyIds) {
			String command = studyPublishCommandPrefix + " "+ studyId + " " + studyCuratorEmail;
			Process cmdProcess = Runtime.getRuntime().exec(command);
			logger.info("Executing command: "+command);
			
			BufferedReader reader = new BufferedReader(new InputStreamReader(cmdProcess.getInputStream()));
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				logger.info(line);
			}
			BufferedReader reader2 = new BufferedReader(new InputStreamReader(cmdProcess.getErrorStream()));
			String line2 = null;
			while ((line2 = reader2.readLine()) != null)
			{
				logger.warn(line2);
			}
			
			cmdProcess.waitFor();
			
			if (cmdProcess.exitValue() != 0) {
				throw new ConfigurationException("The command "+command+" has failed. Please check your configuration.");
			}
		}
		
	}
}
