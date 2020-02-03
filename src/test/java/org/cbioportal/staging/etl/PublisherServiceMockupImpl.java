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

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.services.PublisherServiceImpl;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class PublisherServiceMockupImpl extends PublisherServiceImpl {

    @Value("${central.share.location}")
    private String centralShareLocation;

    public Map<String, String> publish(String date, Map<String, File> initialLogFiles) throws IOException {
        Map<String, String> finalLogFiles = new HashMap<String, String>();
        for (String logName : initialLogFiles.keySet()) {
            File initialLogFile = initialLogFiles.get(logName);
            String finalLogFile = publish(initialLogFile, date);
            finalLogFiles.put(logName, finalLogFile);
        }
        return finalLogFiles;
    }

    public String publish(File file, String date) throws IOException {
        String centralShareLocationPath = getCentralShareLocationPath(centralShareLocation, date);
        return centralShareLocationPath+"/"+file.getName();
    }

    @Override
    public String getCentralShareLocationPath(String centralShareLocation, String date) {
        return centralShareLocation+"/"+date;
    }

}
