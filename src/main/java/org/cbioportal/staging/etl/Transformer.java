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
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.TransformerException;
import org.cbioportal.staging.services.EmailService;
import org.cbioportal.staging.services.TransformerService;

@Component
public class Transformer {
	private static final Logger logger = LoggerFactory.getLogger(Transformer.class);

	@Value("${etl.working.dir:java.io.tmpdir}")
	private File etlWorkingDir;
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private TransformerService transformerService;

	void transform(Integer id, List<String> studies, String transformationCommand) throws InterruptedException, ConfigurationException, IOException {
		File originPath = new File(etlWorkingDir.toPath()+"/"+id);
		File destinationPath = new File(etlWorkingDir.toPath()+"/"+id+"/staging");
		if (!destinationPath.exists()) {
			destinationPath.mkdir();
		}
		for (String study : studies) {
			try {
				File dir = new File(originPath+"/"+study);
				File finalPath = new File(destinationPath+"/"+study);
				transformerService.transform(dir, finalPath);
			} catch (TransformerException e) {
				//tell about error, continue with next study
				logger.error(e.getMessage()+". The app will skip this study.");
				e.printStackTrace();
				try {
					emailService.emailStudyError(study, e);
				} catch (Exception e1) {
					logger.error("The email could not be sent due to the error specified below.", e1);
					e1.printStackTrace();
				}
			}
		}
		logger.info("Transformation step finished.");
	}

}
