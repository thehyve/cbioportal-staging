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
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Profile("local")
@Component
@Primary
public class LocalCommandBuilder implements ICommandBuilder {

	@Value("${portal.source:.}")
	private String portalSource;

	@Autowired
	private ResourceUtils utils;

	@Override
	public ProcessBuilder buildLoaderCommand(Resource studyPath) throws CommandBuilderException {
		try {
		ProcessBuilder loaderCmd = new ProcessBuilder("./cbioportalImporter.py", "-s", utils.getFile(studyPath).getAbsolutePath());
        loaderCmd.directory(new File(portalSource+"/core/src/main/scripts/importer"));
        return loaderCmd;
		} catch (ResourceCollectionException e) {
			throw new CommandBuilderException("File IO problem for 'studyPath' in command builder.", e);
		}
	}
}
