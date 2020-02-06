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

import org.cbioportal.staging.exceptions.CommandBuilderException;
import org.cbioportal.staging.exceptions.LoaderException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class LoaderServiceImpl implements ILoaderService {
    private static final Logger logger = LoggerFactory.getLogger(LoaderServiceImpl.class);

    @Autowired
    private ICommandBuilder commandBuilder;

    @Autowired
    private ResourceUtils utils;

    @Override
    public ExitStatus load(Resource studyPath, Resource logFile) throws LoaderException {
        try {
            ProcessBuilder loaderCmd = commandBuilder.buildLoaderCommand(studyPath);
            // Apply loader command
            logger.info("Executing command: " + String.join(" ", loaderCmd.command()));
            loaderCmd.redirectErrorStream(true);
            loaderCmd.redirectOutput(Redirect.appendTo(utils.getFile(logFile)));
            Process loadProcess = loaderCmd.start();
            loadProcess.waitFor(); // Wait until loading is finished

            // Interprete exit status of the process and return it
            ExitStatus exitStatus = null;
            if (loadProcess.exitValue() == 0) {
                exitStatus = ExitStatus.SUCCESS;
            } else {
                exitStatus = ExitStatus.ERROR;
            }
            return exitStatus;
        } catch (InterruptedException e) {
            throw new LoaderException("The loading process has been interrupted by another process.", e);
        } catch (IOException e) {
            throw new LoaderException("The study directory specified in the command do not exist, "+
                "or you do not have permissions to execute the loading command.", e);
        } catch (CommandBuilderException e) {
            throw new LoaderException("A problem has occurred when building the loading command", e);
        } catch (ResourceCollectionException e) {
            throw new LoaderException("File IO problem during running of the Loader", e);
        }

	}
}
