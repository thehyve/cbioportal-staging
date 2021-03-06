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

import javax.annotation.PostConstruct;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.cbioportal.staging.services.resource.Study;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "log.enable", havingValue = "true")
public class LogReportingService implements IReportingService {

	@Value("${log.file}")
	private Resource logFile;

	@Value("${log.file.createnotexist:true}")
	private boolean creatNotExist;

	@Value("${log.format:text}")
    private String logFormat;
    
    @Value("${server.alias:}")
	private String serverAlias;

	@Autowired
	private LogMessageUtils messageUtils;

	@Autowired
	private ILogFileAppender appender;

	@Autowired
	private ResourceUtils resourceUtils;

	private WritableResource writableLog;

	@PostConstruct
	public void init() throws ResourceUtilsException, ReporterException, ConfigurationException {
		if (logFile == null) {
			throw new ConfigurationException("No valid log.file set in application.properties.");
		}
		if (! (logFormat.equals("text") || (logFormat.equals("html")))) {
			throw new ConfigurationException("No valid log.format set in application.properties (can be 'text' or 'html').");
		}
		if (creatNotExist && ! logFile.exists()) {
			String path = resourceUtils.getFile(logFile).getAbsolutePath();
			resourceUtils.ensureDirs(path.substring(0, path.lastIndexOf("/")));
			logFile = resourceUtils.createFileResource(logFile, "");
		}
		writableLog = resourceUtils.getWritableResource(logFile);
	}

	@Override
	public void reportStudyFileNotFound(Map<String, List<String>> failedStudies, Integer timeRetry)
			throws ReporterException {
		String template = "studyFileNotFound_log_html.ftl";
		if (logFormat.equals("text")) {
			template = "studyFileNotFound_log_txt.ftl";
		}
		appender.addToLog(
			writableLog, messageUtils.messageStudyFileNotFound(template, failedStudies, timeRetry)
		);
	}

	@Override
	public void reportSummary(Study study, Resource transformerLog, Resource validatorLog, Resource validatorReport, 
    Resource loaderLog, ExitStatus transformerStatus, ExitStatus validatorStatus, ExitStatus loaderStatus) throws ReporterException {
        String template = "summary_log_html.ftl";
		if (logFormat.equals("text")) {
			template = "summary_log_txt.ftl";
		}
		appender.addToLog(
			writableLog, messageUtils.messageSummaryStudies(template, study, serverAlias,
            transformerStatus, transformerLog, validatorStatus, validatorLog, validatorReport, 
            loaderStatus, loaderLog)
		);
    }

	@Override
	public void reportGenericError(String errorMessage, Exception e) throws ReporterException {
		String template = "genericError_log_html.ftl";
		if (logFormat.equals("text")) {
			template = "genericError_log_txt.ftl";
		}
		appender.addToLog(
			writableLog, messageUtils.messageGenericError(template, errorMessage, e)
		);
    }

}
