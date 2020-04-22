package org.cbioportal.staging.services.report;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.directory.IDirectoryCreator;
import org.cbioportal.staging.services.resource.Study;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import freemarker.template.Configuration;
import freemarker.template.Template;

@Component
public class LogMessageUtils {

    private static final Logger logger = LoggerFactory.getLogger(LogMessageUtils.class);

	@Value("${study.curator.emails:}")
    private String studyCuratorEmails;

	@Value("${scan.location}")
    private String scanLocation;
    
    @Value("${central.share.location.web.address:}")
    private String centralShareLocationWebAddress;

	@Autowired
    private Configuration freemarkerConfig;
    
    @Autowired
	private IDirectoryCreator directoryCreator;

	public String messageStudyFileNotFound(String template, Map<String, List<String>> failedStudies, Integer timeRetry) throws ReporterException {
        try {
            final Integer totalTime = timeRetry*5;

            Template t = freemarkerConfig.getTemplate(template);
            Map<String, Object> messageParams = new HashMap<String, Object>();
            messageParams.put("failedStudies", failedStudies);
            messageParams.put("totalTime", totalTime.toString());
            messageParams.put("timestamp", new Date());
            return FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
		} catch(Exception me) {
			logger.error(me.getMessage(), me);
			throw new ReporterException("Error while composing message.", me);
		}
	}

	public String messageSummaryStudies(String template, Study study, String serverAlias,
        ExitStatus transformerStatus, Resource transformerLog, ExitStatus validatorStatus, Resource validatorLog, 
        Resource validatorReport, ExitStatus loaderStatus, Resource loaderLog) throws ReporterException {

            String intermediatePath = directoryCreator.getIntermediatePath(study);
            String transformerLogPath = "";
            if (transformerLog != null) {
                transformerLogPath =  centralShareLocationWebAddress + "/" + intermediatePath + "/" + transformerLog.getFilename();
            }
            String validatorLogPath = "";
            if (validatorLog != null) {
                validatorLogPath = centralShareLocationWebAddress + "/" + intermediatePath + "/" +  validatorLog.getFilename();
            }
            String validatorReportPath = "";
            if (validatorReport != null) {
                validatorReportPath = centralShareLocationWebAddress + "/" + intermediatePath + "/" + validatorReport.getFilename();
            }
            String loaderLogPath = "";
            if (loaderLog != null) {
                loaderLogPath = centralShareLocationWebAddress + "/" + intermediatePath + "/" +  loaderLog.getFilename();
            }
            String transformerOutcome = "null";
            if (transformerStatus != null) {
                transformerOutcome = transformerStatus.toString();
            }
            String validatorOutcome = "null";
            if (validatorStatus != null) {
                validatorOutcome = validatorStatus.toString();
            }
            String loaderOutcome = "null";
            if (loaderStatus != null) {
                loaderOutcome = loaderStatus.toString();
            }
            String summaryOutcome = loaderOutcome;
            if (summaryOutcome == "null") {
                summaryOutcome = validatorOutcome;
                if (summaryOutcome == "null") {
                    summaryOutcome = transformerOutcome;
                }
            }

            String studyVersion = "";
            if (study.getVersion() != null) {
                studyVersion = study.getVersion();
            }

        try {
			Template t = freemarkerConfig.getTemplate(template);
			Map<String, Object> messageParams = new HashMap<String, Object>();
            messageParams.put("studyId", study.getStudyId());
            messageParams.put("studyVersion", studyVersion);
            messageParams.put("serverAlias", serverAlias);
            messageParams.put("transformerStatus", transformerOutcome);
            messageParams.put("transformerLog", transformerLogPath);
            messageParams.put("validatorStatus", validatorOutcome);
            messageParams.put("validatorLog", validatorLogPath);
            messageParams.put("validatorReport", validatorReportPath);
            messageParams.put("loaderStatus", loaderOutcome);
            messageParams.put("loaderLog", loaderLogPath);
            messageParams.put("summaryStatus", summaryOutcome);
			return FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
		} catch(Exception me) {
			logger.error(me.getMessage(), me);
			throw new ReporterException("Error while composing message.", me);
		}
	}

	public String messageValidationReport(String template, Map<String,ExitStatus> validatedStudies, String level, Map<String,String> filesPath) throws ReporterException {
        try {

            Map<String, String> studies = validatedStudies.entrySet().stream()
				.collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString()));

			Template t = freemarkerConfig.getTemplate(template);
			Map<String, Object> messageParams = new HashMap<String, Object>();
			messageParams.put("scanLocation", scanLocation);
			messageParams.put("studies", studies);
			messageParams.put("files", filesPath);
			messageParams.put("level", level);
			messageParams.put("timestamp", new Date());
			return FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
		} catch(Exception me) {
			logger.error(me.getMessage(), me);
			throw new ReporterException("Error while composing message.", me);
		}
	}

	public String messageStudiesLoaded(String template, Map<String,ExitStatus> studiesLoaded, Map<String,String> filesPath) throws ReporterException {

        try{

            Map<String, String> studies = studiesLoaded.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().toString()));

			Template t = freemarkerConfig.getTemplate(template);
			Map<String, Object> messageParams = new HashMap<String, Object>();
			messageParams.put("studies", studies);
			messageParams.put("files", filesPath);
			messageParams.put("timestamp", new Date());
			return FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
		} catch(Exception me) {
			logger.error(me.getMessage(), me);
			throw new ReporterException("Error while composing message.", me);
		}
	}

	public String messageGenericErrorUser(String template, String errorMessage, Exception e) throws ReporterException {
        try {
            Template t = freemarkerConfig.getTemplate(template);
            Map<String, Object> messageParams = new HashMap<>();
            messageParams.put("errorMessage", errorMessage);
            messageParams.put("displayError", displayError(e));
			messageParams.put("timestamp", new Date());
            return FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
        } catch(Exception me) {
            logger.error(me.getMessage(), me);
            throw new ReporterException("Error while composing message.", me);
        }
    }

    public String messageGenericError(String template, String errorMessage, Exception e) throws ReporterException {

        try {
			Template t = freemarkerConfig.getTemplate(template);
			Map<String, Object> messageParams = new HashMap<>();
			messageParams.put("users", studyCuratorEmails);
			messageParams.put("errorMessage", errorMessage);
			messageParams.put("displayError", displayError(e));
			messageParams.put("timestamp", new Date());
			return FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
		} catch(Exception me) {
			logger.error(me.getMessage(), me);
			throw new ReporterException("Error while composing message.", me);
		}
	}

	private String displayError(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		String stackTrace = sw.toString();
		return stackTrace.replace(System.getProperty("line.separator"), "<br/>\n");
	}

}