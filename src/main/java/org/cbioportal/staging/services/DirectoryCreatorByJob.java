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
package org.cbioportal.staging.services;

import org.cbioportal.staging.exceptions.DirectoryCreatorException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;


@Component
public class DirectoryCreatorByJob implements IDirectoryCreator {

    @Autowired
    private ResourceUtils utils;

    @Value("${etl.working.dir:${java.io.tmpdir}}")
    private Resource etlWorkingDir;

    @Value("${transformation.directory:}")
    private Resource transformationDir;

    @Override
    public Resource createStudyExtractDir(String timestamp, String studyId) throws DirectoryCreatorException {
        try {
            if (!etlWorkingDir.exists()) {
				throw new DirectoryCreatorException(
						"etl.working.dir does not exist on the local file system: " + etlWorkingDir);
			}
			if (utils.isFile(etlWorkingDir)) {
				throw new DirectoryCreatorException(
						"etl.working.dir points to a file on the local file system, but should point to a directory.: "
								+ etlWorkingDir);
			}
            return utils.createDirResource(etlWorkingDir, timestamp, studyId);
        } catch (ResourceCollectionException e) {
			throw new DirectoryCreatorException("Cannot create Resource.", e);
		}
    }

    @Override
    public Resource createTransformedStudyDir(String timestamp, String studyId, Resource untransformedStudyDir) throws DirectoryCreatorException {
        try {
            if (transformationDir != null) {
                if (utils.isFile(transformationDir)) {
                    throw new DirectoryCreatorException(
                            "transformation.directory points to a file on the local file system, but should point to a directory.: "
                                    + transformationDir);
                }
                return utils.createDirResource(transformationDir, timestamp, studyId);
            } else {
                return utils.createDirResource(untransformedStudyDir, "staging");
            }
        } catch (ResourceCollectionException e) {
            throw new DirectoryCreatorException("Cannot create Resource.", e);
        }
    }

    @Override
    public Resource getCentralShareLocationPath(Resource centralShareLocation, String timestamp) throws DirectoryCreatorException {
        try {
			if (utils.isFile(centralShareLocation)) {
				throw new DirectoryCreatorException(
						"central.share.location points to a file on the local file system, but should point to a directory.: "
								+ centralShareLocation);
			}
            Resource centralShareLocationPath = utils.createDirResource(centralShareLocation, timestamp);
            // TODO is this condition really needed? Better be resource-type independent
            if (! utils.getURL(centralShareLocation).toString().contains("s3:")) {
                utils.ensureDirs(centralShareLocation);
            }
            return centralShareLocationPath;
        } catch (ResourceCollectionException e) {
            throw new DirectoryCreatorException("Cannot create Resource.", e);
        }
    }
}
