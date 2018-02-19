/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.etl;

import org.cbioportal.staging.services.ScheduledScannerService;
import org.springframework.stereotype.Component;

@Component
public class ScheduledScannerServiceMockupImpl implements ScheduledScannerService {
	
	@Override
	public void stopApp() {
	}

}
