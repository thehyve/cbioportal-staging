/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.services;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

public interface EmailService {
	

	public void emailStudyFileNotFound(Map<String, ArrayList<String>> failedStudies) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException;
	
	public void emailStudyError(String studyId, Exception e) throws IOException, TemplateException;
	
	public void emailValidationReport(Map<Pair<String,String>,Map<String, Integer>> validatedStudies, String level) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException;
	
	public void emailStudiesLoaded(Map<String,String> studiesLoaded) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException;
	
	public void emailGenericError(String errorMessage, Exception e) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException;

}
