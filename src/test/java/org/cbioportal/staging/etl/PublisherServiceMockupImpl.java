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
package org.cbioportal.staging.etl;

import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.exceptions.PublisherException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.PublisherServiceImpl;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;
import org.springframework.core.io.Resource;

@TestComponent
public class PublisherServiceMockupImpl extends PublisherServiceImpl {

    @Value("${central.share.location}")
    private Resource centralShareLocation;

    @Autowired
    private ResourceUtils utils;

    @Override
    public Map<String, Resource> publish(String date, Map<String, Resource> initialLogFiles)
            throws PublisherException {
        try {
            Map<String, Resource> finalLogFiles = new HashMap<>();
            for (String logName : initialLogFiles.keySet()) {
                Resource initialLogFile = initialLogFiles.get(logName);
                Resource finalLogFile = this.publish(initialLogFile, date);
                finalLogFiles.put(logName, finalLogFile);
            }
            return finalLogFiles;
        } catch (ResourceCollectionException e) {
            throw new PublisherException("Could not publish log files");
        }
    }

    @Override
    protected Resource publish(Resource file, String date) throws ResourceCollectionException {
        Resource centralShareLocationPath = getCentralShareLocationPath(centralShareLocation, date);
        return utils.getResource(centralShareLocationPath, utils.getFile(file).getName());
    }

}
