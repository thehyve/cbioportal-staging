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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.TransformerException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.directory.IDirectoryCreator;
import org.cbioportal.staging.services.etl.ITransformerService;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.cbioportal.staging.services.resource.Study;
import org.cbioportal.staging.services.resource.filesystem.FileSystemResourceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class Transformer {
    private static final Logger logger = LoggerFactory.getLogger(Transformer.class);

    @Value("${transformation.metafile.check:true}")
    private boolean transformationMetaFileCheck;

    @Autowired
    private ITransformerService transformerService;

    @Autowired
    private ResourceUtils utils;

    @Autowired
    private FileSystemResourceProvider fileSystemResourceProvider;

    @Autowired
	private IDirectoryCreator directoryCreator;

    final private Map<Study, Resource> logFiles = new HashMap<>();
    final private List<Study> validStudies = new ArrayList<>();

    public Map<Study, ExitStatus> transform(Study[] studies)
        throws TransformerException {

        logFiles.clear();
        validStudies.clear();

        Map<Study, ExitStatus> statusStudies = new HashMap<Study, ExitStatus>();

        try {
            for (Study study: studies) {

                String studyId = study.getStudyId();
                ExitStatus transformationStatus = ExitStatus.SUCCESS;
                Resource transformedFilesPath;
                try {
                    Resource untransformedFilesPath = study.getStudyDir();
                    logger.debug("Creating transformed data study directory: untransformedFilesPath=" + untransformedFilesPath);
                    transformedFilesPath = directoryCreator.createTransformedStudyDir(study, untransformedFilesPath);

                    logger.info("Created transformed data study directory: transformedFilesPath=" + transformedFilesPath.getFilename());
                    logger.debug("Creating log file.");
                    Resource logFile = utils.createFileResource(transformedFilesPath, study.getStudyId() + "_transformation_log.txt");
                    logFiles.put(study, logFile);

                    if (transformationMetaFileCheck && metaFileExists(untransformedFilesPath)) {
                        utils.copyDirectory(untransformedFilesPath, transformedFilesPath);
                        transformationStatus = ExitStatus.SKIPPED;
                    } else {
                        transformationStatus = transformerService.transform(untransformedFilesPath, transformedFilesPath, logFile);
                    }

                } catch (Exception e) {
                    throw new TransformerException(e);
                }

                logger.debug("Collecting transformed resources.");
                Resource[] resources = fileSystemResourceProvider.list(transformedFilesPath);
                Study transformedStudy = new Study(studyId, study.getVersion(), study.getTimestamp(), transformedFilesPath, resources);

                //Add status of the validation for the study
                statusStudies.put(study, transformationStatus);
                if (transformationStatus == ExitStatus.SUCCESS) {
                    validStudies.add(transformedStudy);
                    logger.info("Transformation of study "+studyId+" finished successfully.");
                } else if (transformationStatus == ExitStatus.WARNING) {
                    validStudies.add(transformedStudy);
                    logger.warn("Transformation of study "+studyId+" finished successfully with warnings.");
                } else if (transformationStatus == ExitStatus.SKIPPED) {
                    validStudies.add(transformedStudy);
                    logger.info("Study "+studyId+" does contain a meta file, so the transformation step is skipped.");
                } else {
                    logger.error("Transformation process of study "+studyId+" failed.");
                }
            }

        } catch (ResourceCollectionException e) {
            throw new TransformerException(e);
        }

        logger.info("Transformation step finished.");
        return statusStudies;
    }

    public boolean metaFileExists(Resource originPath) throws ResourceCollectionException {
        Resource[] studyFiles = fileSystemResourceProvider.list(originPath);
        return Stream.of(studyFiles).anyMatch(f -> f.getFilename().contains("meta_study.txt"));
    }

    public Map<Study, Resource> getLogFiles() {
        return logFiles;
    }

    public Study[] getValidStudies() {
        return validStudies.toArray(new Study[0]);
    }
}
