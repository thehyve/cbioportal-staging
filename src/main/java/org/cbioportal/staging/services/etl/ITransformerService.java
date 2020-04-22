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

import java.util.List;

import org.cbioportal.staging.exceptions.TransformerException;
import org.cbioportal.staging.services.ExitStatus;
import org.springframework.core.io.Resource;

public interface ITransformerService {

    public List<String> parseCommandScript() throws TransformerException;

    public List<String> buildCommand(Resource untransformedFilesPath, Resource transformedFilesPath) throws TransformerException;

	public ExitStatus transform(Resource originPath, Resource finalPath, Resource logFile) throws TransformerException;

}
