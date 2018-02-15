/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.app;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Main app class to run the staging and loading process as a background service
 *
 */

@SpringBootApplication(scanBasePackages={"org.cbioportal.staging"})
@EnableScheduling
public class App 
{
    private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);

    public static void main( String[] args )
    {
        logger.info("Starting cBioPortal staging app..." );
        SpringApplication.run(App.class, args);
    }
}
