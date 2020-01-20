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

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ValidatorException;
import org.cbioportal.staging.services.ValidationService;
import org.springframework.stereotype.Component;

@Component
public class ValidationServiceMockupImpl implements ValidationService {
	
	private int exitStatus;
	private boolean throwError = false;
	
	@Override
	public int validate(String study, String studyPath, String reportPath, File logFile, String date) throws ValidatorException, ConfigurationException {
		if (throwError) {
			throw new ValidatorException("dummy test error");
		}
		return exitStatus;
	}

	public void reset() {
		this.throwError = false;
		this.exitStatus = 0;
	}
	
	public void copyToResource(File reportFile, String centralShareLocation) throws IOException {
    }
    
    public String getCentralShareLocationPath(String centralShareLocation, String date) {
        return centralShareLocation+"/"+date;
    }
}
