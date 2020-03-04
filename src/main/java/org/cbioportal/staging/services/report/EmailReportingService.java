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

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import javax.mail.Message;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.pivovarit.function.ThrowingFunction;

import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;
@Component
@ConditionalOnProperty(value="mail.enable", havingValue = "true")
public class EmailReportingService implements IReportingService {

	private static final Logger logger = LoggerFactory.getLogger(EmailReportingService.class);

	@Value("${mail.to}")
	private String mailTo;

	@Value("${mail.from}")
	private String mailFrom;

	@Value("${mail.smtp.user}")
	private String mailSmtpUser;

	@Value("${mail.smtp.password}")
	private String mailSmtpPassword;

	@Value("${mail.smtp.host}")
	private String mailSmtpHost;

	@Value("${mail.debug:false}")
	private String mailDebug;

	@Value("${mail.transport.protocol}")
	private String mailTransportProtocol;

	@Value("${mail.smtp.port}")
	private String mailSmtpPort;

	@Value("${mail.smtp.auth:false}")
	private String mailSmtpAuth;

	@Value("${mail.smtp.ssl.enable:true}")
	private String mailSmtpSslEnable;

	@Value("${mail.smtp.starttls.enable:true}")
	private String mailSmtpStarttlsEnable;

	@Value("${scan.location}")
	private String scanLocation;

	@Value("${study.curator.emails:}")
	private String studyCuratorEmails;

	@Value("${server.alias:}")
	private String serverAlias;

	@Value("${debug.mode:false}")
	private Boolean debugMode;

	@Value("${central.share.location:}")
    private Resource centralShareLocation;

    @Value("${central.share.location.web.address:}")
	private Resource centralShareLocationWebAddress;

	@Autowired
	private LogMessageUtils messageUtils;

	@Autowired
    private ResourceUtils utils;

