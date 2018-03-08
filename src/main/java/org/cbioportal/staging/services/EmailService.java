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

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

public interface EmailService {
	

	public void emailStudyFileNotFound(Map<String, ArrayList<String>> failedStudies, Integer timeAttempt) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException;
	
	public void emailStudyError(String studyId, Exception e) throws IOException, TemplateException;
	
	public void emailValidationReport(Map<String,Integer> validatedStudies, String level, String csl_path) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException;
	
	public void emailStudiesLoaded(Map<String,String> studiesLoaded, String csl_path) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException;
	
	public void emailGenericError(String errorMessage, Exception e) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException;

}
