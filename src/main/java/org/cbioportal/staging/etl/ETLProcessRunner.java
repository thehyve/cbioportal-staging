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

import org.cbioportal.staging.exceptions.LoaderException;
import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.exceptions.ValidatorException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.authorize.IAuthorizerService;
import org.cbioportal.staging.services.command.IRestarter;
import org.cbioportal.staging.services.publish.IPublisherService;
import org.cbioportal.staging.services.report.IReportingService;
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

	private static final Logger logger = LoggerFactory.getLogger(ETLProcessRunner.class);

	@Autowired
	private Extractor extractor;

	@Autowired
	private Transformer transformer;

	@Autowired
	private Validator validator;

	@Autowired
	private Loader loader;

	@Autowired
	private IRestarter restarterService;

	@Autowired
    private IAuthorizerService authorizer;

    @Autowired
    private IPublisherService publisher;

    @Autowired
	private IReportingService reportingService;

	@Autowired
	private ResourceUtils utils;

	@Value("${study.authorize.command_prefix:}")
    private String studyAuthorizeCommandPrefix;

    @Value("${etl.working.dir:}")
    private File etlWorkingDir;

    @Value("${skip.transformation:false}")
    private boolean skipTransformation;

    @Value("${validation.level:ERROR}")
	private String validationLevel;

	Map<String, ExitStatus> transformerExitStatus;
	Map<String, ExitStatus> validatorExitStatus;
	Map<String, ExitStatus> loaderExitStatus;

	public void run(Map<String, Resource[]> remoteResources) throws Exception {
		try  {

			String timestamp = utils.getTimeStamp("yyyyMMdd-HHmmss");
			startProcess();

			utils.ensureDirs(etlWorkingDir);

            //E (Extract) step:
			Map<String,Resource> localResources = extractor.run(remoteResources, timestamp);

			if (! extractor.errorFiles().isEmpty()) {
				reportingService.reportStudyFileNotFound(extractor.errorFiles(), extractor.getTimeRetry());
			}

			Map<String, Resource> logPaths = new HashMap<>();

			transformerExitStatus = new HashMap<>();
			validatorExitStatus = new HashMap<>();
			loaderExitStatus = new HashMap<>();

			//T (TRANSFORM) STEP:
			Map<String, Resource> transformedStudiesPaths;
			if (! skipTransformation) {
				transformerExitStatus = transformer.transform(timestamp, localResources, "command");
                Map<String, Resource> transformationLogFiles = publisher.publish(timestamp, transformer.getLogFiles());
                logPaths.putAll(transformationLogFiles);
				if (logPaths.size() > 0) {
					reportingService.reportTransformedStudies(transformerExitStatus, logPaths);
                }
                transformedStudiesPaths = transformer.getValidStudies();
			} else {
				transformedStudiesPaths = localResources;
			}

			//V (VALIDATE) STEP:
			if (! transformedStudiesPaths.isEmpty()) {
                validatorExitStatus = validator.validate(transformedStudiesPaths);
                Map<String, Resource> validationAndReportFiles = publisher.publish(timestamp, validator.getLogAndReportFiles());
                logPaths.putAll(validationAndReportFiles);
				reportingService.reportValidationReport(validatorExitStatus, validationLevel, logPaths);

				Map <String, Resource> studiesThatPassedValidation = validator.getValidStudies();

				//L (LOAD) STEP:
				if (studiesThatPassedValidation.size() > 0) {
                    loaderExitStatus = loader.load(studiesThatPassedValidation);
                    Map<String, Resource> loadingLogFiles = publisher.publish(timestamp, loader.getLogFiles());
                    logPaths.putAll(loadingLogFiles);
					reportingService.reportStudiesLoaded(loaderExitStatus, logPaths);

					if (loader.areStudiesLoaded()) {
						restarterService.restart();
						if (studyAuthorizeCommandPrefix != null && ! studyAuthorizeCommandPrefix.equals("")) {
							authorizer.authorizeStudies(validatorExitStatus.keySet());
						}
					}
				}
			}
		} catch (ReporterException e) {
			try {
				logger.error("An error occurred during the transformation step. Error found: "+ e);
				reportingService.reportGenericError("An error occurred during the transformation step. Error found: ", e);
			} catch (Exception e1) {
				logger.error("The email could not be sent due to the error specified below.");
				e1.printStackTrace();
			}
		} catch (ValidatorException e) {
			try {
				logger.error("An error occurred during the validation step. Error found: "+ e);
				reportingService.reportGenericError("An error occurred during the validation step. Error found: ", e);
			} catch (Exception e1) {
				logger.error("The email could not be sent due to the error specified below.");
				e1.printStackTrace();
			}
		} catch (LoaderException e) {
			try {
				logger.error("An error occurred during the loading step. Error found: "+ e);
				reportingService.reportGenericError("An error occurred during the loading step. Error found: ", e);
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

	public Map<String, ExitStatus> getTransformerExitStatus() {
		return transformerExitStatus;
	}

	public Map<String, ExitStatus> getValidatorExitStatus() {
		return validatorExitStatus;
	}

	public Map<String, ExitStatus> getLoaderExitStatus() {
		return loaderExitStatus;
	}

}
