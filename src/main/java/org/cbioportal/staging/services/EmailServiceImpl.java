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
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.PasswordAuthentication;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
	
	@Value("${study.curator.emails}")
	private String studyCuratorEmails;
	
	@Value("${server.alias}")
	private String serverAlias;
	
	@Value("${debug.mode:false}")
	private Boolean debugMode;
	
	private static final Logger logger = LoggerFactory.getLogger(EmailService.class);

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
	
	public void emailStudyFileNotFound(Map<String, ArrayList<String>> failedStudies, Integer timeRetry) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		StringBuilder allFailedStudies = new StringBuilder();
		for (String failedStudy : failedStudies.keySet()) {
			allFailedStudies.append("STUDY: "+failedStudy+"<br>");
			for (String failedFile : failedStudies.get(failedStudy)) {
				allFailedStudies.append("- "+failedFile+"<br>");
			}
		}
		final String finalFailedStudies = allFailedStudies.toString();
		final Integer totalTime = timeRetry*5;

		Properties properties = getProperties();
		Session session = getSession(properties);
		
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
        Template t = freemarkerConfig.getTemplate("studyFileNotFound.ftl");
        Map<String, String> messageParams = new HashMap<String, String>();
        messageParams.put("finalFailedStudies", finalFailedStudies);
        messageParams.put("totalTime", totalTime.toString());
        String message = FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
        msg.setFrom(new InternetAddress(mailFrom, "cBioPortal staging app"));
        msg.setContent(message, "text/html; charset=utf-8");
        msg.setSentDate(new Date());
        Transport.send(msg);
        } catch(MessagingException me) {
            logger.error(me.getMessage(), me);
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
            msg.setSubject("ERROR - cBioPortal staging app: transformation step failed for study "+studyId+". Server: "+serverAlias);
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
            Template t = freemarkerConfig.getTemplate("studyError.ftl");
            Map<String, String> messageParams = new HashMap<String, String>();
            messageParams.put("studyId", studyId);
            messageParams.put("displayError", displayError(e));
            String message = FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
            msg.setContent(message, "text/html; charset=utf-8");
            msg.setSentDate(new Date());
            Transport.send(msg);
        } catch(MessagingException me) {
            logger.error(me.getMessage(), me);
        }
    }
    
    public void emailTransformedStudies(Map<String,Integer> transformedStudies, Map<String,String> filesPaths) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		Properties properties = getProperties();
		Session session = getSession(properties);
		
		Map<String, String> studies = new HashMap<String, String>();
		for (String study : transformedStudies.keySet()) {
			if (transformedStudies.get(study) == 0) {
				studies.put(study, "VALID"); 
			}
			else if (transformedStudies.get(study) == 3) { //Study with warnings and no errors
				studies.put(study, "VALID with WARNINGS");
			} else { //Study with errors
				studies.put(study, "ERRORS");
			}
		}
		
		Message msg = new MimeMessage(session);
		try {
		    msg.setSubject("INFO - cBioPortal staging app: validation results for transformed studies. Server: "+serverAlias+". Studies: "+studies.keySet());
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
		    Template t = freemarkerConfig.getTemplate("transformedStudies.ftl");
		    Map<String, Object> messageParams = new HashMap<String, Object>();
		    messageParams.put("studies", studies);
		    messageParams.put("files", filesPaths);
		    String message = FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
		    msg.setContent(message, "text/html; charset=utf-8");
		    msg.setSentDate(new Date());
		    Transport.send(msg);
		} catch(MessagingException me) {
		    logger.error(me.getMessage(), me);
		}
	}
	
	public void emailValidationReport(Map<String,Integer> validatedStudies, String level, Map<String,String> filesPath) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		Properties properties = getProperties();
		Session session = getSession(properties);
		
		Map<String, String> studies = new HashMap<String, String>();
		for (String study : validatedStudies.keySet()) {
			if (validatedStudies.get(study) == 0) {
				studies.put(study, "VALID"); 
			}
			else if (validatedStudies.get(study) == 3) { //Study with warnings and no errors
				studies.put(study, "VALID with WARNINGS");
			} else { //Study with errors
				studies.put(study, "ERRORS");
			}
		}
		
		Message msg = new MimeMessage(session);
		try {
		    msg.setSubject("INFO - cBioPortal staging app: validation results for new studies. Server: "+serverAlias+". Studies: "+studies.keySet());
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
		    Template t = freemarkerConfig.getTemplate("validationReport.ftl");
		    Map<String, Object> messageParams = new HashMap<String, Object>();
		    messageParams.put("scanLocation", scanLocation);
		    messageParams.put("studies", studies);
		    messageParams.put("files", filesPath);
		    messageParams.put("level", level);
		    String message = FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
		    msg.setContent(message, "text/html; charset=utf-8");
		    msg.setSentDate(new Date());
		    Transport.send(msg);
		} catch(MessagingException me) {
		    logger.error(me.getMessage(), me);
		}
	}
	
	public void emailStudiesLoaded(Map<String,String> studiesLoaded, Map<String,String> filesPath) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		Properties properties = getProperties();
		Session session = getSession(properties);
		Set<String> studies = studiesLoaded.keySet();
		String status = "SUCCESS";
		for (String study : studies) {
			if (studiesLoaded.get(study).equals("ERRORS")) {
				status = "ERROR";
			}
		}
		
		Message msg = new MimeMessage(session);
		try {
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
		    msg.setFrom(new InternetAddress(mailFrom, "cBioPortal staging app"));
		    Template t = freemarkerConfig.getTemplate("studiesLoaded.ftl");
		    Map<String, Object> messageParams = new HashMap<String, Object>();
		    messageParams.put("studies", studiesLoaded);
		    messageParams.put("files", filesPath);
		    String message = FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
		    msg.setContent(message, "text/html; charset=utf-8");
		    msg.setSentDate(new Date());
		    Transport.send(msg);
		} catch(MessagingException me) {
		    logger.error(me.getMessage(), me);
		}
	}
	
	public void emailGenericError(String errorMessage, Exception e) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		Properties properties = getProperties();
		Session session = getSession(properties);
        
        // Send generic error email to the curator
        if (!debugMode) { //Only if we are not in debug mode
            Message msg = new MimeMessage(session);
            try {
                msg.setSubject("ERROR - cBioPortal staging app: unexpected error. Server: "+serverAlias);
                for (String studyCuratorEmail : studyCuratorEmails.split(",")) {
                    msg.addRecipient(Message.RecipientType.TO, new InternetAddress(studyCuratorEmail, false));
                }
                msg.setFrom(new InternetAddress(mailFrom, "cBioPortal staging app"));
                Template t = freemarkerConfig.getTemplate("genericErrorUser.ftl");
                Map<String, String> messageParams = new HashMap<String, String>();
                messageParams.put("errorMessage", errorMessage);
                messageParams.put("displayError", displayError(e));
                String message = FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
                msg.setContent(message, "text/html; charset=utf-8");
                msg.setSentDate(new Date());
                Transport.send(msg);
            } catch(MessagingException me) {
                logger.error(me.getMessage(), me);
            }
        }
        
        // Send email with details of the error to the staging app admins
        // In debug mode, send this email to the curator
        Message msg2 = new MimeMessage(session);
		try {
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
		    Template t = freemarkerConfig.getTemplate("genericError.ftl");
            Map<String, String> messageParams = new HashMap<String, String>();
            messageParams.put("users", studyCuratorEmails);
		    messageParams.put("errorMessage", errorMessage);
		    messageParams.put("displayError", displayError(e));
		    String message = FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
		    msg2.setContent(message, "text/html; charset=utf-8");
		    msg2.setSentDate(new Date());
		    Transport.send(msg2);
		} catch(MessagingException me) {
		    logger.error(me.getMessage(), me);
		}
    }
    
    public void emailGenericError(String errorMessage, Set<String> studies, Exception e) throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		Properties properties = getProperties();
		Session session = getSession(properties);
        
        // Send generic error email to the curator
        if (!debugMode) { //Only if we are not in debug mode
            Message msg = new MimeMessage(session);
            try {
                msg.setSubject("ERROR - cBioPortal staging app: unexpected error. Server: "+serverAlias+". Studies: "+studies);
                for (String studyCuratorEmail : studyCuratorEmails.split(",")) {
                    msg.addRecipient(Message.RecipientType.TO, new InternetAddress(studyCuratorEmail, false));
                }
                msg.setFrom(new InternetAddress(mailFrom, "cBioPortal staging app"));
                Template t = freemarkerConfig.getTemplate("genericErrorUser.ftl");
                Map<String, String> messageParams = new HashMap<String, String>();
                messageParams.put("errorMessage", errorMessage);
                messageParams.put("displayError", displayError(e));
                String message = FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
                msg.setContent(message, "text/html; charset=utf-8");
                msg.setSentDate(new Date());
                Transport.send(msg);
            } catch(MessagingException me) {
                logger.error(me.getMessage(), me);
            }
        }
        
        // Send email with details of the error to the staging app admins
        // In debug mode, send this email to the curator
        Message msg2 = new MimeMessage(session);
		try {
		    msg2.setSubject("ERROR - cBioPortal staging app: unexpected error. Server: "+serverAlias+". Studies: "+studies);
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
		    Template t = freemarkerConfig.getTemplate("genericError.ftl");
            Map<String, String> messageParams = new HashMap<String, String>();
            messageParams.put("users", studyCuratorEmails);
		    messageParams.put("errorMessage", errorMessage);
		    messageParams.put("displayError", displayError(e));
		    String message = FreeMarkerTemplateUtils.processTemplateIntoString(t, messageParams);
		    msg2.setContent(message, "text/html; charset=utf-8");
		    msg2.setSentDate(new Date());
		    Transport.send(msg2);
		} catch(MessagingException me) {
		    logger.error(me.getMessage(), me);
		}
	}

}
