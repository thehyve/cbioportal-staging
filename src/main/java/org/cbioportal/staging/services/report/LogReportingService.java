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
import java.util.stream.Collectors;

import javax.annotation.PostConstruct;

import com.pivovarit.function.ThrowingFunction;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.resource.ResourceUtils;
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
	public void reportTransformedStudies(Map<String, ExitStatus> studiesTransformed, Map<String, Resource> filesPaths)
			throws ReporterException {
		String template = "transformedStudies_log_html.ftl";
		if (logFormat.equals("text")) {
			template = "transformedStudies_log_txt.ftl";
		}
		appender.addToLog(
			writableLog, messageUtils.messageTransformedStudies(template, studiesTransformed, getLogPaths(filesPaths))
		);
	}

	@Override
	public void reportValidationReport(Map<String, ExitStatus> validatedStudies, String level,
			Map<String, Resource> logPaths) throws ReporterException {
		String template = "validationReport_log_html.ftl";
		if (logFormat.equals("text")) {
			template = "validationReport_log_txt.ftl";
		}
		appender.addToLog(
			writableLog, messageUtils.messageValidationReport(template, validatedStudies, level, getLogPaths(logPaths))
		);
	}

	@Override
	public void reportStudiesLoaded(Map<String, ExitStatus> studiesLoaded, Map<String, Resource> filesPath)
			throws ReporterException {
		String template = "studiesLoaded_log_html.ftl";
		if (logFormat.equals("text")) {
			template = "studiesLoaded_log_txt.ftl";
		}
		appender.addToLog(
			writableLog, messageUtils.messageStudiesLoaded(template, studiesLoaded, getLogPaths(filesPath))
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

	// TODO make sure the log paths are correctly displayed in the reportrs
    private Map<String,String> getLogPaths(Map<String,Resource> filesPaths) {
        return filesPaths.entrySet().stream()
            .collect(Collectors
				.toMap(e -> e.getKey(), ThrowingFunction.sneaky(e ->
					resourceUtils.getURL(e.getValue()).toString())
					// resourceUtils.stripResourceTypePrefix(resourceUtils.getURL(e.getValue()).toString()))
				)
            );
    }

}
