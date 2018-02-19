/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.etl;

import java.io.IOException;

import org.cbioportal.staging.services.RestarterService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Restarter {

	@Autowired
	RestarterService restarterService;
	
	void restart() throws InterruptedException, IOException {
		restarterService.restart();
	}
}
