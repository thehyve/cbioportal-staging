/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.etl;

import java.io.File;
import java.util.List;

import org.cbioportal.staging.app.ScheduledScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import org.cbioportal.staging.exceptions.TransformerException;
import org.cbioportal.staging.services.EmailService;
import org.cbioportal.staging.services.TransformerService;

@Component
class Transformer {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);

	@Value("${etl.working.dir:file:///tmp}")
	private File etlWorkingDir;
	
	@Autowired
	EmailService emailService;
	
	@Autowired
	TransformerService transformerService;

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
				transformerService.transform(dir, finalPath);
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
