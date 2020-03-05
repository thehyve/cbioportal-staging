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
package org.cbioportal.staging.services.publish;

import static com.pivovarit.function.ThrowingFunction.sneaky;

import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.cbioportal.staging.exceptions.PublisherException;
import org.cbioportal.staging.services.resource.IResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class PublisherServiceImpl implements IPublisherService {

    private static final Logger logger = LoggerFactory.getLogger(PublisherServiceImpl.class);

    @Value("${central.share.location:}")
    private Resource centralShareLocation;

    @Value("${transformation.directory:}")
    private Resource transformationDirectory;

    @Value("${etl.working.dir:}")
    private Resource etlWorkingDir;

    @Autowired
    IResourceProvider resourceProvider;

    public Map<String, Resource> publishFiles(Map<String, Resource> logFiles) throws PublisherException {

        if (centralShareLocation == null) {
            logger.info("No central.share.location defined. Skipping publishing of log files.");
            return null;
        }

        if (logFiles == null) {
            throw new PublisherException("Argument 'logFiles' cannot be null.");
        }

        return logFiles.entrySet().stream()
                .collect(Collectors.toMap(Entry::getKey, sneaky(e -> publishOneFile(e.getValue()))));
    }

    private Resource publishOneFile(Resource logFile) throws PublisherException {
        try {
            Resource localRootDir = transformationDirectory != null? transformationDirectory : etlWorkingDir;
            String filePathRelative = logFile.getURL().toString().replaceFirst(localRootDir.getURL().toString(), "");
            filePathRelative = filePathRelative.substring(0, filePathRelative.lastIndexOf("/"));
            Resource remoteDestinationDir = resourceProvider.getResource(centralShareLocation.getURL() + "/" + filePathRelative);
            return resourceProvider.copyToRemote(remoteDestinationDir, logFile);
        } catch (Exception e) {
            throw new PublisherException("There has been an error when getting the Central Share Location URL or copying it to the Log File Path.", e);
        }
    }

}
