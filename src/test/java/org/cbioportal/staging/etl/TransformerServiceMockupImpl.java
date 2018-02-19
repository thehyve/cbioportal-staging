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
import org.cbioportal.staging.exceptions.TransformerException;
import org.cbioportal.staging.services.LoaderService;
import org.cbioportal.staging.services.TransformerService;
import org.springframework.stereotype.Component;

@Component
public class TransformerServiceMockupImpl implements TransformerService {
	
	
	@Override
	public void transform(File originPath, File finalPath) throws TransformerException {
		
	}
	
}
