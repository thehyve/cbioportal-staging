package org.cbioportal.staging.etl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.cbioportal.staging.app.ScheduledScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Component
class Extractor {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);

    @Value("${etl.working.dir:file:///tmp}")
    private File etlWorkingDir;
    
    @Value("${scan.location}")
    private String scanLocation;

    Map<Integer, List<String>> run(Resource indexFile) {
	logger.info("Extract step: downloading files to " + etlWorkingDir.getAbsolutePath());
	//Parse the indexFile and download all referred files to the working directory.
	Map<Integer, List<String>> returnMap = new HashMap<Integer, List<String>>();
	List<String> studiesLoaded = new ArrayList<String>();
	Yaml yaml = new Yaml();
	Map<String, Integer> errors = new HashMap<String, Integer>();
	Integer id = 0;
	    File idPath = new File(etlWorkingDir.toPath()+"/"+id);
	    while (idPath.exists()) {
		id ++;
		idPath = new File(etlWorkingDir.toPath()+"/"+id);
	    }
	    if (!idPath.exists()) {
		idPath.mkdirs();
	    }
	try {
	    InputStream ios = indexFile.getInputStream();
	    @SuppressWarnings("unchecked")
	    Map<String, List<String>> result = (Map<String, List<String>>) yaml.load(ios);
	    for (Entry<String, List<String>>entry : result.entrySet()) {
		File destinationPath = new File(idPath+"/"+entry.getKey());
		if (!destinationPath.exists()) {
			destinationPath.mkdirs();
		    }
		errors.put(entry.getKey(), 0); //Add error counter for the current study
		for (String file : entry.getValue() ) {
		    logger.info("Copying file "+file+" from study "+entry.getKey());
		    String originPath = scanLocation+"/"+entry.getKey();
		    InputStream is = this.getClass().getClassLoader().getResourceAsStream(originPath+"/"+file);
		    try {
			Files.copy(is, Paths.get(destinationPath+"/"+file));
			logger.info("File "+file+" has been copied successfully!");
		    }
		    catch (NullPointerException e) {
			Integer attempt = 1;
			logger.error("The file "+file+" from the study "+entry.getKey()+" does not exist in "+originPath+"/"+file+". Tried "+attempt+" times.");
			attempt ++;
			while (attempt <= 5) {
			    try {
				TimeUnit.MINUTES.sleep(5); //Wait 5 minutes
				try {
				    Files.copy(is, Paths.get(destinationPath+"/"+file));
				    logger.info("File "+file+" has been copied successfully!");
				}
				catch (NullPointerException f) {
				    logger.error("The file "+file+" from the study "+entry.getKey()+" does not exist in "+originPath+"/"+file+". Tried "+attempt+" times.");
				}
				attempt ++;
			if (attempt == 5) {
			    errors.put(entry.getKey(), errors.get(entry.getKey())+1);
			    //TODO: SEND EMAIL
			}
			    } catch (InterruptedException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
				errors.put(entry.getKey(), errors.get(entry.getKey())+1);
				//TODO: SEND EMAIL
			    }
			}
		    }
		}
	    }
	    for (String study : errors.keySet()) {
		if (errors.get(study) == 0) {
		    studiesLoaded.add(study);
		}
	    }
	}
	catch (ClassCastException e) {
		logger.error("The yaml file given has an incorrect format.", e);
		e.printStackTrace();
	}
	catch (IOException e) {
	    // TODO Auto-generated catch block
	    e.printStackTrace();
	}
	returnMap.put(id, studiesLoaded);
	logger.info("Extractor step finished");
	return returnMap;
    }
}
