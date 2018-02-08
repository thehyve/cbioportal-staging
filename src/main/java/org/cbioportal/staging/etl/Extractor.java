package org.cbioportal.staging.etl;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.tuple.Pair;
import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.app.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

@Component
class Extractor {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);
	
	@Autowired
	EmailService emailService;

	@Value("${etl.working.dir:file:///tmp}")
	private File etlWorkingDir;

	@Value("${scan.location}")
	private String scanLocation;

	Pair<Integer, List<String>> run(Resource indexFile) {
		logger.info("Extract step: downloading files to " + etlWorkingDir.getAbsolutePath());
		//Parse the indexFile and download all referred files to the working directory.
		Pair<Integer, List<String>> data;
		List<String> studiesLoaded = new ArrayList<String>();
		List<String> studiesWithErrors = new ArrayList<String>();
		Map<String, ArrayList<String>> filesNotFound = new HashMap<String, ArrayList<String>>();
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
								TimeUnit.SECONDS.sleep(1); //TODO: Change to: TimeUnit.MINUTES.sleep(5); //Wait 5 minutes
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
									if (filesNotFound.get(entry.getKey()) == null) {
										ArrayList<String> newList = new ArrayList<String>();
										newList.add(file);
										filesNotFound.put(entry.getKey(), newList);
									} else {
										filesNotFound.get(entry.getKey()).add(file);
									}
								}
							} catch (InterruptedException e1) {
								// TODO Auto-generated catch block
								e1.printStackTrace();
								errors.put(entry.getKey(), errors.get(entry.getKey())+1);
							}
						}
					}
				}
			}
			for (String study : errors.keySet()) {
				if (errors.get(study) == 0) {
					studiesLoaded.add(study);
				} else {
					studiesWithErrors.add(study);
				}
			}
			if (!studiesWithErrors.isEmpty()) {
				emailService.emailStudyFileNotFound(filesNotFound);
			}
		}
		catch (ClassCastException e) {
			logger.error("The yaml file given has an incorrect format.", e);
			try {
				emailService.emailGenericError("The yaml file given has an incorrect format.", e);
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			try {
				emailService.emailGenericError("", e);
			} catch (UnsupportedEncodingException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
		}
		data = Pair.of(id, studiesLoaded);
		logger.info("Extractor step finished");
		return data;
	}
}
