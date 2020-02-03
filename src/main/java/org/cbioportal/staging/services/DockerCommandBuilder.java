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
import java.io.IOException;

import org.cbioportal.staging.exceptions.CommandBuilderException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;


@Component
public class DockerCommandBuilder implements ICommandBuilder {

    @Autowired
	private ResourceUtils utils;

	@Value("${cbioportal.docker.image}")
	private String cbioportalDockerImage;

    @Value("${cbioportal.docker.network}")
    private String cbioportalDockerNetwork;

    @Value("${cbioportal.docker.properties}")
    private File cbioportalDockerPropertiesFile;

    private String propertiesFile = utils.stripResourceTypePrefix(cbioportalDockerPropertiesFile.getAbsolutePath());

    @Override
    public ProcessBuilder buildPortalInfoCommand(Resource portalInfoFolder) throws CommandBuilderException {
        try {
            ProcessBuilder portalInfoCmd = new ProcessBuilder("docker", "run", "--rm", "--net", cbioportalDockerNetwork,
                                                        "-v", utils.getFile(portalInfoFolder).toString()+":/portalinfo",
                                                        "-v", propertiesFile+":/cbioportal/portal.properties:ro",
                                                        "-w", "/cbioportal/core/src/main/scripts",
                                                        cbioportalDockerImage, "./dumpPortalInfo.pl", "/portalinfo");
            return portalInfoCmd;
        } catch (ResourceCollectionException e) {
            throw new CommandBuilderException("File IO problem during the build of the Portal Info command", e);
        }
        
    }

    @Override
    public ProcessBuilder buildValidatorCommand(Resource studyPath, Resource portalInfoFolder, Resource reportFile) throws CommandBuilderException {
        try {
            //make sure report file exists first, otherwise docker will map it as a folder:
            utils.getFile(reportFile).getParentFile().mkdirs();
            utils.getFile(reportFile).createNewFile();
            //docker command:
            ProcessBuilder validationCmd = new ProcessBuilder ("docker", "run", "-i", "--rm",
                                                            "-v", utils.getFile(studyPath).toString()+":/study:ro", "-v",
                                                            utils.getFile(reportFile).getAbsolutePath()+":/outreport.html",
                                                            "-v", utils.getFile(portalInfoFolder).toString()+ ":/portalinfo:ro",
                                                            "-v", propertiesFile+":/cbioportal/portal.properties:ro", cbioportalDockerImage,
                                                            "validateData.py", "-p", "/portalinfo", "-s", "/study", "--html=/outreport.html");
            return validationCmd;
        } catch (IOException e) {
            throw new CommandBuilderException("The report file could not be created.", e);
        } catch (ResourceCollectionException e) {
            throw new CommandBuilderException("File IO problem during the build of the validator command", e);
        }
    }

    @Override
    public ProcessBuilder buildLoaderCommand(Resource studyPath) throws CommandBuilderException {
        ProcessBuilder loaderCmd;
        if (!cbioportalDockerImage.equals("") && !cbioportalDockerNetwork.equals("")) {
            loaderCmd = new ProcessBuilder ("docker", "run", "-i", "--rm", "--net", cbioportalDockerNetwork,
                    "-v", studyPath.toString()+":/study:ro",
                    "-v", cbioportalDockerPropertiesFile.getAbsolutePath()+":/cbioportal/portal.properties:ro",
                    cbioportalDockerImage,
                    "cbioportalImporter.py", "-s", "/study");
        } else {
            throw new CommandBuilderException("No Docker image or network has been specified in the application.properties.");
        }
        return loaderCmd;
    }
}
