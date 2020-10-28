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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;

import com.pivovarit.function.ThrowingFunction;

import org.cbioportal.staging.exceptions.PublisherException;
import org.cbioportal.staging.services.directory.IDirectoryCreator;
import org.cbioportal.staging.services.resource.IResourceProvider;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.cbioportal.staging.services.resource.Study;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class PublisherServiceImpl implements IPublisherService {

    private static final Logger logger = LoggerFactory.getLogger(PublisherServiceImpl.class);

    @Value("${central.share.location:@null}")
    private Resource centralShareLocation;

    @Value("${transformation.directory:}")
    private Resource transformationDirectory;

    @Value("${etl.working.dir:}")
    private Resource etlWorkingDir;

    @Autowired
    IResourceProvider resourceProvider;

    @Autowired
	private IDirectoryCreator directoryCreator;

    @Autowired
    private ResourceUtils utils;

    private List<Resource> publishedFiles = new ArrayList<>();

    public Map<Study, Resource> publishFiles(Map<Study, Resource> logFiles) throws PublisherException {

        if (centralShareLocation == null) {
            logger.info("No central.share.location defined. Skipping publishing of log files.");
            return null;
        }

        if (logFiles == null) {
            throw new PublisherException("Argument 'logFiles' cannot be null.");
        }

        Map<Study, Resource> files = logFiles.entrySet().stream()
            .collect(Collectors.toMap(Entry::getKey, ThrowingFunction.sneaky(e -> publishOneFile(e.getKey(), e.getValue()))));

        publishedFiles.addAll(files.values());

        return files;
    }

    private Resource publishOneFile(Study study, Resource logFile) throws PublisherException {
        try {
            //Resource localRootDir = transformationDirectory != null? transformationDirectory : etlWorkingDir;
            String intermediatePath = directoryCreator.getIntermediatePath(study);
            // logFile.getURL().toString().replaceFirst(localRootDir.getURL().toString(), "");
            // if (filePathRelative.lastIndexOf("/") > -1) {
            //     filePathRelative = filePathRelative.substring(0, filePathRelative.lastIndexOf("/") + 1);
            // } else {
            //     filePathRelative = "";
            // }
            Resource remoteDestinationDir = resourceProvider.getResource(utils.trimPathRight(centralShareLocation.getURL().toString()) + "/" + intermediatePath);
            return resourceProvider.copyToRemote(remoteDestinationDir, logFile);
        } catch (Exception e) {
            throw new PublisherException("There has been an error when getting the Central Share Location URL or copying it to the Log File Path.", e);
        }
    }

    public List<Resource> getPublishedFiles() {
        return publishedFiles;
    }

    public void clear() {
        publishedFiles.clear();
    }

}
