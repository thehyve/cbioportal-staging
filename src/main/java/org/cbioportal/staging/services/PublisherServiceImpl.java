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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;


/*
    Calls a command when study loading is finished.
 */
@Component
public class PublisherServiceImpl implements PublisherService {

	private static final Logger logger = LoggerFactory.getLogger(PublisherServiceImpl.class);

	@Value("${central.share.location}")
	private String centralShareLocation;

	@Value("${central.share.location.web.address:}")
	private String centralShareLocationWebAddress;

    @Autowired
    private ResourcePatternResolver resourcePatternResolver;

    public Map<String, String> publish(String date, Map<String, File> initialLogFiles) throws IOException {
        Map<String, String> finalLogFiles = new HashMap<String, String>();
        for (String logName : initialLogFiles.keySet()) {
            File initialLogFile = initialLogFiles.get(logName);
            String finalLogFile = publish(initialLogFile, date);
            finalLogFiles.put(logName, finalLogFile);
        }
        return finalLogFiles;
    }

	private String publish(File file, String date) throws IOException {

        //Set the centralShareLocationWebAddress to the centralShareLocation path if no address is available
		if (centralShareLocationWebAddress.equals("")) {
			centralShareLocationWebAddress = centralShareLocation;
        }

        //Get Central Share Location Path and copy the file to the path
        String centralShareLocationPath = getCentralShareLocationPath(centralShareLocation, date);
        copyToResource(file, centralShareLocationPath);

        //Return the path where the file has been copied
		return centralShareLocationWebAddress+"/"+date+"/"+file.getName();
    }

    public void copyToResource(File filePath, String resourceOut) throws IOException { //TODO: isn't this method overlapping with the one in ResourceUtils?
		String resourcePath = resourceOut+"/"+filePath.getName();
		Resource resource;
		if (resourcePath.startsWith("file:")) {
			resource = new FileSystemResource(resourcePath.replace("file:", ""));
		}
		else {
			resource = this.resourcePatternResolver.getResource(resourcePath);
        }
		WritableResource writableResource = (WritableResource) resource;
        try (OutputStream outputStream = writableResource.getOutputStream();
            InputStream inputStream = new FileInputStream(filePath)) {
                IOUtils.copy(inputStream, outputStream);
		}
    }

    public String getCentralShareLocationPath(String centralShareLocation, String date) {
        String centralShareLocationPath = centralShareLocation+"/"+date;
        if (!centralShareLocationPath.startsWith("s3:")) {
            File cslPath = new File(centralShareLocation+"/"+date);
            if (centralShareLocationPath.startsWith("file:")) {
                cslPath = new File(centralShareLocationPath.replace("file:", ""));
            }
            logger.info("Central Share Location path: "+cslPath.getAbsolutePath());
            //If the Central Share Location path does not exist, create it:
            if (!cslPath.exists()) {
                cslPath.mkdirs();
            }
        }
        return centralShareLocationPath;
    }
}
