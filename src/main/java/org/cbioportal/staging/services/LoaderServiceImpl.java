/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.services;

import java.io.File;
import java.io.IOException;
import java.lang.ProcessBuilder.Redirect;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class LoaderServiceImpl implements LoaderService {

	@Value("${cbioportal.mode}")
	private String cbioportalMode;
	
	@Value("${cbioportal.docker.image}")
	private String cbioportalDockerImage;
	
	@Value("${cbioportal.docker.network}")
	private String cbioportalDockerNetwork;
	
	@Value("${portal.home}")
	private String portalHome;
	
	@Value("${central.share.location}")
	private File centralShareLocation;
	
	@Override
	public File load(String study, File studyPath) throws IOException, InterruptedException, ConfigurationException {
		ProcessBuilder loaderCmd;
		if (cbioportalMode.equals("local")) {
			loaderCmd = new ProcessBuilder("./cbioportalImporter.py", "-s", studyPath.toString());
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
		loaderCmd.directory(new File(portalHome+"/core/src/main/scripts/importer"));
		String logTimeStamp = new SimpleDateFormat("yyyy_MM_dd_HH.mm.ss").format(new Date());
		File logFile = new File(centralShareLocation.toString()+"/"+study+"_loading_log_"+logTimeStamp+".log");
		loaderCmd.redirectErrorStream(true);
		loaderCmd.redirectOutput(Redirect.appendTo(logFile));
		Process loadProcess = loaderCmd.start();
		loadProcess.waitFor(); //Wait until loading is finished
		return logFile;
	}
}
