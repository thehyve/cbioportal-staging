package org.cbioportal.staging.services.resource;

import org.cbioportal.staging.etl.ETLProcessRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class ResourceFinder implements IResourceFinder {

    @Value("${execution.stage:ALL}")
    private ETLProcessRunner.Stage executeStage;

    @Value("${scan.location}")
    private Resource scanLocation;

    @Value("${etl.working.dir:}")
    private Resource etlWorkingDir;

    @Value("${transformation.directory:}")
    private Resource transformationDir;

    @Value("${etl.dir.format:timestamp/study_id}")
    private String dirFormat;

    @Override
    public Resource getInputDirectory() {
        if (executeStage == ETLProcessRunner.Stage.TRANSFORM) {
            return etlWorkingDir;
        }

        if (executeStage == ETLProcessRunner.Stage.VALIDATE ||
            executeStage == ETLProcessRunner.Stage.LOAD) {
            //If transformationDirectory is not set, the transformed files will be saved in the working directory,
            //in a folder "staging" inside each study-specific folder.
            if (transformationDir != null) {
                return transformationDir;
            }
            return etlWorkingDir;
        }
        return scanLocation; //For ALL and EXTRACT stages
    }

}
