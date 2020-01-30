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

import java.io.File;
import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.etl.Transformer.ExitStatus;
import org.cbioportal.staging.exceptions.LoaderException;
import org.cbioportal.staging.exceptions.TransformerException;
import org.cbioportal.staging.exceptions.ValidatorException;
import org.cbioportal.staging.services.EmailService;
import org.cbioportal.staging.services.PublisherService;
import org.cbioportal.staging.services.RestarterService;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Main ETL process.
 *
 * @author pieter
 *
 */
@Component
public class ETLProcessRunner {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);

	@Autowired
	private Extractor extractor;

	@Autowired
	private Transformer transformer;

	@Autowired
	private Validator validator;

	@Autowired
	private Loader loader;

	@Autowired
	private RestarterService restarterService;

	@Autowired
    private Authorizer authorizer;

    @Autowired
    private PublisherService publisher;

    @Autowired
	private EmailService emailService;

	@Autowired
	private ResourceUtils utils;

	@Value("${study.authorize.command_prefix:null}")
    private String studyAuthorizeCommandPrefix;

    @Value("${central.share.location}")
    private String centralShareLocation;

    @Value("${etl.working.dir:}")
    private File etlWorkingDir;

    @Value("${skip.transformation:false}")
    private boolean skipTransformation;

    @Value("${validation.level:ERROR}")
	private String validationLevel;

	public void run(Map<String, Resource[]> remoteResources) throws Exception {
		try  {

			String date = utils.getTimeStamp("yyyyMMdd-HHmmss");
			startProcess();

			utils.ensureDirs(etlWorkingDir);

            //E (Extract) step:
			Map<String,File> localResources = extractor.run(remoteResources);

			if (! extractor.errorFiles().isEmpty()) {
				emailService.emailStudyFileNotFound(extractor.errorFiles(), extractor.getTimeRetry());
			}

			Map<String, String> logPaths = new HashMap<String, String>();

			//T (TRANSFORM) STEP:
			Map<String, File> transformedStudiesPaths;
			if (skipTransformation) {
				transformedStudiesPaths = localResources;
			} else {
				String logSuffix = "_transformation_log.txt";
				Map<String, ExitStatus> transformedStudiesStatus = transformer.transform(date, localResources, "command", logSuffix);
				transformedStudiesPaths = transformer.getTransformedStudiesPaths(localResources, transformedStudiesStatus);
				publisher.publish(date, transformedStudiesPaths, logPaths, "transformation log", logSuffix);
				if (logPaths.size() > 0) {

					// TODO ask transformer for logs
					emailService.emailTransformedStudies(transformedStudiesStatus, logPaths);
				}
			}

			//V (VALIDATE) STEP:
			if (! transformedStudiesPaths.isEmpty()) {
				String reportSuffix = "_validation_report.html";
				String logSuffix = "_validation_log.txt";
				Map<String, ExitStatus> validatedStudies = validator.validate(transformedStudiesPaths, reportSuffix, logSuffix);
				publisher.publish(date, transformedStudiesPaths, logPaths, "validation log", logSuffix);
				publisher.publish(date, transformedStudiesPaths, logPaths, "validation report", reportSuffix);
				emailService.emailValidationReport(validatedStudies, validationLevel, logPaths);
				Map <String, File> studiesThatPassedValidation = validator.getStudiesThatPassedValidation(validatedStudies, localResources);

				//L (LOAD) STEP:
				if (studiesThatPassedValidation.size() > 0) {
					String loadingLogSuffix = "_loading_log.txt";
					Map<String, ExitStatus> loadResults = loader.load(studiesThatPassedValidation, loadingLogSuffix);
					publisher.publish(date, studiesThatPassedValidation, logPaths, "loading log", loadingLogSuffix);
					emailService.emailStudiesLoaded(loadResults, logPaths);

					if (loader.areStudiesLoaded()) {
						restarterService.restart();
						if (!studyAuthorizeCommandPrefix.equals("null")) {
							authorizer.authorizeStudies(validatedStudies.keySet());
						}
					}
				}
			}
		} catch (TransformerException e) {
			try {
				logger.error("An error occurred during the transformation step. Error found: "+ e);
				emailService.emailGenericError("An error occurred during the transformation step. Error found: ", e);
			} catch (Exception e1) {
				logger.error("The email could not be sent due to the error specified below.");
				e1.printStackTrace();
			}
		} catch (ValidatorException e) {
			try {
				logger.error("An error occurred during the validation step. Error found: "+ e);
				emailService.emailGenericError("An error occurred during the validation step. Error found: ", e);
			} catch (Exception e1) {
				logger.error("The email could not be sent due to the error specified below.");
				e1.printStackTrace();
			}
		} catch (LoaderException e) {
			try {
				logger.error("An error occurred during the loading step. Error found: "+ e);
				emailService.emailGenericError("An error occurred during the loading step. Error found: ", e);
			} catch (Exception e1) {
				logger.error("The email could not be sent due to the error specified below.");
				e1.printStackTrace();
			}
		}
		finally
		{
			//end process / release lock:
			endProcess();
		}
	}

	/**
	 * Socket configuration and respective synchronized start method that ensures only one
	 * ETL process runs at a time (also if started in another process - this is guaranteed by
	 * implementation of a race condition on a single socket). This is important because
	 * cBioPortal currently only supports loading one study at a time, since this data loading
	 * operation is not thread safe in cBioPortal itself.
	 */
	@Value("${etl.lock.port:9999}")
	private Integer PORT;
	private static ServerSocket socket;

	private synchronized void startProcess() {
		try {
			//"Lock implementation". Try to reserve a socket:
			logger.info("Reserving socket on port " + PORT + " to avoid parallel ETL processes (not supported)");

			socket = new ServerSocket(PORT,0,InetAddress.getByAddress(new byte[] {127,0,0,1}));
		}
		catch (BindException e) {
			logger.error("Another ETL process is already running.", e);
			throw new RuntimeException("Another ETL process is already running", e);
		}
		catch (IOException e) {
			logger.error("Unexpected error.", e);
			e.printStackTrace();
			throw new RuntimeException("Unexpected error.", e);
		}
	}

	private void endProcess() {
		try {
			logger.info("Closing socket / releasing lock" );
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
