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
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.ArrayList;
import java.util.Collections;

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

	@Value("${transformation.command.script:}")
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

            //Build transformation command
            ArrayList<String> transformationCmd = new ArrayList<String>();
            for(String part:transformationCommandScript.split(" ")) { //Remove potential spaces in Transformation Command Script
                transformationCmd.add(part);
            }
            Collections.addAll(transformationCmd, "-i", studyPath.toString(), "-o", finalPath.toString());

            //Run transformation command
            transformationCommand = new ProcessBuilder(transformationCmd);
            //transformationCommand = new ProcessBuilder (transformationCmd, "-i", studyPath.toString(), "-o", finalPath.toString());
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
}
