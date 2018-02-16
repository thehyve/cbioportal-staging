/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.etl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

import org.cbioportal.staging.app.ScheduledScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.cbioportal.staging.exceptions.TransformerException;
import org.cbioportal.staging.services.EmailServiceImpl;

@Component
class Transformer {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);

	@Value("${etl.working.dir:file:///tmp}")
	private File etlWorkingDir;
	
	@Autowired
	EmailServiceImpl emailService;

	void transform(Integer id, List<String> studies, String transformationCommand) {
		File originPath = new File(etlWorkingDir.toPath()+"/"+id);
		File destinationPath = new File(etlWorkingDir.toPath()+"/"+id+"/staging");
		if (!destinationPath.exists()) {
			destinationPath.mkdir();
		}
		for (String study : studies) {
			try {
				logger.info("Starting transformation of study "+study);
				File dir = new File(originPath+"/"+study);
				File finalPath = new File(destinationPath+"/"+study);
				if (!finalPath.exists()) {
					finalPath.mkdir();
				}
				File[] directoryListing = dir.listFiles();
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

			} catch (TransformerException e) {
				//tell about error, continue with next study
				logger.error(e.getMessage()+". The app will continue with the next study.");
				e.printStackTrace();
				try {
					emailService.emailStudyError(study, e);
				} catch (Exception e1) {
					logger.error("The email could not be sent due to the error specified below.");
					e1.printStackTrace();
				}
			}
		}
		logger.info("Transformation step finished.");
	}

}
