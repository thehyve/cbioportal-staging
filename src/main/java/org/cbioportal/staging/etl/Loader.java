/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.etl;

import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.services.EmailService;
import org.cbioportal.staging.services.LoaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class Loader {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);
	
	@Autowired
	EmailService emailService;
	
	@Autowired
	LoaderService loaderService;
	
	@Value("${etl.working.dir:file:///tmp}")
	private File etlWorkingDir;
	
	@Value("${cbioportal.mode}")
	private String cbioportalMode;
	
	@Value("${cbioportal.docker.image}")
	private String cbioportalDockerImage;
	
	@Value("${cbioportal.docker.network}")
	private String cbioportalDockerNetwork;
	
	@Value("${central.share.location}")
	private File centralShareLocation;

	@Value("${portal.home}")
	private String portalHome;
	
	void load(Integer id, List<String> studies) {
		try {
			if (!centralShareLocation.exists()) {
				throw new Exception("The central share location directory specified in application.properties do not exist: "+centralShareLocation.toString()+
						". Stopping process...");
			} else {
				Map<String, String> statusStudies = new HashMap<String, String>();
				//Get studies from appropriate staging folder
				File originPath = new File(etlWorkingDir.toPath()+"/"+id+"/staging");
				for (String study : studies) {
					try {
						logger.info("Starting loading of study "+study);
						File studyPath = new File(originPath+"/"+study);
						File logFile = loaderService.load(study, studyPath);
						logger.info("Loading of study "+study+" finished.");
						statusStudies.put(logFile.getAbsolutePath(), "<b><font style=\"color: #04B404\">SUCCESSFULLY LOADED</font></b>");
					} catch (Exception e) {
						//tell about error, continue with next study
						logger.error(e.getMessage()+". The app will continue with the next study.");
						statusStudies.put(study, "<b><font style=\"color: #FF0000\">ERRORS</font></b>");
						e.printStackTrace();
					}
				}
				emailService.emailStudiesLoaded(statusStudies);
			}
		} catch (Exception e) {
			logger.error("An error not expected occurred. Stopping process...");
			try {
				emailService.emailGenericError("An error not expected occurred. Stopping process...", e);
			} catch (Exception e1) {
				logger.error("The email could not be sent due to the error specified below.");
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
	}
}
