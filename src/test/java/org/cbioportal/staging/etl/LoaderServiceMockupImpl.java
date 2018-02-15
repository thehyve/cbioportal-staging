/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.etl;

import java.io.File;
import java.io.IOException;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.services.LoaderService;
import org.springframework.stereotype.Component;

@Component
public class LoaderServiceMockupImpl implements LoaderService {
	
	private String testFile;
	private boolean throwError = false;
	
	@Override
	public File load(String study, File studyPath) throws IOException, InterruptedException, ConfigurationException {
		File logFile = new File(testFile);
		return logFile; 
	}
	
}
