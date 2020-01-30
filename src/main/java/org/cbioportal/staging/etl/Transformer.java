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
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.cbioportal.staging.exceptions.TransformerException;
import org.cbioportal.staging.services.TransformerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class Transformer {
	private static final Logger logger = LoggerFactory.getLogger(Transformer.class);

	@Autowired
    private TransformerService transformerService;

    public enum ExitStatus {
        SUCCESS, WARNINGS, ERRORS, NOTRANSF;
    }

	boolean metaFileExists(File originPath) {
		File metaStudyFile = new File(originPath+"/meta_study.txt");
		if (metaStudyFile.exists() && metaStudyFile.isFile()) {
			return true;
		}
		return false;
    }

    File getTransformedFilesPath(File untransformedFilesPath) {
        File transformedFilesPath = new File(untransformedFilesPath+"/staging");
        if (!transformedFilesPath.exists()) {
            transformedFilesPath.mkdir();
        }
        return transformedFilesPath;
    }

	Map<String, ExitStatus> transform(String date, Map<String, File> studyPaths, String transformationCommand, String logSuffix) throws TransformerException {
        Map<String, ExitStatus> statusStudies = new HashMap<String, ExitStatus>();
		for (String studyId : studyPaths.keySet()) {
            File untransformedFilesPath = studyPaths.get(studyId);
            File transformedFilesPath = getTransformedFilesPath(untransformedFilesPath);

            ExitStatus transformationStatus = null;
            //Create transformation log file
            String logName = studyId+logSuffix;
            File logFile = new File(transformedFilesPath+"/"+logName);
			try {
				if (metaFileExists(untransformedFilesPath)) {
                    try {
                        FileUtils.copyDirectory(untransformedFilesPath, transformedFilesPath);
                    } catch(IOException e) {
                        throw new TransformerException("The untransformed files path "+untransformedFilesPath.getAbsolutePath()+
                            " or the transformed files path "+transformedFilesPath.getAbsolutePath()+" do not exist.", e);
                    } finally {
                        transformationStatus = ExitStatus.NOTRANSF;
                    }
				} else {
					transformationStatus = transformerService.transform(untransformedFilesPath, transformedFilesPath, logFile);
				}
			} finally {
                //Add status of the validation for the study
                statusStudies.put(studyId, transformationStatus);
                if (transformationStatus == ExitStatus.SUCCESS) {
                    logger.info("Transformation of study "+studyId+" finished successfully.");
                } else if (transformationStatus == ExitStatus.WARNINGS) {
                    logger.info("Transformation of study "+studyId+" finished successfully with warnings.");
                } else if (transformationStatus == ExitStatus.NOTRANSF) {
                    logger.info("Study "+studyId+" does contain a meta file, so the transformation step is skipped.");
                } else {
                    logger.error("Transformation process of study "+studyId+" failed.");
                }
            }
        }
        logger.info("Transformation step finished.");
        return statusStudies;
    }

    Map<String, File> getTransformedStudiesPaths(Map<String, File> studyPaths, Map<String, ExitStatus> transformedStudiesStatus) {
        Map<String, File> transformedFilesPaths = new HashMap<String, File>();
        for (Map.Entry<String, ExitStatus> entry : transformedStudiesStatus.entrySet()) {
            String studyId = entry.getKey();
            File untransformedFilesPath = studyPaths.get(entry.getKey());
            if (entry.getValue().equals(ExitStatus.SUCCESS) || entry.getValue().equals(ExitStatus.WARNINGS)) {
                transformedFilesPaths.put(studyId, getTransformedFilesPath(untransformedFilesPath));
            }
        }
        return transformedFilesPaths;
    }
}
