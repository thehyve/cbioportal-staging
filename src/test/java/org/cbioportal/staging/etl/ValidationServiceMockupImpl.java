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

import org.cbioportal.staging.etl.Transformer.ExitStatus;
import org.cbioportal.staging.exceptions.ValidatorException;
import org.cbioportal.staging.services.ValidationService;
import org.springframework.boot.test.context.TestComponent;

@TestComponent
public class ValidationServiceMockupImpl implements ValidationService {

	private ExitStatus exitStatus;
	private boolean throwError = false;

	@Override
	public ExitStatus validate(File studyPath, File reportFile, File logFile) throws ValidatorException {
		if (throwError) {
			throw new ValidatorException("dummy test error");
		}
		return exitStatus;
	}

	public void reset() {
		this.throwError = false;
		this.exitStatus = ExitStatus.SUCCESS;
	}

}
