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
package org.cbioportal.staging.services.etl;

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cbioportal.staging.etl.Transformer;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.exceptions.TransformerException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class TransformerServiceImpl implements ITransformerService {
    private static final Logger logger = LoggerFactory.getLogger(Transformer.class);

    @Value("${transformation.command.script:}")
    private String transformationCommandScript;

    @Value("${transformation.command.script.docker.image:}")
    private String transformationCommandScriptDockerImage;

    @Autowired
    private ResourcePatternResolver resourceResolver;

    @Autowired
    private ResourceUtils utils;

    @Override
    public List<String> parseCommandScript() throws TransformerException {
        try {
            List<String> command = Stream.of(transformationCommandScript.trim().split("\\s+")).collect(Collectors.toList());
            Resource script = resourceResolver.getResource(command.get(0));
            script.getFile().setExecutable(true); // required for tests: x-permissions are stripped in maven target resource
                                                  // dir

            if (!script.exists()) {
                throw new TransformerException(
                        "Transformation command script specified in the application.properties points does not exist at the indication location.");
            }

            if (script.getFile().isDirectory()) {
                throw new TransformerException(
                        "Transformation command script specified in the application.properties points to directory.");
            }
            String scriptPath = utils.stripResourceTypePrefix(script.getURL().toString());
            command.set(0, scriptPath);
            return command;
        } catch (IOException e) {
            throw new TransformerException("Cannot access script path.", e);
        }
    }

    @Override
    public List<String> buildCommand(Resource untransformedFilesPath, Resource transformedFilesPath) throws TransformerException {
        if (transformationCommandScriptDockerImage.equals("")) {
            return parseCommandScript();
        } else {
            try {
                String untransformedPath = utils.stripResourceTypePrefix(untransformedFilesPath.getURL().toString());
                String transformedPath = utils.stripResourceTypePrefix(transformedFilesPath.getURL().toString());
                String dockerPrefix = "docker run --rm -v "+untransformedPath+":"+untransformedPath+
                    " -v "+transformedPath+":"+transformedPath+" "+transformationCommandScriptDockerImage;
                List<String> command = Stream.of(dockerPrefix.trim().split("\\s+")).collect(Collectors.toList());
                if (!transformationCommandScript.equals("")) {
                    List<String> transformationCommand = parseCommandScript();
                    command.addAll(transformationCommand);
                }
                return command;
            } catch (IOException e) {
                throw new TransformerException("Cannot access ETL Working Directory.", e);
            }
        }
    }

    @Override
    public ExitStatus transform(Resource untransformedFilesPath, Resource transformedFilesPath, Resource logFile)
            throws TransformerException {

        try {
            List<String> command = buildCommand(untransformedFilesPath, transformedFilesPath);

            logger.info("Starting transformation for study: " + untransformedFilesPath.getFilename());

            utils.ensureDirs(transformedFilesPath);

            // Build transformation command
            Stream.of("-i", utils.getFile(untransformedFilesPath).getAbsolutePath(),
                    "-o", utils.getFile(transformedFilesPath).getAbsolutePath()).forEach(e -> command.add(e));

            // Run transformation command
            ProcessBuilder transformation = new ProcessBuilder(command);
            logger.info("Executing command: " + String.join(" ", transformation.command()));
            transformation.redirectErrorStream(true);
            logger.info("Step 1");
            transformation.redirectOutput(Redirect.appendTo(utils.getFile(logFile)));
            logger.info("Step 2");
            Process transformationProcess = transformation.start();

            transformationProcess.waitFor();

            // Interprete exit status of the process and return it
            ExitStatus exitStatus = null;
            if (transformationProcess.exitValue() == 0) {
                logger.info("Successfully completed transformation for study: " + untransformedFilesPath.getFilename());
                exitStatus = ExitStatus.SUCCESS;
            } else if (transformationProcess.exitValue() == 3) {
                exitStatus = ExitStatus.WARNING;
            } else {
                exitStatus = ExitStatus.ERROR;
            }
            return exitStatus;
        } catch (InterruptedException e) {
            throw new TransformerException("The transformation process has been interrupted by another process.", e);
        } catch (ResourceUtilsException e) {
            throw new TransformerException("Could not read from Resource.", e);
        } catch (IOException e) {
             throw new TransformerException("The study directory specified in the command do not exist, "
                    + "or you do not have permissions to execute the transformer command.", e);
        }
	}
}
