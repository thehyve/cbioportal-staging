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
	public String load(String study, File studyPath, int id, String centralShareLocation) throws IOException, InterruptedException, ConfigurationException {
		File logFile = new File(testFile);
		loadedStudies.add(study);
		return logFile.toString(); 
	}
	
	public List<String> getLoadedStudies() {
		return this.loadedStudies;
	}
	
	public void reset() {
		this.loadedStudies = new ArrayList<String>();
	}
	
}
