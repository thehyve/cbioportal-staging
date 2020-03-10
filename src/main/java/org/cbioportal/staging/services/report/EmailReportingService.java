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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.mail.internet.MimeMessage;

import org.apache.commons.lang3.ArrayUtils;
import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.resource.Study;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.Resource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value="mail.enable", havingValue = "true")
public class EmailReportingService implements IReportingService {

	private static final Logger logger = LoggerFactory.getLogger(EmailReportingService.class);

	@Value("${mail.to}")
	private String mailTo;

	@Value("${mail.from}")
	private String mailFrom;

	@Value("${scan.location}")
	private String scanLocation;

	@Value("${study.curator.emails:}")
	private String studyCuratorEmails;

	@Value("${server.alias:}")
	private String serverAlias;

	@Value("${debug.mode:false}")
	private Boolean debugMode;

	@Autowired
    private JavaMailSender javaMailSender;

	@Autowired
	private LogMessageUtils messageUtils;

	public void reportStudyFileNotFound(
		Map<String, List<String>> failedStudies,
		Integer timeRetry
	) throws ReporterException {

		logger.debug("Email Service: building reportStudyFileNotFound");

		String finalFailedStudies = String.join(", ", failedStudies.keySet());
		String subject = "ERROR: transformation step failed. Server: "+serverAlias+". Failed studies: "+finalFailedStudies;

		String message =  messageUtils.messageStudyFileNotFound("studyFileNotFound_email.ftl", failedStudies, timeRetry);

		sendMail(debugMode? getEmailAddressesCurators() : getEmailAddressesAll(), subject, message);
	}

	public void reportSummary(
		Study		study,
		Resource	transformerLog,
		Resource	validatorLog,
		Resource	validatorReport,
		Resource	loaderLog,
		ExitStatus	transformerStatus,
		ExitStatus 	validatorStatus,
		ExitStatus 	loaderStatus
	) throws ReporterException {
		logger.debug("Email Service: building reportSummary");

		String subject = "INFO: summary for load process of study "+study.getStudyId()+" in server "+serverAlias+".";

		String message = messageUtils.messageSummaryStudies(
			"summary_email.ftl", study, serverAlias, transformerStatus,
			transformerLog, validatorStatus, validatorLog, validatorReport,
			loaderStatus, loaderLog
		);

		sendMail(debugMode? getEmailAddressesCurators() : getEmailAddressesAll(), subject, message);
	}

	public void reportGenericError(String errorMessage, Exception e) throws ReporterException {

		logger.debug("Email Service: building reportGenericError");

		String subject = "ERROR: unexpected error. Server: "+serverAlias;

		String messageTo = messageUtils.messageGenericError("genericError_email.ftl", errorMessage, e);
		if (! debugMode) {
			String curatorMessage = messageUtils.messageGenericErrorUser("genericErrorUser_email.ftl", errorMessage, e);
			sendMail(getEmailAddressesCurators(), subject, curatorMessage);
			sendMail(getEmailAddressesTo(), subject, messageTo);
		} else {
			sendMail(getEmailAddressesCurators(), subject, messageTo);
		}

	}

	private void sendMail(
		String[] recipients,
		String subject,
		String message
	) throws ReporterException {
		try {
			logger.debug("Email Service: MESSAGE=" + message);

			MimeMessage msg = javaMailSender.createMimeMessage();

			MimeMessageHelper helper = new MimeMessageHelper(msg, true);
			helper.setTo(recipients);
			helper.setFrom(mailFrom, "cBioPortal staging app");
			helper.setSubject(subject);
			helper.setText(message, true);
			helper.setSentDate(new Date());

            logger.debug("Email service: trying to send reportSummary ...");
			javaMailSender.send(msg);
            logger.debug("Email service: reportSummary has been sent");
		} catch(Exception me) {
			logger.error(me.getMessage(), me);
			throw new ReporterException("Error while sending email.", me);
		}
	}

	private String[] getEmailAddressesAll() {
		return ArrayUtils.addAll(getEmailAddressesTo(), getEmailAddressesCurators());
	}

	private String[] getEmailAddressesTo() {
		return Stream.of(mailTo.split(","))
			.filter(address -> ! address.equals(""))
			.toArray(String[]::new);
	}

	private String[] getEmailAddressesCurators() {
		return Stream.of(studyCuratorEmails.split(","))
			.filter(address -> ! address.equals(""))
			.toArray(String[]::new);
	}

}
