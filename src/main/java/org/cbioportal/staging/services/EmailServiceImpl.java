/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.services;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.apache.commons.lang3.tuple.Pair;
import org.cbioportal.staging.app.ScheduledScanner;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.ui.freemarker.FreeMarkerTemplateUtils;

import freemarker.core.ParseException;
import freemarker.template.Configuration;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

@Component
public class EmailServiceImpl implements EmailService {
	
	@Autowired
    private Configuration freemarkerConfig;
	
	@Value("${mail.admin.user}")
	private String mailAdminUser;
	
	@Value("${mail.app.user}")
	private String mailAppUser;
	
	@Value("${mail.app.password}")
	private String mailAppPassword;
	
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
	
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);

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
						return new PasswordAuthentication(mailAppUser, mailAppPassword);
					}
				  });
	}
	
	public void emailStudyFileNotFound(Map<String, ArrayList<String>> failedStudies) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		StringBuilder allFailedStudies = new StringBuilder();
		for (String failedStudy : failedStudies.keySet()) {
			allFailedStudies.append("STUDY: "+failedStudy+"<br>");
			for (String failedFile : failedStudies.get(failedStudy)) {
				allFailedStudies.append("- "+failedFile+"<br>");
			}
		}
		final String finalFailedStudies = allFailedStudies.toString();

		Properties properties = getProperties();
		Session session = getSession(properties);
		
		Message msg = new MimeMessage(session);
		try {
		    msg.setSubject("ERROR - cBioPortal staging app: transformation step failed");
		    msg.setRecipient(Message.RecipientType.TO, new InternetAddress(mailAdminUser, false));
		    Template t = freemarkerConfig.getTemplate("studyFileNotFound.ftl");
		    String message = FreeMarkerTemplateUtils.processTemplateIntoString(t, new HashMap<String, String>() {{
		        put("finalFailedStudies",finalFailedStudies);
		    }});
		    msg.setFrom(new InternetAddress("noreply@cbioportal.org", "cBioPortal staging app"));
		    msg.setContent(message, "text/html; charset=utf-8");
		    msg.setSentDate(new Date());
		    Transport.send(msg);
		} catch(MessagingException me) {
		    logger.info(me.getMessage());
		}
	}
	
	private String displayError(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		String stackTrace = sw.toString();
		return stackTrace.replace(System.getProperty("line.separator"), "<br/>\n");
	}
	
	public void emailStudyError(String studyId, Exception e) throws IOException, TemplateException {
		logger.info("Sending email regarding transformation error" );
		Properties properties = getProperties();
		Session session = getSession(properties);
		
		Message msg = new MimeMessage(session);
		try {
		    msg.setSubject("ERROR - cBioPortal staging app: transformation step failed for study "+studyId);
		    msg.setRecipient(Message.RecipientType.TO, new InternetAddress(mailAdminUser, false));
		    msg.setFrom(new InternetAddress("noreply@cbioportal.org", "cBioPortal staging app"));
		    Template t = freemarkerConfig.getTemplate("studyError.ftl");
		    Map<String, String> messageParams = new HashMap<String, String>();
		    messageParams.put("studyId", studyId);
		    messageParams.put("displayError", displayError(e));
		    String message = FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
		    msg.setContent(message, "text/html; charset=utf-8");
		    msg.setSentDate(new Date());
		    Transport.send(msg);
		} catch(MessagingException me) {
		    logger.error(me.getMessage());
		}
	}
	
	public void emailValidationReport(Map<Pair<String,String>,Map<String, Integer>> validatedStudies, String level) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		Properties properties = getProperties();
		Session session = getSession(properties);
		
		String studiesToLoad = new String();
		if (level.equals("Errors")) {
			studiesToLoad = "<b><font style=\"color: #04B404\">VALID</font></b> and <b><font style=\"color: #FFBF00\">VALID with WARNINGS</font></b>";
		} else if (level.equals("Warnings")) {
			studiesToLoad = "<b><font style=\"color: #04B404\">VALID</font></b>";
		} else {
			logger.error("The level should be 'Errors' or 'Warnings'");
		}
		
		String studies = new String();
		for (Pair<String, String> study : validatedStudies.keySet()) {
			if (validatedStudies.get(study).get("ERROR").equals(0)) {
				if (validatedStudies.get(study).get("WARNING").equals(0)) { //Study with no warnings and no errors
					studies = studies + "<br>- "+study.getLeft()+", "+study.getRight()+", status: "+"<b><font style=\"color: #04B404\">VALID</font></b>";
				}
				else { //Study with warnings and no errors
					studies = studies + "<br>- "+study.getLeft()+", "+study.getRight()+", status: "+"<b><font style=\"color: #FFBF00\">VALID with WARNINGS</font></b>";
				}
			} else { //Study with errors
				studies = studies + "<br>- "+study.getLeft()+", "+study.getRight()+", status: "+"<b><font style=\"color: #FF0000\">ERRORS</font></b>";
			}
		}
		
		Message msg = new MimeMessage(session);
		try {
		    msg.setSubject("INFO - cBioPortal staging app: validation results for new studies");
		    msg.setRecipient(Message.RecipientType.TO, new InternetAddress(mailAdminUser, false));
		    msg.setFrom(new InternetAddress("noreply@cbioportal.org", "cBioPortal staging app"));
		    Template t = freemarkerConfig.getTemplate("validationReport.ftl");
		    Map<String, String> messageParams = new HashMap<String, String>();
		    messageParams.put("studies", studies);
		    messageParams.put("studiesToLoad", studiesToLoad);
		    String message = FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
		    msg.setContent(message, "text/html; charset=utf-8");
		    msg.setSentDate(new Date());
		    Transport.send(msg);
		} catch(MessagingException me) {
		    logger.info(me.getMessage());
		}
	}
	
	public void emailStudiesLoaded(Map<String,String> studiesLoaded) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		Properties properties = getProperties();
		Session session = getSession(properties);
		
		String studies = new String();
		for (String study : studiesLoaded.keySet()) {
			studies = studies + "<br> - " + study + ", status: " + studiesLoaded.get(study);
		}
		
		Message msg = new MimeMessage(session);
		try {
		    msg.setSubject("INFO - cBioPortal staging app: data loading results for new studies");
		    msg.setRecipient(Message.RecipientType.TO, new InternetAddress(mailAdminUser, false));
		    msg.setFrom(new InternetAddress("noreply@cbioportal.org", "cBioPortal staging app"));
		    Template t = freemarkerConfig.getTemplate("studiesLoaded.ftl");
		    Map<String, String> messageParams = new HashMap<String, String>();
		    messageParams.put("studies", studies);
		    String message = FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
		    msg.setContent(message, "text/html; charset=utf-8");
		    msg.setSentDate(new Date());
		    Transport.send(msg);
		} catch(MessagingException me) {
		    logger.info(me.getMessage());
		}
	}
	
	public void emailGenericError(String errorMessage, Exception e) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		Properties properties = getProperties();
		Session session = getSession(properties);
		
		Message msg = new MimeMessage(session);
		try {
		    msg.setSubject("ERROR - cBioPortal staging app: unexpected error");
		    msg.setRecipient(Message.RecipientType.TO, new InternetAddress(mailAdminUser, false));
		    msg.setFrom(new InternetAddress("noreply@cbioportal.org", "cBioPortal staging app"));
		    Template t = freemarkerConfig.getTemplate("genericError.ftl");
		    Map<String, String> messageParams = new HashMap<String, String>();
		    messageParams.put("errorMessage", errorMessage);
		    messageParams.put("displayError", displayError(e));
		    String message = FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
		    msg.setContent(message, "text/html; charset=utf-8");
		    msg.setSentDate(new Date());
		    Transport.send(msg);
		} catch(MessagingException me) {
		    logger.info(me.getMessage());
		}
	}

}
