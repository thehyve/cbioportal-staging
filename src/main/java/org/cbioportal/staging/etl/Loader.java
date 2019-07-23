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

import org.cbioportal.staging.exceptions.LoaderException;
import org.cbioportal.staging.services.EmailService;
import org.cbioportal.staging.services.LoaderService;
import org.cbioportal.staging.services.ValidationService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.text.SimpleDateFormat;
import java.util.Date;

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
    
    @Autowired
	private ValidationService validationService;
	
	@Value("${etl.working.dir:${java.io.tmpdir}}")
	private File etlWorkingDir;

	@Value("${central.share.location}")
	private String centralShareLocation;
	
	@Value("${central.share.location.portal:}")
    private String centralShareLocationPortal;
    
	
	boolean load(Integer id, Map<String, File> studyPaths, Map<String, String> filesPath) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException, RuntimeException, LoaderException {
        Map<String, String> statusStudies = new HashMap<String, String>();
        int studiesNotLoaded = 0;
		if (centralShareLocationPortal.equals("")) {
			centralShareLocationPortal = centralShareLocation;
		}
		for (String study : studyPaths.keySet()) {
            logger.info("Starting loading of study "+study+". This can take some minutes.");
            File studyPath = new File(studyPaths.get(study)+"/staging");
            int loadingStatus = -1;                 
            //Create loading log file
            String logTimeStamp = new SimpleDateFormat("yyyy_MM_dd_HH.mm.ss").format(new Date());
            String logName = study+"_loading_log_"+logTimeStamp+".log";
            File logFile = new File(studyPaths.get(study)+"/"+logName);
			try {
                loadingStatus = loaderService.load(study, studyPath, logFile);
            } catch (RuntimeException e) {
				throw new RuntimeException(e);
			} catch (Exception e) {
				//tell about error, continue with next study
				logger.error(e.getMessage()+". The app will skip this study.");
				e.printStackTrace();
			} finally {
                validationService.copyToResource(logFile, centralShareLocation+"/"+id);
                filesPath.put(study+" loading log", centralShareLocationPortal+"/"+id+"/"+logName);	

                //Add loading result for the email loading report
                if (loadingStatus == 0) {
                    statusStudies.put(study, "SUCCESSFULLY LOADED");
                    logger.info("Loading of study "+study+" finished successfully.");
                } else {
                    statusStudies.put(study, "ERRORS");
                    studiesNotLoaded += 1;
                    logger.error("Loading process of study "+study+" failed.");
                }
			}
        }
        
        emailService.emailStudiesLoaded(statusStudies, filesPath);
        //Return false if no studies have been loaded
        if (studyPaths.keySet().size() == studiesNotLoaded) {
            return false;
        } 
		return true;
	}
}
