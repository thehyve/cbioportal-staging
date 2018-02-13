package org.cbioportal.staging.app;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

public interface EmailService {
	

	public void emailStudyFileNotFound(Map<String, ArrayList<String>> failedStudies) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException;
	
	public void emailStudyError(String studyId, Exception e) throws IOException, TemplateException;
	
	public void emailValidationReport(Map<Pair<String,String>,List<Integer>> validatedStudies, String level) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException;
	
	public void emailStudiesLoaded(Map<String,String> studiesLoaded) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException;
	
	public void emailGenericError(String errorMessage, Exception e) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException;

}
