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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.TransformerException;
import org.cbioportal.staging.services.EmailService;
import org.cbioportal.staging.services.TransformerService;
import org.cbioportal.staging.services.ValidationService;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

@Component
public class Transformer {
	private static final Logger logger = LoggerFactory.getLogger(Transformer.class);

	@Value("${etl.working.dir:java.io.tmpdir}")
	private File etlWorkingDir;

	@Value("${skip.transformation:false}")
    private boolean skipTransformation;
    
    @Value("${central.share.location}")
    private String centralShareLocation;

	@Value("${central.share.location.portal:}")
    private String centralShareLocationPortal;
	
	@Autowired
	private EmailService emailService;
	
	@Autowired
    private TransformerService transformerService;
    
    @Autowired
	private ValidationService validationService;

	boolean skipTransformation(File originPath) {
		File metaStudyFile = new File(originPath+"/meta_study.txt");
		if (skipTransformation || metaStudyFile.exists() && metaStudyFile.isFile()) {
			return true;
		}
		return false;
	}

	List<String> transform(Integer id, List<String> studies, String transformationCommand,  Map<String, String> filesPaths) throws InterruptedException, ConfigurationException, IOException, TemplateNotFoundException, MalformedTemplateNameException, ParseException, TemplateException {
		File originPath = new File(etlWorkingDir.toPath()+"/"+id);
		File destinationPath = new File(etlWorkingDir.toPath()+"/"+id+"/staging");
		if (!destinationPath.exists()) {
			destinationPath.mkdir();
        }
        Map<String, Integer> statusStudies = new HashMap<String, Integer>();
        List<String> transformedStudies = new ArrayList<String>();
		for (String study : studies) {
            File dir = new File(originPath+"/"+study);
            File finalPath = new File(destinationPath+"/"+study);
            if (centralShareLocationPortal.equals("")) {
                centralShareLocationPortal = centralShareLocation;
            }
            int transformationStatus = -1;  
            //Create transformation log file
            String logTimeStamp = new SimpleDateFormat("yyyy_MM_dd_HH.mm.ss").format(new Date());
            String logName = study+"_transformation_log_"+logTimeStamp+".log";
            File logFile = new File(etlWorkingDir+"/"+id+"/"+logName);
			try {
				if (skipTransformation(dir)) {
                    transformerService.copyStudy(dir, finalPath);
                    transformedStudies.add(study);
				} else {
					transformationStatus = transformerService.transform(dir, finalPath, logFile);
				}
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
			} finally {
                //Only copy the files if the transformation has been performed
                if (!skipTransformation(dir)) {
                    String centralShareLocationPath = centralShareLocation+"/"+id;
                    if (!centralShareLocationPath.startsWith("s3:")) {
                        File cslPath = new File(centralShareLocation+"/"+id);
                        if (centralShareLocationPath.startsWith("file:")) {
                            cslPath = new File(centralShareLocationPath.replace("file:", ""));
                        }
                        logger.info("PATH TO BE CREATED: "+cslPath.getAbsolutePath());
                        if (!cslPath.exists()) {
                            cslPath.mkdirs();
                        }
                    }
                    validationService.copyToResource(logFile, centralShareLocationPath);
                    filesPaths.put(study+" transformation log", centralShareLocationPath+"/"+logName);
                    //Add transformation status for the email loading report
                    statusStudies.put(study, transformationStatus);
                    if (transformationStatus == 0) {
                        logger.info("Transformation of study "+study+" finished successfully.");
                    } else {
                        logger.error("Transformation process of study "+study+" failed.");
                    }
                }
            }	
        }
        //Only send the email if at least one transformation has been done
        if (filesPaths.size() > 0) {
            emailService.emailTransformedStudies(statusStudies, filesPaths);
        }
        logger.info("Transformation step finished.");
        
        //Return the list of the successfully transformed studies to pass to the validator
        for (Map.Entry<String, Integer> entry : statusStudies.entrySet()) {
            if (entry.getValue().equals(0)) {
                transformedStudies.add(entry.getKey());
            }
        }
        return transformedStudies;
    }
}
