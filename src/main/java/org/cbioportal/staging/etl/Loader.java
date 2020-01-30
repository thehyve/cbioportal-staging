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
import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.etl.Transformer.ExitStatus;
import org.cbioportal.staging.exceptions.LoaderException;
import org.cbioportal.staging.services.LoaderService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Loader {
	private static final Logger logger = LoggerFactory.getLogger(Loader.class);

	@Autowired
    private LoaderService loaderService;

    private boolean areStudiesLoaded = false;

    private String logSuffix = "_loading_log.txt";

	Map<String, ExitStatus> load(final Map<String, File> studyPaths) throws LoaderException {

        final Map<String, ExitStatus> loadResults = new HashMap<String, ExitStatus>();

        for (final String studyId : studyPaths.keySet()) {
            logger.info("Starting loading of study " + studyId + ". This can take some minutes.");
            final File studyPath = studyPaths.get(studyId);
            // Create loading log file
            final File logFile = new File(studyPath + "/" + studyId + logSuffix);
            ExitStatus loadingStatus = loaderService.load(studyPath, logFile);
            //Add loading result for the email loading report
            if (loadingStatus == ExitStatus.SUCCESS) {
                loadResults.put(studyId, ExitStatus.SUCCESS);
                areStudiesLoaded = true;
                logger.info("Loading of study "+studyId+" finished successfully.");
            } else {
                loadResults.put(studyId, ExitStatus.ERRORS);
                logger.error("Loading process of study "+studyId+" failed.");
            }
        }
        return loadResults;
    }

    Map<String, File> getLogFiles(Map<String, File> studyPaths) {
        Map<String, File> logFiles = new HashMap<String, File>();
        for (String studyId : studyPaths.keySet()) {
            File logFile = new File(studyPaths.get(studyId)+"/"+studyId+logSuffix);
            logFiles.put(studyId, logFile);
        }
        return logFiles;
    }

    boolean areStudiesLoaded() {
        return areStudiesLoaded;
	}
}
