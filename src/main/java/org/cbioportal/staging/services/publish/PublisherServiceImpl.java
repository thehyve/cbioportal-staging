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

import org.cbioportal.staging.exceptions.DirectoryCreatorException;
import org.cbioportal.staging.exceptions.PublisherException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.directory.IDirectoryCreator;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/*
    Calls a command when study loading is finished.
 */
@Component
public class PublisherServiceImpl implements IPublisherService {

    private static final Logger logger = LoggerFactory.getLogger(PublisherServiceImpl.class);

    @Value("${central.share.location:}")
    private Resource centralShareLocation;

    @Autowired
    private ResourceUtils utils;

    @Autowired
    private IDirectoryCreator directoryCreator;

    public Map<String, Resource> publish(String date, Map<String, Resource> logFiles) throws PublisherException {

        if (centralShareLocation == null) {
            logger.info("No central.share.location was defined. Skipping publishing of log files.");
        }

        if (date == null) {
            throw new PublisherException("Argument 'date' may not be null.");
        }

        if (logFiles == null) {
            throw new PublisherException("Argument 'logFiles' may not be null.");
        }

        return logFiles.entrySet().stream()
            .collect(Collectors
                .toMap(Entry::getKey, sneaky(e -> publish(e.getValue(), date))
                )
            );
    }

    private Resource publish(Resource logFile, String timestamp) throws PublisherException {
        try {
            Resource centralShareLocationPath = directoryCreator.getCentralShareLocationPath(centralShareLocation, timestamp);
            // TODO is this conditional really needed (does it throw an error if not)?
            if (! utils.getURL(centralShareLocation).toString().contains("s3:")) {
                utils.ensureDirs(centralShareLocation);
            }
            return utils.copyResource(centralShareLocationPath, logFile, logFile.getFilename());
        } catch (DirectoryCreatorException e) {
            throw new PublisherException("There has been an error creating the Central Share Location", e);
        } catch (ResourceUtilsException e) {
            throw new PublisherException("There has been an error when getting the Central Share Location URL or copying it to the Log File Path.", e);
        }
    }

}
