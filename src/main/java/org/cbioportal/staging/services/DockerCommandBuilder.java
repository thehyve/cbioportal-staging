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

import org.cbioportal.staging.exceptions.CommandBuilderException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;


@Component
public class DockerCommandBuilder implements ICommandBuilder {

    @Value("${cbioportal.docker.image}")
    private String cbioportalDockerImage;

    @Value("${cbioportal.docker.network}")
    private String cbioportalDockerNetwork;

    @Value("${cbioportal.docker.properties}")
    private File cbioportalDockerProperties;

    @Override
    public ProcessBuilder buildLoaderCommand(Resource studyPath) throws CommandBuilderException {
        ProcessBuilder loaderCmd;
        if (!cbioportalDockerImage.equals("") && !cbioportalDockerNetwork.equals("")) {
            loaderCmd = new ProcessBuilder ("docker", "run", "-i", "--rm", "--net", cbioportalDockerNetwork,
                    "-v", studyPath.toString()+":/study:ro",
                    "-v", cbioportalDockerProperties.getAbsolutePath()+":/cbioportal/portal.properties:ro",
                    cbioportalDockerImage,
                    "cbioportalImporter.py", "-s", "/study");
        } else {
            throw new CommandBuilderException("No Docker image or network has been specified in the application.properties.");
        }
        return loaderCmd;
	}
}
