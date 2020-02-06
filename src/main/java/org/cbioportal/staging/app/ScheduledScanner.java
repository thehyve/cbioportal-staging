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
import org.cbioportal.staging.services.IEmailService;
import org.cbioportal.staging.services.IScheduledScannerService;
import org.cbioportal.staging.services.resource.IResourceCollector;
import org.cbioportal.staging.services.resource.ResourceIgnoreSet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
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

	@Value("${scan.location}")
	private String scanLocation;

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
	private IEmailService emailService;

	@Autowired
	private ETLProcessRunner etlProcessRunner;

	@Autowired
	private ResourcePatternResolver resourcePatternResolver;

	@Autowired
	private ResourceIgnoreSet resourceIgnoreSet;

	@Scheduled(cron = "${scan.cron}")
	public boolean scan() {
		try {
			logger.info("Fixed Rate Task :: Execution Time - {}", dateTimeFormatter.format(LocalDateTime.now()));
			nrIterations++;

			logger.info("Started fetching of resources.");
			Resource scanDir = resourcePatternResolver.getResource(scanLocation);
			Map<String, Resource[]> resourcesPerStudy = resourceCollector.getResources(scanDir);

			if (resourcesPerStudy.keySet().size() == 0) {
				checkIfShouldExit(nrIterations, scanIterations);
				return false;
			}

			logger.info("Started ETL process for studies: ", String.join(", ", resourcesPerStudy.keySet()));

			etlProcessRunner.run(resourcesPerStudy);

			if (ignoreAppend)
				addToIgnoreFile(etlProcessRunner.getLoaderExitStatus(), resourcesPerStudy);

		} catch (Exception e) {
			try {
				logger.error("An error not expected occurred. Stopping process... Error found: " + e.getMessage());
				emailService.emailGenericError("An error not expected occurred. Stopping process... \n\nError found: \n" + e.getMessage(), e);
			} catch (Exception e1) {
				logger.error("The email could not be sent due to the error specified below.");
				e1.printStackTrace();
			} finally {
				e.printStackTrace();
				scheduledScannerService.stopApp();
			}
		}

		//return true when an ETL process was triggered
		return true;
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

	/**
	 * Checks if nrIterations reached the configured max and
	 * triggers System.exit if this is the case.
	 *
	 * @param nrIterations
	 * @param max
	 */
	private void checkIfShouldExit(int nrIterations, int max) {
		if (max != -1 && nrIterations >= max) {
			logger.info("==>>>>> Reached configured number of iterations (" + max + "). Exiting... <<<<<==");
			scheduledScannerService.stopAppWithSuccess();
		}
	}

}
