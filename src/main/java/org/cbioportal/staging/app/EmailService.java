package org.cbioportal.staging.app;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
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
import org.cbioportal.staging.exceptions.TransformerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class EmailService {
	
	@Autowired
	//private ConfigService configService;
    //private JavaMailSender mailSender;
    //private VelocityEngine velocityEngine;
	
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

	public Properties getProperties() {
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
	
	public Session getSession(Properties properties) {
		return Session.getInstance(properties,
				  new javax.mail.Authenticator() {
					protected PasswordAuthentication getPasswordAuthentication() {
						return new PasswordAuthentication(mailAppUser, mailAppPassword);
					}
				  });
	}
	
	public void emailStudyFileNotFound(Map<String, ArrayList<String>> failedStudies) throws UnsupportedEncodingException {
		StringBuilder allFailedStudies = new StringBuilder();
		for (String failedStudy : failedStudies.keySet()) {
			allFailedStudies.append("STUDY: "+failedStudy+"<br>");
			for (String failedFile : failedStudies.get(failedStudy)) {
				allFailedStudies.append("- "+failedFile+"<br>");
			}
		}
		String finalFailedStudies = allFailedStudies.toString();

		Properties properties = getProperties();
		Session session = getSession(properties);
		
		Message msg = new MimeMessage(session);
		try {
		    msg.setSubject("ERROR - cBioPortal staging app: transformation step failed");
		    msg.setRecipient(Message.RecipientType.TO, new InternetAddress(mailAdminUser, false));
		    msg.setFrom(new InternetAddress("noreply@cbioportal.org", "cBioPortal staging app"));
		    String message = "<div>Dear cBioPortal Administrator,<br><br>"+
		    "While checking the S3 location for the study files, "+
		    "the following files were found to be missing (after "+
		    "trying 5 times over a period of 25 minutes):<br>" + 
		    finalFailedStudies +
		    "<br>Please add these files or update the "+
		    "\"list_of_studies.yaml\" configuration file on S3.<br><br>" +
		    "Regards,<br>" + 
		    "cBioPortal staging app. </div>";
		    msg.setContent(message, "text/html; charset=utf-8");
		    msg.setSentDate(new Date());
		    Transport.send(msg);
		} catch(MessagingException me) {
		    logger.info(me.getMessage());
		}
	}
	
	public String displayError(Throwable t) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		t.printStackTrace(pw);
		String stackTrace = sw.toString();
		return stackTrace.replace(System.getProperty("line.separator"), "<br/>\n");
	}
	
	public void emailStudyError(String studyId, Exception e) throws UnsupportedEncodingException {
		Properties properties = getProperties();
		Session session = getSession(properties);
		
		Message msg = new MimeMessage(session);
		try {
		    msg.setSubject("ERROR - cBioPortal staging app: transformation step failed for study "+studyId);
		    msg.setRecipient(Message.RecipientType.TO, new InternetAddress(mailAdminUser, false));
		    msg.setFrom(new InternetAddress("noreply@cbioportal.org", "cBioPortal staging app"));
		    String message = "<div>Dear cBioPortal Administrator,<br><br>"+
		    "The study files for study "+studyId+" could not be transformed by the given "+
		    "transformation script. Error details:<br>"+
		    displayError(e) +
		    "<br>Please check the data files and/or the transformation script and "+
		    "fix them accordingly.<br><br>" +
		    "Regards,<br>" + 
		    "cBioPortal staging app. </div>";
		    msg.setContent(message, "text/html; charset=utf-8");
		    msg.setSentDate(new Date());
		    Transport.send(msg);
		} catch(MessagingException me) {
		    logger.info(me.getMessage());
		}
	}
	
	public void emailValidationReport(Map<Pair<String,String>,List<Integer>> validatedStudies, String level) throws UnsupportedEncodingException {
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
			if (validatedStudies.get(study).get(1).equals(0)) {
				if (validatedStudies.get(study).get(0).equals(0)) { //Study with no warnings and no errors
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
		    String message = "<div>Dear cBioPortal Administrator,<br><br>"+
		    "New studies were found on S3. They were transformed to cBioPortal staging files and " +
		    	"the staging files have been validated. These are the validation reports:" +
		    studies +
		    "<br><br>The system will proceed and attempt loading the " +
		    studiesToLoad + " studies. " + 
		    "Please update the other studies accordingly.<br><br>"+
		    "Regards,<br>" + 
		    "cBioPortal staging app. </div>";
		    msg.setContent(message, "text/html; charset=utf-8");
		    msg.setSentDate(new Date());
		    Transport.send(msg);
		} catch(MessagingException me) {
		    logger.info(me.getMessage());
		}
	}
	
	public void emailStudiesLoaded(Map<String,String> studiesLoaded) throws UnsupportedEncodingException {
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
		    String message = "<div>Dear cBioPortal Administrator,<br><br>"+
		    "The system tried loading the studies below. These are the data loading " + 
		    "log files for each of the attempted studies:"+
		    studies +
		    "<br><br>The <b><font style=\"color: #04B404\">SUCCESSFULLY LOADED</font></b> studies are " + 
		    "available for querying in the portal.<br><br>" +
		    "Regards,<br>" + 
		    "cBioPortal staging app. </div>";
		    msg.setContent(message, "text/html; charset=utf-8");
		    msg.setSentDate(new Date());
		    Transport.send(msg);
		} catch(MessagingException me) {
		    logger.info(me.getMessage());
		}
	}
	
	public void emailGenericError(String errorMessage, Exception e) throws UnsupportedEncodingException {
		Properties properties = getProperties();
		Session session = getSession(properties);
		
		Message msg = new MimeMessage(session);
		try {
		    msg.setSubject("ERROR - cBioPortal staging app: unexpected error");
		    msg.setRecipient(Message.RecipientType.TO, new InternetAddress(mailAdminUser, false));
		    msg.setFrom(new InternetAddress("noreply@cbioportal.org", "cBioPortal staging app"));
		    String message = "<div>Dear cBioPortal Administrator,<br><br>"+
		    "An unexpected error occurred while running one of the steps. Error details:<br> "+
		    errorMessage + "<br><br>" +
		    displayError(e) +
		    "<br>Please check if the necessary dependencies are up and running and " +
		    "if current configuration is still correct.<br><br>" +
		    "Regards,<br>" + 
		    "cBioPortal staging app. </div>";
		    msg.setContent(message, "text/html; charset=utf-8");
		    msg.setSentDate(new Date());
		    Transport.send(msg);
		} catch(MessagingException me) {
		    logger.info(me.getMessage());
		}
	}

}
