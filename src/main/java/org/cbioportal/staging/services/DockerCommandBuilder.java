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
    private Resource cbioportalDockerPropertiesFile;


    @Override
    public ProcessBuilder buildPortalInfoCommand(Resource portalInfoFolder) throws CommandBuilderException {
        try {
            String propertiesFilePath = utils.getFile(cbioportalDockerPropertiesFile).getAbsolutePath();
            String portalInfoPath = utils.getFile(portalInfoFolder).getAbsolutePath();

            ProcessBuilder portalInfoCmd = new ProcessBuilder("docker", "run", "--rm", "--net", cbioportalDockerNetwork,
            "-v", portalInfoPath + ":/portalinfo",
            "-v", propertiesFilePath + ":/cbioportal/portal.properties:ro",
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

            String propertiesFilePath = utils.getFile(cbioportalDockerPropertiesFile).getAbsolutePath();
            String studyDirPath = utils.getFile(studyPath).getAbsolutePath();
            String reportFilePath = utils.getFile(reportFile).getAbsolutePath();
            String portalInfoPath = utils.getFile(portalInfoFolder).getAbsolutePath();

            //docker command:
            ProcessBuilder validationCmd = new ProcessBuilder ("docker", "run", "-i", "--rm",
            "-v", studyDirPath + ":/study:ro",
            "-v", reportFilePath + ":/outreport.html",
            "-v", portalInfoPath + ":/portalinfo:ro",
            "-v", propertiesFilePath + ":/cbioportal/portal.properties:ro", cbioportalDockerImage,
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
        try {
            ProcessBuilder loaderCmd;
            if (!cbioportalDockerImage.equals("") && !cbioportalDockerNetwork.equals("")) {
                String propertiesFilePath = utils.getFile(cbioportalDockerPropertiesFile).getAbsolutePath();
                String studyDirPath = utils.getFile(studyPath).getAbsolutePath();
                loaderCmd = new ProcessBuilder ("docker", "run", "-i", "--rm", "--net", cbioportalDockerNetwork,
                "-v", studyDirPath + ":/study:ro",
                "-v", propertiesFilePath+":/cbioportal/portal.properties:ro",
                cbioportalDockerImage,
                "cbioportalImporter.py", "-s", "/study");
            } else {
                throw new CommandBuilderException("No Docker image or network has been specified in the application.properties.");
            }
            return loaderCmd;
        } catch (ResourceCollectionException e) {
            throw new CommandBuilderException("CommandBuilder experiences File IO problems.", e);
        }
    }
}
