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

import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.exceptions.PublisherException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/*
    Calls a command when study loading is finished.
 */
@Component
public class PublisherServiceImpl implements PublisherService {

    @Value("${central.share.location}")
    private Resource centralShareLocation;

    @Value("${central.share.location.web.address:}")
    private Resource centralShareLocationWebAddress;

    @Autowired
    private ResourceUtils utils;

    public Map<String, Resource> publish(String date, Map<String, Resource> initialLogFiles) throws PublisherException {
        try {
            Map<String, Resource> finalLogFiles = new HashMap<>();
            for (String logName : initialLogFiles.keySet()) {
                Resource initialLogFile = initialLogFiles.get(logName);
                Resource finalLogFile;
                finalLogFile = publish(initialLogFile, date);
                finalLogFiles.put(logName, finalLogFile);
            }
            return finalLogFiles;
        } catch (ResourceCollectionException e) {
            throw new PublisherException("Could not publish log files");
        }
    }

    private Resource publish(Resource file, String date) throws ResourceCollectionException {

        //Set the centralShareLocationWebAddress to the centralShareLocation path if no address is available
		if (centralShareLocationWebAddress == null) {
			centralShareLocationWebAddress = centralShareLocation;
        }

        //Get Central Share Location Path and copy the file to the path
        Resource centralShareLocationPath = getCentralShareLocationPath(centralShareLocation, date);
        return utils.copyResource(centralShareLocationPath, file, file.getFilename());
    }

    private Resource getCentralShareLocationPath(Resource centralShareLocation, String date)
            throws ResourceCollectionException {
        Resource centralShareLocationPath = utils.createDirResource(centralShareLocation, date);
        if (! utils.getURL(centralShareLocation).toString().contains("s3:")) {
            utils.ensureDirs(centralShareLocation);
        }
        return centralShareLocationPath;
    }
}
