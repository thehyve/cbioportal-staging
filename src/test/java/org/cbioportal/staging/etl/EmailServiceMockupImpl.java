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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import org.cbioportal.staging.etl.Transformer.ExitStatus;
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
    private boolean isEmailTransformedStudiesSent = false;
	private boolean isEmailValidationReportSent = false;
	private boolean isEmailStudiesLoadedSent = false;
	private boolean isEmailGenericErrorSent = false;

	public void emailStudyFileNotFound(Map<String, ArrayList<String>> failedStudies, Integer timeRetry) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		this.isEmailStudyFileNotFoundSent  = true;
	}
    
    public void emailTransformedStudies(Map<String,ExitStatus> studiesTransformed, Map<String,String> logPaths) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
        this.isEmailTransformedStudiesSent  = true;
    }
	
	public void emailValidationReport(Map<String,ExitStatus> validatedStudies, String level, Map<String,String> studyPaths) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		this.isEmailValidationReportSent = true;
	}
	
	public void emailStudiesLoaded(Map<String,String> studiesLoaded, Map<String,String> studyPaths) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
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
    
    public boolean isEmailTransformedStudiesSent() {
		return this.isEmailTransformedStudiesSent;
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
        this.isEmailTransformedStudiesSent = false;
		this.isEmailValidationReportSent = false;
		this.isEmailStudiesLoadedSent = false;
		this.isEmailGenericErrorSent = false;
	}
	
}
