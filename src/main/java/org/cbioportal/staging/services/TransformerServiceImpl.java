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

import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.cbioportal.staging.etl.Transformer;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.TransformerException;
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

    @Autowired
    private ResourcePatternResolver resourceResolver;

    @Autowired
    private ResourceUtils utils;

    @Override
    public ExitStatus transform(Resource untransformedFilesPath, Resource transformedFilesPath, Resource logFile)
            throws TransformerException, ConfigurationException, IOException {

        if (transformationCommandScript.equals("")) {
            throw new TransformerException(
                    "No transformation command script has been specified in the application.properties.");
        }

        List<String> command = Stream.of(transformationCommandScript.trim().split("\\s+")).collect(Collectors.toList());
        Resource script = resourceResolver.getResource(command.get(0));
        script.getFile().setExecutable(true); // required for tests: x-permissions are stripped in maven target resource
                                              // dir

        try {
            if (!script.exists()) {
                throw new ConfigurationException(
                        "Transformation command script specified in the application.properties points does not exist at the indication location.");
            }

            if (script.getFile().isDirectory()) {
                throw new ConfigurationException(
                        "Transformation command script specified in the application.properties points to directory.");
            }
            String scriptPath = utils.stripResourceTypePrefix(script.getURL().toString());
            command.set(0, scriptPath);

            logger.info("Starting transformation for study: " + untransformedFilesPath.getFilename());

            utils.ensureDirs(transformedFilesPath);

            // Build transformation command
            Stream.of("-i", utils.getFile(untransformedFilesPath).getAbsolutePath(), "-o",
                    utils.getFile(transformedFilesPath).getAbsolutePath()).forEach(e -> command.add(e));

            // Run transformation command
            ProcessBuilder transformation = new ProcessBuilder(command);
            logger.info("Executing command: " + String.join(" ", transformation.command()));
            transformation.redirectErrorStream(true);
            transformation.redirectOutput(Redirect.appendTo(utils.getFile(logFile)));
            Process transformationProcess = transformation.start();

            transformationProcess.waitFor();

            // Interprete exit status of the process and return it
            ExitStatus exitStatus = null;
            if (transformationProcess.exitValue() == 0) {
                exitStatus = ExitStatus.SUCCESS;
            } else if (transformationProcess.exitValue() == 3) {
                exitStatus = ExitStatus.WARNINGS;
            } else {
                exitStatus = ExitStatus.ERRORS;
            }
            return exitStatus;

            // } catch (FileNotFoundException e) {
            // throw new TransformerException("The following file path was not found:
            // "+utils.getFile(untransformedFilesPath).getAbsolutePath(), e);
        } catch (InterruptedException e) {
            throw new TransformerException("The transformation process has been interrupted by another process.", e);
        } catch (ResourceCollectionException e) {
            throw new TransformerException("Could not read from Resource.", e);
        }
	}
}
