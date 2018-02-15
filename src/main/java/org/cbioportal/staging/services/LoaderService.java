/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.services;

import java.io.File;
import java.io.IOException;

import org.cbioportal.staging.exceptions.ConfigurationException;

public interface LoaderService {
	
	public File load(String study, File studyPath) throws IOException, InterruptedException, ConfigurationException;

}
