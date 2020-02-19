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

            // by job timestamp --> studies
            if (dirFormat.equals("job")) {
                return createDir(etlWorkingDir, study.getTimestamp(), study.getStudyId());
            }

            // by study --> version/timestamp
            String versionLabel = versionFormat.equals("version")? study.getVersion() : study.getTimestamp();
            return createDir(etlWorkingDir, study.getStudyId(), versionLabel);

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
                            // by job timestamp --> studies
                if (dirFormat.equals("job")) {
                    return createDir(transformationDir, study.getTimestamp(), study.getStudyId());
                }

                // by study --> version/timestamp
                String versionLabel = versionFormat.equals("version")? study.getVersion() : study.getTimestamp();
                return createDir(transformationDir, study.getStudyId(), versionLabel);
            } else {
                return utils.createDirResource(untransformedStudyDir, "staging");
            }
        } catch (ResourceUtilsException e) {
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
            if (utils.getURL(centralShareLocation).toString().contains("file:")) {
                return utils.createDirResource(centralShareLocation, timestamp);
            }
            return centralShareLocation.createRelative(timestamp);
        } catch (ResourceUtilsException e) {
            throw new DirectoryCreatorException("Cannot create Resource.", e);
        } catch (IOException e) {
            throw new DirectoryCreatorException("Cannot create the relative folder.", e);
        }
    }

    private Resource createDir(Resource dir, String frstLevelDirName, String scndLevelDirName)
            throws ResourceUtilsException {
        return utils.createDirResource(dir, frstLevelDirName, scndLevelDirName);
    }
}
