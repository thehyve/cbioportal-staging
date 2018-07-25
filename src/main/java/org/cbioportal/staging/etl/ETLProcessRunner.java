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
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.exceptions.ConfigurationException;
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
	private Publisher publisher;
	
	@Value("${study.publish.script}")
	private String studyPublishScript;

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
			Pair<Integer, List<String>> idAndStudies = extractor.run(indexFile);
			//Execute Transforming, Validating and Loading steps:
			runCommon(idAndStudies);
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
			Pair<Integer, List<String>> idAndStudies = localExtractor.run(directories);
			logger.debug("ID AND STUDIES: "+idAndStudies.toString());
			//Execute Transforming, Validating and Loading steps:
			runCommon(idAndStudies);
		}
		finally
		{
			//end process / release lock:
			endProcess();
		}
	}
	
	private void runCommon(Pair<Integer, List<String>> idAndStudies) throws Exception {
		boolean loadSuccessful = false;
		try  {
			//T (Transform) step:
			transformer.transform(idAndStudies.getKey(), idAndStudies.getValue(), "command");
			//V (Validate) step:
			ArrayList<String> validatedStudies = validator.validate(idAndStudies.getKey(), idAndStudies.getValue());
			//L (Load) step:
			if (validatedStudies.size() > 0) {
				loadSuccessful = loader.load(idAndStudies.getKey(), validatedStudies);
				
				if (!studyPublishScript.isEmpty()) {
					publisher.publishStudies(validatedStudies);
				}
			}
		}
		finally
		{
			//restart cBioPortal:
			if (loadSuccessful) {
				restarter.restart();
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
