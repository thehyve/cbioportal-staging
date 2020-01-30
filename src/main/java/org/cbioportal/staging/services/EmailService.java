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
package org.cbioportal.staging.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.cbioportal.staging.etl.Transformer.ExitStatus;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

public interface EmailService {

	public void emailStudyFileNotFound(Map<String, ArrayList<String>> failedStudies, Integer timeRetry) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException;
	
    public void emailTransformedStudies(Map<String,ExitStatus> studiesTransformed, Map<String,String> filesPaths) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException;
	
	public void emailValidationReport(Map<String,ExitStatus> validatedStudies, String level, Map<String,String> studyPaths) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException;
	
	public void emailStudiesLoaded(Map<String,ExitStatus> studiesLoaded, Map<String,String> filesPath) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException;
	
	public void emailGenericError(String errorMessage, Exception e) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException;

}
