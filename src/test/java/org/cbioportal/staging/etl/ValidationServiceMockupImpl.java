/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.etl;

import java.io.File;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ValidatorException;
import org.cbioportal.staging.services.ValidationService;
import org.springframework.stereotype.Component;

@Component
public class ValidationServiceMockupImpl implements ValidationService {
	
	private String testFile;
	private boolean throwError = false;
	
	@Override
	public File validate(String study, String studyPath, String reportPath)
			throws ValidatorException, ConfigurationException {
		if (throwError) {
			throw new ValidatorException("dummy test error");
		}
		File logFile = new File(testFile);
		return logFile;
	}
	
}
