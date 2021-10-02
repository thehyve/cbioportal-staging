/*
* Copyright (c) 2020 The Hyve B.V.
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
package org.cbioportal.staging.services.directory;

import java.io.IOException;
import org.cbioportal.staging.exceptions.DirectoryCreatorException;
import org.cbioportal.staging.services.resource.Study;
import org.springframework.core.io.Resource;

public interface IDirectoryCreator {

    public Resource createStudyExtractDir(Resource studyExtractDir) throws DirectoryCreatorException;

    public Resource createTransformedStudyDir(Resource studyTransformDir) throws DirectoryCreatorException;

    Resource getStudyExtractDir(Study study) throws DirectoryCreatorException;

    Resource getStudyTransformDir(Study study) throws DirectoryCreatorException;

    public String getIntermediatePath(Study study) throws DirectoryCreatorException;

}
