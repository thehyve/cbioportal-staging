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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.TransformerException;
import org.cbioportal.staging.services.IDirectoryCreator;
import org.cbioportal.staging.services.ITransformerService;
import org.cbioportal.staging.services.resource.IResourceProvider;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class Transformer {
    private static final Logger logger = LoggerFactory.getLogger(Transformer.class);

    @Autowired
    private ITransformerService transformerService;

    @Autowired
    private ResourceUtils utils;

    @Autowired
    private IResourceProvider provider;

    @Autowired
	private IDirectoryCreator directoryCreator;

    public enum ExitStatus {
        SUCCESS, WARNINGS, ERRORS, NOTRANSF; // TODO - Remove "no transformation" option from this file and move it up
                                             // to ETLProcessRunner
    }

    final private Map<String, Resource> logFiles = new HashMap<>();
    final private Map<String, Resource> dirsValidStudies = new HashMap<>();

    private boolean metaFileExists(Resource originPath) throws ResourceCollectionException {
        Resource[] studyFiles = provider.list(originPath);
        return Stream.of(studyFiles).anyMatch(f -> f.getFilename().contains("meta_study.txt"));
        // Resource metaStudyFile = utils.getResource(originPath, "meta_study.txt");
        // return metaStudyFile != null && metaStudyFile.exists() && utils.isFile(metaStudyFile);
    }

    public Map<String, ExitStatus> transform(String timestamp, Map<String, Resource> studyPaths, String transformationCommand) throws TransformerException {

        logFiles.clear();
        dirsValidStudies.clear();

        Map<String, ExitStatus> statusStudies = new HashMap<String, ExitStatus>();

        for (String studyId : studyPaths.keySet()) {

            ExitStatus transformationStatus = ExitStatus.SUCCESS;
            Resource transformedFilesPath;
            try {
                Resource untransformedFilesPath = studyPaths.get(studyId);
                transformedFilesPath = directoryCreator.createTransformedStudyDir(timestamp, studyId, untransformedFilesPath);

                Resource logFile = utils.createFileResource(transformedFilesPath, studyId + "_transformation_log.txt");
                logFiles.put(studyId+" loading log", logFile);

                if (metaFileExists(untransformedFilesPath)) {
                    utils.copyDirectory(untransformedFilesPath, transformedFilesPath);
                    transformationStatus = ExitStatus.NOTRANSF;
                } else {
                    transformationStatus = transformerService.transform(untransformedFilesPath, transformedFilesPath, logFile);
                }

            } catch (Exception e) {
                throw new TransformerException(e);
            }

            //Add status of the validation for the study
            statusStudies.put(studyId, transformationStatus);
            if (transformationStatus == ExitStatus.SUCCESS) {
                dirsValidStudies.put(studyId, transformedFilesPath);
                logger.info("Transformation of study "+studyId+" finished successfully.");
            } else if (transformationStatus == ExitStatus.WARNINGS) {
                logger.info("Transformation of study "+studyId+" finished successfully with warnings.");
            } else if (transformationStatus == ExitStatus.NOTRANSF) {
                dirsValidStudies.put(studyId, transformedFilesPath);
                logger.info("Study "+studyId+" does contain a meta file, so the transformation step is skipped.");
            } else {
                logger.error("Transformation process of study "+studyId+" failed.");
            }

        }

        logger.info("Transformation step finished.");
        return statusStudies;
    }

    public Map<String, Resource> getLogFiles() {
        return logFiles;
    }

    public Map<String, Resource> getValidStudies() {
        return dirsValidStudies;
    }
}
