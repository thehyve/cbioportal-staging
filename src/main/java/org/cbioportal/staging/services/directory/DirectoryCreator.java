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

import org.cbioportal.staging.exceptions.DirectoryCreatorException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.cbioportal.staging.services.resource.Study;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;


@Component
public class DirectoryCreator implements IDirectoryCreator {

    @Autowired
    private ResourceUtils utils;

    @Value("${etl.working.dir:}")
    private Resource etlWorkingDir;

    @Value("${transformation.directory:}")
    private Resource transformationDir;

    @Value("${etl.dir.format:job}")
    private String dirFormat;

    @Value("${etl.dir.version.format:version}")
    private String versionFormat;

    @Override
    public Resource createStudyExtractDir(Study study) throws DirectoryCreatorException {
        try {

            if (etlWorkingDir  == null) {
                throw new DirectoryCreatorException("etl.working.dir not defined. Please check the application properties.");
            }

            if (!etlWorkingDir.exists()) {
				throw new DirectoryCreatorException(
						"etl.working.dir does not exist on the local file system: " + etlWorkingDir);
			}
			if (utils.isFile(etlWorkingDir)) {
				throw new DirectoryCreatorException(
						"etl.working.dir points to a file on the local file system, but should point to a directory.: "
								+ etlWorkingDir);
            }

            return createDir(etlWorkingDir, study);

        } catch (ResourceUtilsException e) {
			throw new DirectoryCreatorException("Cannot create Resource.", e);
		}
    }

    @Override
    public Resource createTransformedStudyDir(Study study, Resource untransformedStudyDir) throws DirectoryCreatorException {
        try {
            if (transformationDir != null) {
                if (utils.isFile(transformationDir)) {
                    throw new DirectoryCreatorException(
                            "transformation.directory points to a file on the local file system, but should point to a directory.: "
                                    + transformationDir);
                }

                return createDir(transformationDir, study);

            } else {
                return utils.createDirResource(untransformedStudyDir, "staging");
            }
        } catch (ResourceUtilsException e) {
            throw new DirectoryCreatorException("Cannot create Resource.", e);
        }
    }

    private Resource createDir(Resource dir, Study study)
            throws ResourceUtilsException {
                // by job timestamp --> studies
                if (dirFormat.equals("job")) {
                    return utils.createDirResource(dir, study.getTimestamp(), study.getStudyId());
                }

                // by study --> version/timestamp
                String versionLabel = versionFormat.equals("version")? study.getVersion() : study.getTimestamp();
                return utils.createDirResource(dir, study.getStudyId(), versionLabel);
    }
}
