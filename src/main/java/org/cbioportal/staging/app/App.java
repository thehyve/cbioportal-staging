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
