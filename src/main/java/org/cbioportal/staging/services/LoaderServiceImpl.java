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
package org.cbioportal.staging.services;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.cbioportal.staging.etl.Loader;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.LoaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoaderServiceImpl implements LoaderService {
	private static final Logger logger = LoggerFactory.getLogger(Loader.class);

	@Value("${cbioportal.mode}")
	private String cbioportalMode;
	
	@Value("${cbioportal.docker.image}")
	private String cbioportalDockerImage;
	
	@Value("${cbioportal.docker.network}")
	private String cbioportalDockerNetwork;
	
	@Value("${portal.home:.}")
	private String portalHome;
	
	@Value("${etl.working.dir:java.io.tmpdir}")
	private String etlWorkingDir;
	
	@Autowired
	ValidationService validationService;
	
	@Override
	public String load(String study, File studyPath, int id, String centralShareLocation) throws ConfigurationException, IOException, Exception {
		ProcessBuilder loaderCmd;
		if (cbioportalMode.equals("local")) {
			loaderCmd = new ProcessBuilder("./cbioportalImporter.py", "-s", studyPath.toString());
			loaderCmd.directory(new File(portalHome+"/core/src/main/scripts/importer"));
		} else if (cbioportalMode.equals("docker")) {
			if (!cbioportalDockerImage.equals("") && !cbioportalDockerNetwork.equals("")) {
				loaderCmd = new ProcessBuilder ("docker", "run", "-i", "--rm", "--net", cbioportalDockerNetwork,
						"-v", studyPath.toString()+":/study:ro", cbioportalDockerImage,
						"cbioportalImporter.py", "-s", "/study");
			} else {
				throw new ConfigurationException("cbioportal.mode is 'docker', but no Docker image or network has been specified in the application.properties.");
			}
		} else {
			throw new ConfigurationException("cbioportal.mode is not 'local' or 'docker'. Value encountered: "+cbioportalMode+
					". Please change the mode in the application.properties.");
		}

		//Apply loader command
		logger.info("Executing command: "+String.join(" ", loaderCmd.command()));
		String logTimeStamp = new SimpleDateFormat("yyyy_MM_dd_HH.mm.ss").format(new Date());
		String logName = study+"_loading_log_"+logTimeStamp+".log";
		File logFile = new File(etlWorkingDir+"/"+id+logName);
		loaderCmd.redirectErrorStream(true);
		loaderCmd.redirectOutput(Redirect.appendTo(logFile));
		try {
			Process loadProcess = loaderCmd.start();
			loadProcess.waitFor(); //Wait until loading is finished
			validationService.copyToResource(logFile, centralShareLocation);
			return logName;
		} catch (IOException e) {
			throw new IOException(e);
		} catch (Exception e) {
			byte[] encoded = Files.readAllBytes(Paths.get(logFile.getAbsolutePath()));
			String message = new String(encoded, "UTF-8");
			throw new LoaderException("There was an error when executing the loader command. The output of the log file is: "+message, e);
		}
	}
}
