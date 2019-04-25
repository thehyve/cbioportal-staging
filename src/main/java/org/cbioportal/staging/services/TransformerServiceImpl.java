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
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;

import org.cbioportal.staging.etl.Transformer;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.TransformerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TransformerServiceImpl implements TransformerService {
	private static final Logger logger = LoggerFactory.getLogger(Transformer.class);

	@Value("${transformation.command.script}")
	private String transformationCommandScript;
	
	@Override
	public int transform(File studyPath, File finalPath, File logFile) throws TransformerException, InterruptedException, ConfigurationException, IOException {
		if (!finalPath.exists()) {
			finalPath.mkdir();
        }
		try {
			ProcessBuilder transformationCommand;
			logger.info("Starting transformation for study: "+studyPath.getName());
			//Skip transformation if skipTransformation=True or the study contains a meta_study.txt file
			if (transformationCommandScript.equals("")) {
				throw new ConfigurationException("No transformation command script has been specified in the application.properties.");
            }

			//Run transformation command
            transformationCommand = new ProcessBuilder (transformationCommandScript, "-i", studyPath.toString(), "-o", finalPath.toString(), 
                "-l", logFile.getAbsolutePath());
            logger.info("Executing command: " + String.join(" ", transformationCommand.command()));
            transformationCommand.redirectErrorStream(true);
		    transformationCommand.redirectOutput(Redirect.appendTo(logFile));
            Process transformationProcess = transformationCommand.start();
			
            transformationProcess.waitFor(); //Wait until transformation is finished
            return transformationProcess.exitValue();
		} catch (FileNotFoundException e1) {
			throw new TransformerException("The following file path was not found: "+studyPath.getAbsolutePath(), e1);
		}
	}


	private String logAndReturnProcessStream(InputStream stream, boolean errorStream) throws IOException {
		InputStreamReader streamReader = new InputStreamReader(stream);
		String result = "";
		// log stream in the log system
		BufferedReader reader = new BufferedReader(streamReader);
		String line = null;
		while ((line = reader.readLine()) != null)
		{
			if (errorStream) {
				//Because subprocesses print both warnings and errors to stderr, we log them here as WARNING.
				//Assumption is that if the transformation process hits a real ERROR, it will exit with exit
				//status > 0, resulting in an ERROR in the logs.
				logger.warn(line);
			} else {
				logger.info(line);
			}
			// also copy it as String for returning the full output
			result += line + "\n";
		}

		return result;
	}

	@Override
	public void copyStudy(File studyPath, File finalPath) throws TransformerException, InterruptedException, ConfigurationException, IOException {
		if (!finalPath.exists()) {
			finalPath.mkdir();
		}
		logger.info("Skipping transformation for study: "+studyPath);
		String transformationCommand = "cp -R " + studyPath.toString() + "/. " + finalPath.toString();
		logger.info("Executing command: " + transformationCommand);
		try {
			Process transformationProcess = Runtime.getRuntime().exec(transformationCommand);
			
			//TODO - ideally these two loggers are running in parallel so that we see the stdout and stderr in the same
			//order as reported by transformation script. This is not yet done below, so we first get all stdout, followed
			//by all stderr:
			logAndReturnProcessStream(transformationProcess.getInputStream(), false);
			String errorStack = logAndReturnProcessStream(transformationProcess.getErrorStream(), true);
			
			transformationProcess.waitFor(); //Wait until transformation is finished
			if (transformationProcess.exitValue() != 0) {
				throw new RuntimeException("The command has failed: "+errorStack);
			}
			logger.info("Finished copying for study: "+studyPath.getName());
		} catch (FileNotFoundException e1) {
			throw new TransformerException("The following file path was not found: "+studyPath.getAbsolutePath(), e1);
		}
	}
}
