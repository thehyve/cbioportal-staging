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
import java.util.Map.Entry;
import java.util.stream.Collectors;

import org.cbioportal.staging.etl.ETLProcessRunner;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.IScheduledScannerService;
import org.cbioportal.staging.services.reporting.IReportingService;
import org.cbioportal.staging.services.resource.IResourceCollector;
import org.cbioportal.staging.services.resource.ResourceIgnoreSet;
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
	private boolean ignoreAppend;

	@Autowired
	private IResourceCollector resourceCollector;

	@Autowired
	private IScheduledScannerService scheduledScannerService;

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

		try {
			logger.info("Fixed Rate Task :: Execution Time - {}", dateTimeFormatter.format(LocalDateTime.now()));
			nrIterations++;

			logger.info("Started fetching of resources.");
			Map<String, Resource[]> resourcesPerStudy = resourceCollector.getResources(scanLocation);

			if (resourcesPerStudy.keySet().size() == 0) {
				return shouldStopApp();
			}

			logger.info("Started ETL process for studies: ", String.join(", ", resourcesPerStudy.keySet()));

			etlProcessRunner.run(resourcesPerStudy);

			if (ignoreAppend)
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

		return shouldStopApp();
	}

	private void addToIgnoreFile(Map<String,ExitStatus> loaderStatus, Map<String, Resource[]> resourcesPerStudy) {

		List<String> successStudies = loaderStatus.entrySet().stream()
			.filter(e -> e.getValue() == ExitStatus.SUCCESS)
			.map(Entry::getKey)
			.collect(Collectors.toList());

		List<Entry<String,Resource[]>> exludeResources = resourcesPerStudy.entrySet().stream()
			.filter(e -> successStudies.contains(e.getKey()))
			.collect(Collectors.toList());

		exludeResources.stream().forEach(e -> {
			try {
				resourceIgnoreSet.appendResources(e.getValue());
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

}
