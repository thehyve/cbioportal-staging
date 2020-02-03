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
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Profile("local")
@Component
@Primary
public class LocalCommandBuilder implements ICommandBuilder {

	@Value("${portal.source:.}")
	private String portalSource;

	@Override
	public ProcessBuilder buildLoaderCommand(File studyPath) throws CommandBuilderException {
        ProcessBuilder loaderCmd = new ProcessBuilder("./cbioportalImporter.py", "-s", studyPath.toString());
        loaderCmd.directory(new File(portalSource+"/core/src/main/scripts/importer"));
        return loaderCmd;
	}
}