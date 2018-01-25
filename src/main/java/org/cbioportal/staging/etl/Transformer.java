package org.cbioportal.staging.etl;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cbioportal.staging.app.ScheduledScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
class Transformer {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);
    
    @Value("${etl.working.dir:file:///tmp}")
    private File etlWorkingDir;
    
    void transform(Map<Integer, List<String>> studiesLoaded, String transformationCommand) {
	if (studiesLoaded.keySet().size() == 1) {
	    for (Integer id : studiesLoaded.keySet()) { //We always have only one job ID
		    File originPath = new File(etlWorkingDir.toPath()+"/"+id);
		    File destinationPath = new File(etlWorkingDir.toPath()+"/"+id+"/staging");
		    if (!destinationPath.exists()) {
			destinationPath.mkdir();
		    }
		    for (String study : studiesLoaded.get(id)) {
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
				    logger.error("Error when copying the file: "+file.getAbsolutePath());
				    e.printStackTrace();
				}
			    } catch (FileNotFoundException e1) {
				logger.error("The following file was not found: "+file.getAbsolutePath());
				e1.printStackTrace();
			    }
			}
		    }
		}
	logger.info("Transformation step finished.");
	} else {
	    logger.error("The transformation script received more than one job ID at once.");
	}
	
    }

}
