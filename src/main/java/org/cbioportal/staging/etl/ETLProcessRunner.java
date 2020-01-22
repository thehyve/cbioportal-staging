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
import java.net.BindException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.app.ScheduledScanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * Main ETL process.
 * 
 * @author pieter
 *
 */
@Component
public class ETLProcessRunner {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);

	@Autowired
	private Extractor extractor;
	
	@Autowired
	private LocalExtractor localExtractor;

	@Autowired
	private Transformer transformer;

	@Autowired
	private Validator validator;

	@Autowired
	private Loader loader;

	@Autowired
	private Restarter restarter;
	
	@Autowired
	private Authorizer authorizer;
	
	@Value("${study.authorize.command_prefix:null}")
    private String studyAuthorizeCommandPrefix;
    
    @Value("${central.share.location}")
    private String centralShareLocation;

    @Value("${etl.working.dir:false}")
	private String etlWorkingDir;

	/**
	 * Runs all the steps of the ETL process.
	 * 
	 * @param indexFile: index YAML file containing the names of the files to be "ETLed".
	 * @throws Exception 
	 */
	public void run(Resource indexFile) throws Exception {
		try  {
			startProcess();
            //E (Extract) step:
            Map<String, File> studyPaths = new HashMap<String, File>();
            if (etlWorkingDir.equals("false")) {
                throw new Exception("When providing a yaml file instead of a directory, you need to define a working directory.");
            } else {
                studyPaths = extractor.run(indexFile);
            }
			String date = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
			//Execute Transforming, Validating and Loading steps:
			runCommon(date, studyPaths);
		}
		finally
		{
			//end process / release lock:
			endProcess();
		}
	}
	
	/**
	 * Runs all the steps of the ETL process.
	 * 
	 * @param directories: list of strings with the directory names inside the scanLocation folder.
	 * @throws Exception 
	 */
	public void run(ArrayList<File> directories) throws Exception {
		try  {
            startProcess();
            //E (Extract) step:
            String date = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
            Map<String, File> studyPaths = new HashMap<String, File>(localExtractor.extractInWorkingDir(directories, date));
            if (etlWorkingDir.equals("false")) {
                studyPaths = localExtractor.extractWithoutWorkingDir(directories);
            }
			
			//Execute Transforming, Validating and Loading steps:
			runCommon(date, studyPaths);
		}
		finally
		{
			//end process / release lock:
			endProcess();
		}
	}
	
	private void runCommon(String date, Map<String, File> studyPaths) throws Exception {
        Map<String, String> logPaths = new HashMap<String, String>();
		boolean loadSuccessful = false;
		//T (Transform) step:
		Map<String, File> transformedStudiesPaths = transformer.transform(date, studyPaths, "command", logPaths);
        //V (Validate) step:
        if (transformedStudiesPaths.keySet().size() > 0) {
            Map<String, File> validatedStudies = validator.validate(date, transformedStudiesPaths, logPaths);
            //L (Load) step:
            if (validatedStudies.size() > 0) {
                loadSuccessful = loader.load(date, validatedStudies, logPaths);
                if (loadSuccessful) {
                    restarter.restart();
                    if (!studyAuthorizeCommandPrefix.equals("null")) {
                        authorizer.authorizeStudies(validatedStudies.keySet());
                    }
                }
            }
        }
	}

	/**
	 * Socket configuration and respective synchronized start method that ensures only one
	 * ETL process runs at a time (also if started in another process - this is guaranteed by
	 * implementation of a race condition on a single socket). This is important because
	 * cBioPortal currently only supports loading one study at a time, since this data loading
	 * operation is not thread safe in cBioPortal itself.
	 */
	@Value("${etl.lock.port:9999}")
	private Integer PORT;
	private static ServerSocket socket;

	private synchronized void startProcess() {
		try {
			//"Lock implementation". Try to reserve a socket:
			logger.info("Reserving socket on port " + PORT + " to avoid parallel ETL processes (not supported)");

			socket = new ServerSocket(PORT,0,InetAddress.getByAddress(new byte[] {127,0,0,1}));
		}
		catch (BindException e) {
			logger.error("Another ETL process is already running.", e);
			throw new RuntimeException("Another ETL process is already running", e);
		}
		catch (IOException e) {
			logger.error("Unexpected error.", e);
			e.printStackTrace();
			throw new RuntimeException("Unexpected error.", e);
		}
	}

	private void endProcess() {
		try {
			logger.info("Closing socket / releasing lock" );
			socket.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
