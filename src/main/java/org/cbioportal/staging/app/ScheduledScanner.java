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
package org.cbioportal.staging.app;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.cbioportal.staging.etl.ETLProcessRunner;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.report.IReportingService;
import org.cbioportal.staging.services.resource.IResourceCollector;
import org.cbioportal.staging.services.resource.IResourceFinder;
import org.cbioportal.staging.services.resource.ResourceIgnoreSet;
import org.cbioportal.staging.services.resource.Study;
import org.cbioportal.staging.services.scanner.IScheduledScannerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Main app class to run the staging and loading process as a background service
 *
 */

@Component
public class ScheduledScanner {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);
	private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

	@Value("${scan.cron:* * * * * *}")
	private String scanCron;

	@Value("${scan.location:}")
	private Resource scanLocation;

	@Value("${scan.cron.iterations:-1}")
	private Integer scanIterations;
	private int nrIterations = 0;

	@Value("${scan.ignore.appendonsuccess:false}")
	private boolean ignoreAppendSuccess;

	@Value("${scan.ignore.appendonfailure:false}")
	private boolean ignoreAppendFailure;

	@Autowired
	private IResourceCollector resourceCollector;

	@Autowired
	private IScheduledScannerService scheduledScannerService;

	@Autowired
	private IResourceFinder resourceFinder;

	@Autowired
	private IReportingService reporingService;

	@Autowired
	private ETLProcessRunner etlProcessRunner;

	@Autowired
	private ResourceIgnoreSet resourceIgnoreSet;

	@Scheduled(cron = "${scan.cron:* * * * * *}")
	public boolean scan() {

		if (scanLocation == null) {
			logger.info("No scan.location property is defined. Exiting...");
			scheduledScannerService.stopApp();
		}

		Study[] resourcesPerStudy = {};

		try {
			logger.info("Fixed Rate Task :: Execution Time - {}", dateTimeFormatter.format(LocalDateTime.now()));
			nrIterations++;

			logger.info("Started fetching of resources.");
			Resource inputDir = resourceFinder.getInputDirectory();
			resourcesPerStudy = resourceCollector.getResources(inputDir);

			if (resourcesPerStudy.length == 0) {
				return shouldStopApp();
			}

			logger.info("Started ETL process for studies: ", Stream.of(resourcesPerStudy).map(Study::getStudyId).collect(Collectors.joining(", ")));

			etlProcessRunner.run(resourcesPerStudy);

			if (ignoreAppendSuccess || ignoreAppendFailure)
				addToIgnoreFile(etlProcessRunner.getLoaderExitStatus(), resourcesPerStudy);

		} catch (Exception e) {
			try {
				logger.error("An error not expected occurred. Stopping process... Error found: " + e.getMessage());
				reporingService.reportGenericError("An error not expected occurred. Stopping process... \n\nError found: \n" + e.getMessage(), e);
			} catch (Exception e1) {
				logger.error("The email could not be sent due to the error specified below.");
				e1.printStackTrace();
			} finally {
				e.printStackTrace();
				scheduledScannerService.stopApp();
			}
		}

		return shouldStopApp(resourcesPerStudy, etlProcessRunner.getLoaderExitStatus());
	}

	private void addToIgnoreFile(Map<Study,ExitStatus> loaderStatus, Study[] resourcesPerStudy) {

		List<String> successStudyIds = loaderStatus.entrySet().stream()
			.filter(e -> e.getValue() == ExitStatus.SUCCESS)
			.map(e -> e.getKey().getStudyId())
			.collect(Collectors.toList());

		List<Study> successStudies = Stream.of(resourcesPerStudy)
			.filter(s -> successStudyIds.contains(s.getStudyId()))
			.collect(Collectors.toList());

		List<Study> failureStudies = Stream.of(resourcesPerStudy)
				.filter(s -> ! successStudyIds.contains(s.getStudyId()))
				.collect(Collectors.toList());

		if (ignoreAppendSuccess)
			successStudies.stream().forEach(s -> {
				try {
					resourceIgnoreSet.appendResources(s.getResources());
				} catch (ResourceCollectionException ex) {
					throw new RuntimeException(ex);
				}
			});

		if (ignoreAppendFailure)
			failureStudies.stream().forEach(s -> {
				try {
					resourceIgnoreSet.appendResources(s.getResources());
				} catch (ResourceCollectionException ex) {
					throw new RuntimeException(ex);
				}
			});
	}

	private boolean shouldStopApp() {
		// When scanning every second, we assume that the scanner should run
		// only once. The appl is closed when the patter is '* * * * * *'.
		if (scanCron.equals("* * * * * *")) {
			logger.info("Closing the app after running one time. When scheduled scanning " +
			"is needed set the scan.cron property to a value different from '* * * * * *'");
			scheduledScannerService.stopAppWithSuccess();
		}
		if (scanIterations != -1 && nrIterations >= scanIterations) {
			logger.info("==>>>>> Reached configured number of iterations (" + scanIterations + "). Exiting... <<<<<==");
			scheduledScannerService.stopAppWithSuccess();
		}
		return true;
	}

	private boolean shouldStopApp(Study[] resourcesPerStudy, Map<Study, ExitStatus> LoaderExitStatus) {
		// When scanning every second, we assume that the scanner should run
		// only once. The appl is closed when the patter is '* * * * * *'.
		if (scanCron.equals("* * * * * *")) {
			logger.info("Closing the app after running one time. When scheduled scanning " +
			"is needed set the scan.cron property to a value different from '* * * * * *'");
			//Return error if the study doesn't reach the loader step (size = 0) or if the Loader Exit status is not SUCCESS
			if (resourcesPerStudy.length == 1 && (LoaderExitStatus.size() == 0 || LoaderExitStatus.values().toArray()[0] != ExitStatus.SUCCESS)) {
				scheduledScannerService.stopApp(); 
			}
			scheduledScannerService.stopAppWithSuccess();
		}
		if (scanIterations != -1 && nrIterations >= scanIterations) {
			logger.info("==>>>>> Reached configured number of iterations (" + scanIterations + "). Exiting... <<<<<==");
			scheduledScannerService.stopAppWithSuccess();
		}
		return true;
	}

}
