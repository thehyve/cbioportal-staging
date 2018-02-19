/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.etl;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.services.LoaderService;
import org.springframework.stereotype.Component;

@Component
public class LoaderServiceMockupImpl implements LoaderService {
	
	private String testFile;
	private List<String> loadedStudies = new ArrayList<String>();
	
	@Override
	public File load(String study, File studyPath) throws IOException, InterruptedException, ConfigurationException {
		File logFile = new File(testFile);
		loadedStudies.add(study);
		return logFile; 
	}
	
	public List<String> getLoadedStudies() {
		return this.loadedStudies;
	}
	
	public void reset() {
		this.loadedStudies = new ArrayList<String>();
	}
	
}
