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
package org.cbioportal.staging.services.report;

import java.util.List;
import java.util.Map;

import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.services.ExitStatus;
import org.springframework.core.io.Resource;

public interface IReportingService {

	public void reportStudyFileNotFound(Map<String, List<String>> failedStudies, Integer timeRetry) throws ReporterException;

    public void reportTransformedStudies(Map<String,ExitStatus> studiesTransformed, Map<String,Resource> filesPaths) throws ReporterException;

	public void reportValidationReport(Map<String,ExitStatus> validatedStudies, String level, Map<String,Resource> studyPaths) throws ReporterException;

	public void reportStudiesLoaded(Map<String,ExitStatus> studiesLoaded, Map<String,Resource> filesPath) throws ReporterException;

	public void reportGenericError(String errorMessage, Exception e) throws ReporterException;

}
