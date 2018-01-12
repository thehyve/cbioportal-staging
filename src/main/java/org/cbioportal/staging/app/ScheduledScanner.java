package org.cbioportal.staging.app;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Main app class to run the staging and loading process as a background service
 *
 */

@Component
public class ScheduledScanner 
{
    private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);
    private static final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");
    
    @Value("${scan.location}")
    private String scanLocation;
    
    @Scheduled(cron = "${scan.cron}")
    public void scan() throws IOException {
        logger.info("Fixed Rate Task :: Execution Time - {}", dateTimeFormatter.format(LocalDateTime.now()) );
        
        if (scanLocation.startsWith("file:")) {
            Resource scanFileLocation = new FileSystemResource (scanLocation.split("file://")[1]);
            logger.info("Scanning file location: " + scanFileLocation.getURL().getPath() );
            logger.info("Scan location protocol: " + scanFileLocation.getURL().getProtocol() );
        } else if (scanLocation.startsWith("s3:")) {
            logger.info("Scanning s3 location: " + scanLocation);
        }
        
    }

}
