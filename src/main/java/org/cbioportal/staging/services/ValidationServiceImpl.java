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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.ProcessBuilder.Redirect;
import java.nio.file.Paths;

import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ValidatorException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.InvalidPropertyException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
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
	private Resource centralShareLocation;
	
	@Autowired
	private ResourcePatternResolver resourcePatternResolver;
	
	@Override
	public int validate(String study, String studyPath, String reportPath, File logFile) throws ValidatorException, ConfigurationException, Exception {
		try {
			File cslPath = Paths.get(centralShareLocation.getURI()).toFile();
			File portalInfoFolder = new File(cslPath+"/portalInfo");
			
			ProcessBuilder validationCmd;
			ProcessBuilder portalInfoCmd;
			if (cbioportalMode.equals("local")) {
				validationCmd = new ProcessBuilder("./validateData.py", "-s", studyPath.toString(), "-p", portalInfoFolder.toString(), "-html", reportPath, "-v");
				portalInfoCmd = new ProcessBuilder("./dumpPortalInfo.pl", portalInfoFolder.toString());
				portalInfoCmd.directory(new File(portalHome+"/core/src/main/scripts"));
				validationCmd.directory(new File(portalHome+"/core/src/main/scripts/importer"));
			} else if (cbioportalMode.equals("docker")) {
				if (!cbioportalDockerImage.equals("") && !cbioportalDockerNetwork.equals("")) {
					//make sure report file exists first, otherwise docker will map it as a folder:
					File f = new File(reportPath);
					f.getParentFile().mkdirs(); 
					f.createNewFile();
					//docker command:
					validationCmd = new ProcessBuilder ("docker", "run", "-i", "--rm",
							"-v", studyPath.toString()+":/study:ro", "-v", reportPath+":/outreport.html",
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
			
			//dump portal data (first delete previous portalInfo folder if exists)
			if (portalInfoFolder.exists()) {
				String[]entries = portalInfoFolder.list();
				for(String s: entries){
				    File currentFile = new File(portalInfoFolder.getPath(),s);
				    currentFile.delete();
				}
				portalInfoFolder.delete();
			}
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
			logger.info("Starting validation. Report will be stored in: " + reportPath);
			validationCmd.redirectErrorStream(true);
			validationCmd.redirectOutput(Redirect.appendTo(logFile));
			Process validateProcess = validationCmd.start();
			validateProcess.waitFor(); //Wait until validation is finished
			int exitValue = validateProcess.exitValue();
			
			//Delete portalInfo
			String[]entries = portalInfoFolder.list();
			for(String s: entries){
			    File currentFile = new File(portalInfoFolder.getPath(),s);
			    currentFile.delete();
			}
			portalInfoFolder.delete();
			
			return exitValue;
		}
		catch (InvalidPropertyException e) {
			throw new ValidatorException("Error during validation execution: property not valid, check the validation command. ", e);
		}
		catch (ConfigurationException e) {
			throw new ConfigurationException(e.toString(), e);
		}
		catch (Exception e) {
			throw new Exception("Error during validation execution. ");
		}
	}
	
	public void copyToResource(String fileName, String filePath, Resource resourceOut) throws IOException {
		String resourcePath = resourceOut.getURI()+fileName;
		//logger.info("COMMAND PATH: "+resourcePath);
		Resource resource;
		if (resourcePath.startsWith("file:")) {
			resource = new FileSystemResource(resourcePath.replace("file:", ""));
		}
		else {
			resource = this.resourcePatternResolver.getResource(resourcePath);
		}
		WritableResource writableResource = (WritableResource) resource;
		OutputStream outputStream = writableResource.getOutputStream();
		BufferedReader br = new BufferedReader(new FileReader(filePath));
		String line = null;
		while ((line = br.readLine()) != null)  
		{
			outputStream.write(line.getBytes());
		}
		br.close();
	}

}
