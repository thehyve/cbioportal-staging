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
import java.io.StringWriter;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
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
	public void transform(File studyPath, File finalPath) throws TransformerException, InterruptedException, ConfigurationException, IOException {
		if (!finalPath.exists()) {
			finalPath.mkdir();
		}
		try {
			logger.info("Starting transformation for file: "+studyPath.getName());
			if (transformationCommandScript.equals("")) {
				throw new ConfigurationException("No transformation command script has been specified in the application.properties.");
			}
			//Apply transformation command
			String transformationCommand = transformationCommandScript +  " -i " + studyPath.toString() + " -o " + finalPath.toString();
			transformationCommand = resolveEnvVars(transformationCommand);
			logger.info("Executing command: " + transformationCommand);
			Process transformationProcess = Runtime.getRuntime().exec(transformationCommand);
			
			//TODO - ideally these two loggers are running in parallel so that we see the stdout and stderr in the same
			//order as reported by transformation script. This is not yet done below, so we first get all stdout, followed
			//by all stderr:
			logAndReturnProcessStream(transformationProcess.getInputStream(), false);
			String errorStack = logAndReturnProcessStream(transformationProcess.getErrorStream(), true);
			
			transformationProcess.waitFor(); //Wait until transformation is finished
			if (transformationProcess.exitValue() != 0) {
				throw new RuntimeException("The transformation script has failed: "+errorStack);
			}
			logger.info("Finished transformation for file: "+studyPath.getName());
		} catch (FileNotFoundException e1) {
			throw new TransformerException("The following file path was not found: "+studyPath.getAbsolutePath(), e1);
		}
	}


	private String logAndReturnProcessStream(InputStream stream, boolean errorStream) throws IOException {
		InputStreamReader streamReader = new InputStreamReader(stream);
		// copy it as String for returning
		StringWriter writer = new StringWriter();
		IOUtils.copy(streamReader, writer);
		
		// also log stream in the log system
		BufferedReader reader = new BufferedReader(streamReader);
		String line = null;
		while ((line = reader.readLine()) != null)
		{
			if (errorStream) {
				logger.error(line);
			} else {
				logger.info(line);
			}
			
		}

		return writer.toString();
	}


	/**
	 * Returns input string with environment variable references expanded, e.g. $SOME_VAR or ${SOME_VAR}
	 */
	private String resolveEnvVars(String input)
	{
		if (null == input)
		{
			return null;
		}
		// match ${ENV_VAR_NAME} or $ENV_VAR_NAME
		Pattern p = Pattern.compile("\\$\\{(\\w+)\\}|\\$(\\w+)");
		Matcher m = p.matcher(input); // get a matcher object
		StringBuffer sb = new StringBuffer();
		while(m.find()){
			String envVarName = null == m.group(1) ? m.group(2) : m.group(1);
			String envVarValue = System.getenv(envVarName);
			m.appendReplacement(sb, null == envVarValue ? "" : envVarValue);
		}
		m.appendTail(sb);
		return sb.toString();
	}
}
