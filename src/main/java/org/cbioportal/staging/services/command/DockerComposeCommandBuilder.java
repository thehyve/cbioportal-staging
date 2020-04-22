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
package org.cbioportal.staging.services.command;

import java.io.IOException;

import org.cbioportal.staging.exceptions.CommandBuilderException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConditionalOnProperty(value="cbioportal.mode", havingValue = "compose")
public class DockerComposeCommandBuilder implements ICommandBuilder {

    @Autowired
	private ResourceUtils utils;

	@Value("${cbioportal.compose.service}")
	private String cbioportalDockerService;

    @Override
    public ProcessBuilder buildPortalInfoCommand(Resource portalInfoFolder) throws CommandBuilderException {

        return new ProcessBuilder("docker-compose", "run", "--rm",
        "-w", "/cbioportal/core/src/main/scripts",
        cbioportalDockerService, "./dumpPortalInfo.pl", "/portalinfo");
    }

    @Override
    public ProcessBuilder buildValidatorCommand(Resource studyPath, Resource portalInfoFolder, Resource reportFile) throws CommandBuilderException {
        try {
            //make sure report file exists first, otherwise docker will map it as a folder:
            utils.getFile(reportFile).getParentFile().mkdirs();
            utils.getFile(reportFile).createNewFile();

            String studyDirPath = utils.getFile(studyPath).getAbsolutePath();
            String reportFilePath = utils.getFile(reportFile).getAbsolutePath();
            String portalInfoPath = utils.getFile(portalInfoFolder).getAbsolutePath();

            //docker command:
            ProcessBuilder validationCmd = new ProcessBuilder ("docker-compose", "run", "--rm",
            "-v", studyDirPath + ":/study:ro",
            "-v", reportFilePath + ":/outreport.html",
            "-v", portalInfoPath + ":/portalinfo:ro",
            cbioportalDockerService,
            "validateData.py", "-p", "/portalinfo", "-s", "/study", "--html=/outreport.html");

            return validationCmd;
        } catch (IOException e) {
            throw new CommandBuilderException("The report file could not be created.", e);
        } catch (ResourceUtilsException e) {
            throw new CommandBuilderException("File IO problem during the build of the validator command", e);
        }
    }

    @Override
    public ProcessBuilder buildLoaderCommand(Resource studyPath) throws CommandBuilderException {
        try {
            String studyDirPath = utils.getFile(studyPath).getAbsolutePath();
            return new ProcessBuilder ("docker-compose", "run", "--rm",
                "-v", studyDirPath + ":/study:ro",
                cbioportalDockerService,
                "cbioportalImporter.py", "-s", "/study");
        } catch (ResourceUtilsException e) {
            throw new CommandBuilderException("CommandBuilder experiences File IO problems.", e);
        }
    }
}
