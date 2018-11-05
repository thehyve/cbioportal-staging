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
import java.util.List;
import java.util.Map;

import org.cbioportal.staging.services.EmailService;
import org.cbioportal.staging.services.LoaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

@Component
public class Loader {
	private static final Logger logger = LoggerFactory.getLogger(Loader.class);
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
	private LoaderService loaderService;
	
	@Value("${etl.working.dir:java.io.tmpdir}")
	private File etlWorkingDir;

	@Value("${central.share.location}")
	private String centralShareLocation;
	
	@Value("${central.share.location.portal:}")
	private String centralShareLocationPortal;
	
	boolean load(Integer id, List<String> studies, Map<String, String> filesPath) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException, RuntimeException {
		Map<String, String> statusStudies = new HashMap<String, String>();
		//Get studies from appropriate staging folder
		File originPath = new File(etlWorkingDir.toPath()+"/"+id+"/staging");
		for (String study : studies) {
			try {
				logger.info("Starting loading of study "+study+". This can take some minutes.");
				File studyPath = new File(originPath+"/"+study);
				String loadingLog = loaderService.load(study, studyPath, id, centralShareLocation+"/"+id);
				filesPath.put(study+" loading log", loadingLog);
				logger.info("Loading of study "+study+" finished.");
				statusStudies.put(study, "SUCCESSFULLY LOADED");
			} catch (RuntimeException e) {
				throw new RuntimeException(e);
			} catch (Exception e) {
				//tell about error, continue with next study
				logger.error(e.getMessage()+". The app will skip this study.");
				statusStudies.put(study, "ERRORS");
				e.printStackTrace();
			}
		}
		if (centralShareLocationPortal.equals("")) {
			centralShareLocationPortal = centralShareLocation;
		}
		emailService.emailStudiesLoaded(statusStudies, filesPath);
		return true;
	}
}
