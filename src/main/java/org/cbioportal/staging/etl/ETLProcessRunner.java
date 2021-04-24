/*
 * Copyright (c) 2020 The Hyve B.V.
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

import java.io.IOException;
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.LoaderException;
import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.exceptions.TransformerException;
import org.cbioportal.staging.exceptions.ValidatorException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.authorize.IAuthorizerService;
import org.cbioportal.staging.services.command.IRestarter;
import org.cbioportal.staging.services.etl.EtlUtils;
import org.cbioportal.staging.services.publish.IPublisherService;
import org.cbioportal.staging.services.report.IReportingService;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.cbioportal.staging.services.resource.Study;
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

    @Autowired
	private EtlUtils etlUtils;

	@Value("${study.authorize.command_prefix:}")
    private String studyAuthorizeCommandPrefix;

    @Value("${etl.working.dir:}")
    private Resource etlWorkingDir;

    @Value("${validation.level:ERROR}")
	private String validationLevel;

	Map<Study, ExitStatus> transformerExitStatus;
	Map<Study, ExitStatus> validatorExitStatus;
	Map<Study, ExitStatus> loaderExitStatus;

	public void run(Study[] remoteResources) throws Exception {
		try  {
			startProcess();

			if (etlWorkingDir  == null) {
                throw new ConfigurationException("etl.working.dir not defined. Please check the application properties.");
			} else {
				utils.ensureDirs(etlWorkingDir);
			}

            //E (Extract) step:
			Study[] localResources = extractor.run(remoteResources);

			if (! extractor.errorFiles().isEmpty()) {
				reportingService.reportStudyFileNotFound(extractor.errorFiles(), extractor.getTimeRetry());
			}

			transformerExitStatus = new HashMap<>();
			validatorExitStatus = new HashMap<>();
			loaderExitStatus = new HashMap<>();

			//T (TRANSFORM) STEP:
			Study[] transformedStudies;
			if (etlUtils.doTransformation()) {
				transformerExitStatus = transformer.transform(localResources);
                publisher.publishFiles(transformer.getLogFiles());
                transformedStudies = transformer.getValidStudies();
			} else {
                for (Study study : localResources) {
                    transformerExitStatus.put(study, ExitStatus.SKIPPED);
                }
				transformedStudies = localResources;
			}

			//V (VALIDATE) STEP:
			if (transformedStudies.length > 0) {
                validatorExitStatus = validator.validate(transformedStudies);
                publisher.publishFiles(validator.getLogFiles());
                publisher.publishFiles(validator.getReportFiles());

				Study[] studiesThatPassedValidation = validator.getValidStudies();

				//L (LOAD) STEP:
				if (studiesThatPassedValidation.length > 0) {
                    loaderExitStatus = loader.load(studiesThatPassedValidation);
                    publisher.publishFiles(loader.getLogFiles());

					if (loader.areStudiesLoaded()) {
                        restarterService.restart();
						if (studyAuthorizeCommandPrefix != null && ! studyAuthorizeCommandPrefix.equals("")) {
                            Set<String> studyIds = new HashSet<String>();
                            for (Study study : validatorExitStatus.keySet()) {
                                studyIds.add(study.getStudyId());
                            }
							authorizer.authorizeStudies(studyIds);
						}
					}
				}
            }
            reportSummary(localResources, transformer.getLogFiles(), validator.getLogFiles(), validator.getReportFiles(), loader.getLogFiles(),
                transformerExitStatus, validatorExitStatus, loaderExitStatus);
		} catch (TransformerException e) {
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

    private void reportSummary(Study[] studies, Map<Study,Resource> transformerLogs, Map<Study,Resource> validatorLogs,
    Map<Study,Resource> validatorReports, Map<Study,Resource> loaderLogs, Map<Study,ExitStatus> transformerStatus,
    Map<Study,ExitStatus> validatorStatus, Map<Study,ExitStatus> loaderStatus) throws ReporterException {

        for (Study study : studies) {
            logger.debug("ETL calling the Reporting Service...");
            reportingService.reportSummary(study, getStudyLogs(study.getStudyId(), transformerLogs), getStudyLogs(study.getStudyId(),validatorLogs),
            getStudyLogs(study.getStudyId(), validatorReports), getStudyLogs(study.getStudyId(),loaderLogs),
            getStudyStatus(study.getStudyId(), transformerStatus), getStudyStatus(study.getStudyId(), validatorStatus),
            getStudyStatus(study.getStudyId(), loaderStatus));
        }

    }

    private Resource getStudyLogs(String studyId, Map<Study, Resource> info) {
        Resource studyLogs = null;
        for (Study study : info.keySet()) {
            if (study.getStudyId().equals(studyId)) {
                studyLogs = info.get(study);
            }
        }
        return studyLogs;
    }

    private ExitStatus getStudyStatus(String studyId, Map<Study, ExitStatus> info) {
        ExitStatus studyStatus = null;
        for (Study study : info.keySet()) {
            if (study.getStudyId().equals(studyId)) {
                studyStatus = info.get(study);
            }
        }
        return studyStatus;
    }

	public Map<Study, ExitStatus> getTransformerExitStatus() {
		return transformerExitStatus;
	}

	public Map<Study, ExitStatus> getValidatorExitStatus() {
		return validatorExitStatus;
	}

	public Map<Study, ExitStatus> getLoaderExitStatus() {
		return loaderExitStatus;
	}

}
