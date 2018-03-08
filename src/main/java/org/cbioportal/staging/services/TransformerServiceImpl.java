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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;

import org.apache.commons.io.IOUtils;
import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.TransformerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TransformerServiceImpl implements TransformerService {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);

	@Value("${transformation.command.location}")
	private String transformationCommandLocation;
	
	@Value("${transformation.command.script}")
	private String transformationCommandScript;
	
	@Override
	public void transform(File studyPath, File finalPath) throws TransformerException, InterruptedException, ConfigurationException, IOException {
		if (!finalPath.exists()) {
			finalPath.mkdir();
		}
		try {
			logger.info("Starting transformation for file: "+studyPath.getName());
			ProcessBuilder transformationCmd;
			if (transformationCommandLocation.equals("")) {
				throw new ConfigurationException("No transformation command location has been specified in the application.properties.");
			} else if (transformationCommandLocation.equals("")) {
				throw new ConfigurationException("No transformation command script has been specified in the application.properties.");
			} else {
				transformationCmd = new ProcessBuilder(transformationCommandScript, "-i", studyPath.toString(), "-o", finalPath.toString());
				transformationCmd.directory(new File(transformationCommandLocation));
			}
			//Apply transformation command
			logger.info("Executing command: "+String.join(" ", transformationCmd.command()));
			Process transformationProcess = transformationCmd.start();
			InputStreamReader errorStream = new InputStreamReader(transformationProcess.getErrorStream());
			//Create error stack for exception
			StringWriter writer = new StringWriter();
			IOUtils.copy(errorStream, writer);
			String errorStack = writer.toString();
			//Print stream in the screen
			BufferedReader reader = new BufferedReader(errorStream);
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				logger.info(line);
			}
			transformationProcess.waitFor(); //Wait until transformation is finished
			if (transformationProcess.exitValue() != 0) {
				throw new RuntimeException("The transformation script has failed: "+errorStack);
			}
			logger.info("Finished transformation for file: "+studyPath.getName());
		} catch (FileNotFoundException e1) {
			throw new TransformerException("The following file path was not found: "+studyPath.getAbsolutePath(), e1);
		}
	}
}
