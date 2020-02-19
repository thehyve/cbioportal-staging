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

import java.io.File;

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
@ConditionalOnProperty(value="cbioportal.mode", havingValue = "local")
public class LocalCommandBuilder implements ICommandBuilder {

    @Autowired
	private ResourceUtils utils;

	@Value("${portal.source:.}")
    private String portalSource;

    @Override
    public ProcessBuilder buildPortalInfoCommand(Resource portalInfoFolder) throws CommandBuilderException {
        try {
            String portalInfoPath = utils.getFile(portalInfoFolder).getAbsolutePath();

            ProcessBuilder portalInfoCmd = new ProcessBuilder("./dumpPortalInfo.pl", portalInfoPath);
            portalInfoCmd.directory(new File(portalSource+"/core/src/main/scripts"));
            return portalInfoCmd;
        } catch (ResourceUtilsException e) {
            throw new CommandBuilderException("File IO problem during the build of the Portal Info command", e);
        }
    }

    @Override
    public ProcessBuilder buildValidatorCommand(Resource studyPath, Resource portalInfoFolder, Resource reportFile) throws CommandBuilderException {
        try {
            String studyDirPath = utils.getFile(studyPath).getAbsolutePath();
            String reportFilePath = utils.getFile(reportFile).getAbsolutePath();
            String portalInfoPath = utils.getFile(portalInfoFolder).getAbsolutePath();

            ProcessBuilder validationCmd = new ProcessBuilder("./validateData.py", "-s", studyDirPath, "-p",
            portalInfoPath, "-html", reportFilePath, "-v");
            validationCmd.directory(new File(portalSource+"/core/src/main/scripts/importer"));
            return validationCmd;
        } catch (ResourceUtilsException e) {
            throw new CommandBuilderException("File IO problem during the build of the validator command", e);
        }
    }

	@Override
	public ProcessBuilder buildLoaderCommand(Resource studyPath) throws CommandBuilderException {
		try {
            String studyDirPath = utils.getFile(studyPath).getAbsolutePath();
            ProcessBuilder loaderCmd = new ProcessBuilder("./cbioportalImporter.py", "-s", studyDirPath);
            loaderCmd.directory(new File(portalSource+"/core/src/main/scripts/importer"));
            return loaderCmd;
		} catch (ResourceUtilsException e) {
			throw new CommandBuilderException("File IO problem for 'studyPath' in command builder.", e);
		}
	}
}
