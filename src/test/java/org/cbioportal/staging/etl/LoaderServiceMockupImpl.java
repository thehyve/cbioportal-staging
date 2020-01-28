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

import org.cbioportal.staging.exceptions.LoaderException;
import org.cbioportal.staging.services.LoaderService;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class LoaderServiceMockupImpl implements LoaderService {

	private int exitStatus;
	private boolean throwError = false;

	@Override
	public int load(String study, File studyPath, File logFile) throws LoaderException {
        if (throwError) {
			throw new LoaderException("dummy test error");
		}
		return exitStatus;
	}

	public void reset() {
		this.throwError = false;
		this.exitStatus = 0;
	}

}