	public void reportStudyFileNotFound(Map<String, List<String>> failedStudies, Integer timeRetry) throws ReporterException {

		Properties properties = getProperties();
		Session session = getSession(properties);

		String finalFailedStudies = String.join(", ", failedStudies.keySet());

		Message msg = new MimeMessage(session);
		try {
			msg.setSubject("ERROR - cBioPortal staging app: transformation step failed. Server: "+serverAlias+". Failed studies: "+finalFailedStudies);
			if (debugMode) {
				for (String studyCuratorEmail : studyCuratorEmails.split(",")) {
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(studyCuratorEmail, false));
				}
			} else {
				for (String mailToEmail : mailTo.split(",")) {
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(mailToEmail, false));
				}
				for (String studyCuratorEmail : studyCuratorEmails.split(",")) {
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(studyCuratorEmail, false));
				}
			}
			String message = messageUtils.messageStudyFileNotFound("studyFileNotFound_email.ftl", failedStudies, timeRetry);
			msg.setFrom(new InternetAddress(mailFrom, "cBioPortal staging app"));
			msg.setContent(message, "text/html; charset=utf-8");
			Transport.send(msg);
		} catch(Exception me) {
			logger.error(me.getMessage(), me);
			throw new ReporterException("Error while sending email.", me);
		}

	}

	public void reportTransformedStudies(Map<String,ExitStatus> transformedStudies, Map<String,Resource> filesPaths) throws ReporterException {
		try {

            Map<String, String> logPaths = getLogPaths(filesPaths);

			Properties properties = getProperties();
			Session session = getSession(properties);

			Message msg = new MimeMessage(session);
			msg.setSubject("INFO - cBioPortal staging app: validation results for transformed studies. Server: "+serverAlias+". Studies: "+transformedStudies.keySet());
			if (debugMode) {
				for (String studyCuratorEmail : studyCuratorEmails.split(",")) {
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(studyCuratorEmail, false));
				}
			} else {
				for (String mailToEmail : mailTo.split(",")) {
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(mailToEmail, false));
				}
				for (String studyCuratorEmail : studyCuratorEmails.split(",")) {
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(studyCuratorEmail, false));
				}
			}
			String message = messageUtils.messageTransformedStudies("transformedStudies_email.ftl", transformedStudies, logPaths);
			msg.setContent(message, "text/html; charset=utf-8");
			msg.setSentDate(new Date());
			Transport.send(msg);
		} catch(Exception me) {
			logger.error(me.getMessage(), me);
			throw new ReporterException("Error while sending email.", me);
		}
	}

	public void reportValidationReport(Map<String,ExitStatus> validatedStudies, String level, Map<String,Resource> filesPaths) throws ReporterException {
		try {

			Map<String, String> logPaths = getLogPaths(filesPaths);

			Properties properties = getProperties();
			Session session = getSession(properties);

			Message msg = new MimeMessage(session);
			msg.setSubject("INFO - cBioPortal staging app: validation results for new studies. Server: "+serverAlias+". Studies: "+validatedStudies.keySet());
			if (debugMode) {
				for (String studyCuratorEmail : studyCuratorEmails.split(",")) {
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(studyCuratorEmail, false));
				}
			} else {
				for (String mailToEmail : mailTo.split(",")) {
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(mailToEmail, false));
				}
				for (String studyCuratorEmail : studyCuratorEmails.split(",")) {
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(studyCuratorEmail, false));
				}
			}
			msg.setFrom(new InternetAddress(mailFrom, "cBioPortal staging app"));
			String message = messageUtils.messageValidationReport("validationReport_email.ftl", validatedStudies, level, logPaths);
			msg.setContent(message, "text/html; charset=utf-8");
			msg.setSentDate(new Date());
			Transport.send(msg);
		} catch(Exception me) {
			logger.error(me.getMessage(), me);
			throw new ReporterException("Error while sending email.", me);
		}
	}

	public void reportStudiesLoaded(Map<String,ExitStatus> studiesLoaded, Map<String,Resource> filesPaths) throws ReporterException {
		try {

			Map<String, String> logPaths = getLogPaths(filesPaths);

			Properties properties = getProperties();
			Session session = getSession(properties);

			Map<String, String> studies = new HashMap<String, String>();
			String status = "SUCCESS";
			for (String study : studiesLoaded.keySet()) {
				if (studiesLoaded.get(study) == ExitStatus.SUCCESS) {
					studies.put(study, "VALID");
				} else {
					studies.put(study, "ERRORS");
					status = "ERROR";
				}
			}

			Message msg = new MimeMessage(session);
			msg.setSubject(status+" - cBioPortal study loading report. Server: "+serverAlias+". Studies: "+studies);
			if (debugMode) {
				for (String studyCuratorEmail : studyCuratorEmails.split(",")) {
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(studyCuratorEmail, false));
				}
			} else {
				for (String mailToEmail : mailTo.split(",")) {
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(mailToEmail, false));
				}
				for (String studyCuratorEmail : studyCuratorEmails.split(",")) {
					msg.addRecipient(Message.RecipientType.TO, new InternetAddress(studyCuratorEmail, false));
				}
			}
			String message = messageUtils.messageStudiesLoaded("studiesLoaded_email.ftl", studiesLoaded, logPaths);
			msg.setContent(message, "text/html; charset=utf-8");
			msg.setSentDate(new Date());
			Transport.send(msg);
		} catch(Exception me) {
			logger.error(me.getMessage(), me);
			throw new ReporterException("Error while sending email.", me);
		}
	}

	public void reportGenericError(String errorMessage, Exception e) throws ReporterException {

		try {

			Properties properties = getProperties();
			Session session = getSession(properties);

			// Send generic error email to the curator
			if (!debugMode) { //Only if we are not in debug mode
				Message msg = new MimeMessage(session);
					msg.setSubject("ERROR - cBioPortal staging app: unexpected error. Server: "+serverAlias);
					for (String studyCuratorEmail : studyCuratorEmails.split(",")) {
						msg.addRecipient(Message.RecipientType.TO, new InternetAddress(studyCuratorEmail, false));
					}
					msg.setFrom(new InternetAddress(mailFrom, "cBioPortal staging app"));
					String message = messageUtils.messageGenericErrorUser("genericErrorUser_email.ftl", errorMessage, e);
					msg.setContent(message, "text/html; charset=utf-8");
					msg.setSentDate(new Date());
					Transport.send(msg);
			}

			// Send email with details of the error to the staging app admins
			// In debug mode, send this email to the curator
			Message msg2 = new MimeMessage(session);
			msg2.setSubject("ERROR - cBioPortal staging app: unexpected error. Server: "+serverAlias);
			if (debugMode) {
				for (String studyCuratorEmail : studyCuratorEmails.split(",")) {
					msg2.addRecipient(Message.RecipientType.TO, new InternetAddress(studyCuratorEmail, false));
				}
			} else {
				for (String mailToEmail : mailTo.split(",")) {
					msg2.addRecipient(Message.RecipientType.TO, new InternetAddress(mailToEmail, false));
				}
			}
			msg2.setFrom(new InternetAddress(mailFrom, "cBioPortal staging app"));
			String message = messageUtils.messageGenericError("genericError_email.ftl", errorMessage, e);
			msg2.setContent(message, "text/html; charset=utf-8");
			msg2.setSentDate(new Date());
			Transport.send(msg2);
		} catch(Exception me) {
			logger.error(me.getMessage(), me);
			throw new ReporterException("Error while sending email.", me);
		}
	}

    private Map<String,String> getLogPaths(Map<String,Resource> filesPaths) throws ResourceCollectionException, IOException {
		Map<String, String> logPaths = new HashMap<String, String>();
        if (centralShareLocation != null && centralShareLocationWebAddress != null) {
			logPaths = replaceLogPaths(filesPaths);
        } else {
			logPaths = filesPaths.entrySet().stream()
            .collect(Collectors
                .toMap(e -> e.getKey(), ThrowingFunction.sneaky(e -> e.getValue().getURL().toString())
                )
            );
        }
        return logPaths;
    }

	private Map<String,String> replaceLogPaths(Map<String,Resource> filesPaths) {
		return filesPaths.entrySet().stream()
			.collect(
				Collectors.toMap(
					e -> e.getKey(),
					ThrowingFunction.sneaky( e -> {
                        String cslUrl = centralShareLocation.getURL().toString();
                        String cslWebUrl = centralShareLocationWebAddress.getURL().toString();
                        String logUrl = e.getValue().getURL().toString();
                        return logUrl.replaceFirst(cslUrl,cslWebUrl);
					})
				)
			);
	}

	private Properties getProperties() {
		// Get system properties
		Properties properties = System.getProperties();

		// Setup mail server
		properties.setProperty("mail.smtp.host", mailSmtpHost);
		properties.setProperty("mail.debug", mailDebug);
		properties.setProperty("mail.transport.protocol", mailTransportProtocol);
		properties.setProperty("mail.smtp.port", mailSmtpPort);
		properties.setProperty("mail.smtp.auth", mailSmtpAuth);
		properties.setProperty("mail.smtp.ssl.enable", mailSmtpSslEnable);
		properties.setProperty("mail.smtp.starttls.enable", mailSmtpStarttlsEnable);

		return properties;
	}

	private Session getSession(Properties properties) {
		return Session.getInstance(properties,
		new javax.mail.Authenticator() {
			protected PasswordAuthentication getPasswordAuthentication() {
				return new PasswordAuthentication(mailSmtpUser, mailSmtpPassword);
			}
		});
	}

}
