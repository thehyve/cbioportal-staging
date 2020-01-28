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
import org.cbioportal.staging.etl.Transformer.ExitStatus;
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
	public ExitStatus transform(File untransformedFilesPath, File transformedFilesPath, File logFile) throws TransformerException {
		if (!transformedFilesPath.exists()) {
			transformedFilesPath.mkdir();
        }
		try {
			ProcessBuilder transformationCommand;
			logger.info("Starting transformation for study: "+untransformedFilesPath.getName());
			//Skip transformation if skipTransformation=True or the study contains a meta_study.txt file
			if (transformationCommandScript.equals("")) {
				throw new TransformerException("No transformation command script has been specified in the application.properties.");
            }

            //Build transformation command
            ArrayList<String> transformationCmd = new ArrayList<String>();
            for(String part:transformationCommandScript.split(" ")) { //Remove potential spaces in Transformation Command Script
                transformationCmd.add(part);
            }
            Collections.addAll(transformationCmd, "-i", untransformedFilesPath.toString(), "-o", transformedFilesPath.toString());

            //Run transformation command
            transformationCommand = new ProcessBuilder(transformationCmd);
            logger.info("Executing command: " + String.join(" ", transformationCommand.command()));
            transformationCommand.redirectErrorStream(true);
            transformationCommand.redirectOutput(Redirect.appendTo(logFile));
            Process transformationProcess = transformationCommand.start();
            
            transformationProcess.waitFor(); //Wait until transformation is finished

            //Interprete exit status of the process and return it
            ExitStatus exitStatus = null;
            if (transformationProcess.exitValue() == 0) {
                exitStatus = ExitStatus.SUCCESS;
            } else if (transformationProcess.exitValue() == 3) {
                exitStatus = ExitStatus.WARNINGS;
            } else {
                exitStatus = ExitStatus.ERRORS;
            }
            return exitStatus;
            
		} catch (FileNotFoundException e) {
			throw new TransformerException("The following file path was not found: "+untransformedFilesPath.getAbsolutePath(), e);
		} catch (InterruptedException e) {
            throw new TransformerException("The transformation process has been interrupted by another process.", e);
        } catch (IOException e) {
            throw new TransformerException ("The working directory specified in the command or the transformation script file do not exist, "+
                "or you do not have permissions to work with the transformation script file.", e);
        }
	}
}
