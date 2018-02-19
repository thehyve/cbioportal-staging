/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.services;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;

import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.exceptions.TransformerException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class TransformerServiceImpl implements TransformerService {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);

	@Value("${cbioportal.mode}")
	private String cbioportalMode;
	
	@Value("${cbioportal.docker.image}")
	private String cbioportalDockerImage;
	
	@Value("${cbioportal.docker.network}")
	private String cbioportalDockerNetwork;
	
	@Value("${portal.home}")
	private String portalHome;
	
	@Value("${central.share.location}")
	private File centralShareLocation;
	
	@Override
	public void transform(File originPath, File finalPath) throws TransformerException {
		if (!finalPath.exists()) {
			finalPath.mkdir();
		}
		File[] directoryListing = originPath.listFiles();
		for (File file : directoryListing) {
			try {
				InputStream is = new FileInputStream(file);
				//TODO: transformation step
				try {
					Files.copy(is, Paths.get(finalPath+"/"+file.getName()));
					logger.info("The file "+file.getName()+" has been copied successfully.");
				} catch (IOException e) {
					e.printStackTrace();
					throw new TransformerException("Error when copying the file: "+file.getAbsolutePath(), e);
				}
			} catch (FileNotFoundException e1) {
				throw new TransformerException("The following file was not found: "+file.getAbsolutePath(), e1);
			}
		}
		
	}

}
