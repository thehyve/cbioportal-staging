/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.services;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ValidatorException;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.annotation.Value;

public class ValidationServiceImpl implements ValidationService {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);

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
	public File validate(String study, String studyPath, String reportPath) throws ValidatorException, ConfigurationException {
		try {
			
			TemporaryFolder portalInfoFolder = new TemporaryFolder();
			String logTimeStamp = new SimpleDateFormat("yyyy_MM_dd_HH.mm.ss").format(new Date());
			File logFile = new File(centralShareLocation.toString()+"/"+study+"_validation_log_"+logTimeStamp+".log");
			
			ProcessBuilder validationCmd;
			ProcessBuilder portalInfoCmd;
			if (cbioportalMode.equals("local")) {
				validationCmd = new ProcessBuilder("./validateData.py", "-s", studyPath.toString(), "-p", portalInfoFolder.toString(), "-html", reportPath, "-v");
				portalInfoCmd = new ProcessBuilder("./dumpPortalInfo.pl", portalInfoFolder.toString());
				portalInfoCmd.directory(new File(portalHome+"/core/src/main/scripts"));
				validationCmd.directory(new File(portalHome+"/core/src/main/scripts/importer"));
			} else if (cbioportalMode.equals("docker")) {
				if (!cbioportalDockerImage.equals("") && !cbioportalDockerNetwork.equals("")) {
					validationCmd = new ProcessBuilder ("docker", "run", "-i", "--rm",
							"-v", studyPath.toString()+":/study:ro", "-v", reportPath+"/:/outreport.html",
							"-v", portalInfoFolder.toString()+ ":/portalinfo:ro", cbioportalDockerImage,
							"validateData.py", "-p", "/portalinfo", "-s", "/study", "--html=/outreport.html");
					portalInfoCmd = new ProcessBuilder("docker", "run", "--rm", "--net", cbioportalDockerNetwork,
							"-v", portalInfoFolder.toString()+":/portalinfo", "-w", "/cbioportal/core/src/main/scripts",
							cbioportalDockerImage, "./dumpPortalInfo.pl", "/portalinfo");
				} else {
					throw new ConfigurationException("cbioportal.mode is 'docker', but no Docker image or network has been specified in the application.properties.");
				}
			} else {
				throw new ConfigurationException("cbioportal.mode is not 'local' or 'docker'. Value encountered: "+cbioportalMode+
						". Please change the mode in the application.properties.");
			}
			
			//dump portal data
			logger.info("Dumping portalInfo...");
			Process pInfo = portalInfoCmd.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(pInfo.getErrorStream()));
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				logger.info(line);
			}
			pInfo.waitFor();
			logger.info("Dump portalInfo finished. Continuing validation...");
		
			//Apply validation command
			validationCmd.redirectErrorStream(true);
			validationCmd.redirectOutput(Redirect.appendTo(logFile));
			return logFile;
		}
		catch (InvalidPropertyException e) {
			throw e;
		}
		catch (Exception e) {
			throw new ValidatorException("Error during validation execution. ", e);
		}
		
	}

}
