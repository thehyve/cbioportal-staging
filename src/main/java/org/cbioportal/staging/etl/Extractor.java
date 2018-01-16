package org.cbioportal.staging.etl;

import java.io.File;

import org.cbioportal.staging.app.ScheduledScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
class Extractor {
    private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);

    @Value("${etl.working.dir:file:///tmp}")
    private File etlWorkingDir;
    
    void run(Resource indexFile) {
        logger.info("Extract step: downloading files to " + etlWorkingDir.getAbsolutePath());
        //Parse the indexFile and download all referred files to the working directory.
        //TODO
    }
}
