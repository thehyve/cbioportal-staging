/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.etl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.cbioportal.staging.services.EmailService;
import org.springframework.stereotype.Component;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

@Component
public class EmailServiceMockupImpl implements EmailService {
	
	private boolean isEmailStudyErrorSent = false;
	private boolean isEmailStudyFileNotFoundSent = false;
	private boolean isEmailValidationReportSent = false;
	private boolean isEmailStudiesLoadedSent = false;
	private boolean isEmailGenericErrorSent = false;

	public void emailStudyFileNotFound(Map<String, ArrayList<String>> failedStudies) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		this.isEmailStudyFileNotFoundSent  = true;
	}
	
	public void emailStudyError(String studyId, Exception e) throws IOException, TemplateException {
		this.isEmailStudyErrorSent  = true;
	}
	
	public void emailValidationReport(Map<Pair<String,String>,Map<String, Integer>> validatedStudies, String level) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		this.isEmailValidationReportSent = true;
	}
	
	public void emailStudiesLoaded(Map<String,String> studiesLoaded) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		this.isEmailStudiesLoadedSent = true;
	}
	
	public void emailGenericError(String errorMessage, Exception e) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		this.isEmailGenericErrorSent = true;
	}

	public boolean isEmailStudyErrorSent() {
		return this.isEmailStudyErrorSent;
	}
	
	public boolean isEmailStudyFileNotFoundSent() {
		return this.isEmailStudyFileNotFoundSent;
	}
	
	public boolean isEmailValidationReportSent() {
		return this.isEmailValidationReportSent;
	}
	
	public boolean isEmailStudiesLoadedSent() {
		return this.isEmailStudiesLoadedSent;
	}
	
	public boolean isEmailGenericErrorSent() {
		return this.isEmailGenericErrorSent;
	}
	
	public void reset() {
		this.isEmailStudyErrorSent = false;
		this.isEmailStudyFileNotFoundSent = false;
		this.isEmailValidationReportSent = false;
		this.isEmailStudiesLoadedSent = false;
		this.isEmailGenericErrorSent = false;
	}
	
}
