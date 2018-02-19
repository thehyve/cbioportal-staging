/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.services;

import java.io.IOException;

public interface RestarterService {
	
	public void restart() throws InterruptedException, IOException;

}
