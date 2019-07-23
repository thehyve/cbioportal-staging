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
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.FileInputStream;
import java.lang.ProcessBuilder.Redirect;

import org.cbioportal.staging.etl.Validator;
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
	private static final Logger logger = LoggerFactory.getLogger(Validator.class);

	@Value("${cbioportal.mode}")
	private String cbioportalMode;
	
	@Value("${cbioportal.docker.image}")
	private String cbioportalDockerImage;
	
	@Value("${cbioportal.docker.network}")
    private String cbioportalDockerNetwork;
    
    @Value("${cbioportal.docker.properties}")
    private String cbioportalDockerProperties;
	
	@Value("${portal.source:.}")
	private String portalSource;
	
	@Value("${etl.working.dir:false}")
	private String etlWorkingDir;
	
	@Autowired
	private ResourcePatternResolver resourcePatternResolver;
	
	@Override
	public int validate(String study, String studyPath, String reportPath, File logFile, int id) throws ValidatorException, ConfigurationException, Exception {
		try {
            File portalInfoFolder = new File(etlWorkingDir+"/"+id+"/portalInfo");
            if (etlWorkingDir.equals("false")){
                portalInfoFolder = new File(studyPath+"/portalInfo");
            }
			
			ProcessBuilder validationCmd;
			ProcessBuilder portalInfoCmd;
			if (cbioportalMode.equals("local")) {
				validationCmd = new ProcessBuilder("./validateData.py", "-s", studyPath.toString(), "-p", portalInfoFolder.toString(), "-html", reportPath, "-v");
				portalInfoCmd = new ProcessBuilder("./dumpPortalInfo.pl", portalInfoFolder.toString());
				portalInfoCmd.directory(new File(portalSource+"/core/src/main/scripts"));
				validationCmd.directory(new File(portalSource+"/core/src/main/scripts/importer"));
			} else if (cbioportalMode.equals("docker")) {
				if (!cbioportalDockerImage.equals("") && !cbioportalDockerNetwork.equals("")) {
					//make sure report file exists first, otherwise docker will map it as a folder:
					File f = new File(reportPath);
					f.getParentFile().mkdirs(); 
					f.createNewFile();
					//docker command:
					validationCmd = new ProcessBuilder ("docker", "run", "-i", "--rm",
							"-v", studyPath.toString()+":/study:ro", "-v", reportPath+":/outreport.html",
                            "-v", portalInfoFolder.toString()+ ":/portalinfo:ro", 
                            "-v", cbioportalDockerProperties+":/cbioportal/portal.properties:ro", cbioportalDockerImage,
							"validateData.py", "-p", "/portalinfo", "-s", "/study", "--html=/outreport.html");
					portalInfoCmd = new ProcessBuilder("docker", "run", "--rm", "--net", cbioportalDockerNetwork,
                            "-v", portalInfoFolder.toString()+":/portalinfo",
                            "-v", cbioportalDockerProperties+":/cbioportal/portal.properties:ro",
                            "-w", "/cbioportal/core/src/main/scripts",
							cbioportalDockerImage, "./dumpPortalInfo.pl", "/portalinfo");
				} else {
					throw new ConfigurationException("cbioportal.mode is 'docker', but no Docker image or network has been specified in the application.properties.");
				}
			} else {
				throw new ConfigurationException("cbioportal.mode is not 'local' or 'docker'. Value encountered: "+cbioportalMode+
						". Please change the mode in the application.properties.");
			}
			
			logger.info("Dumping portalInfo...");
			logger.info("Executing command: "+String.join(" ", portalInfoCmd.command()));
			Process pInfo = portalInfoCmd.start();
			BufferedReader reader = new BufferedReader(new InputStreamReader(pInfo.getErrorStream()));
			String line = null;
			while ((line = reader.readLine()) != null)
			{
				logger.warn(line);//TODO warn, since this is error stream output ^ ?
			}
			pInfo.waitFor();
			if (pInfo.exitValue() != 0) {
				throw new RuntimeException("Dump portalInfo step failed");
			}

			logger.info("Dump portalInfo finished. Continuing validation...");
		
			//Apply validation command
			logger.info("Starting validation. Report will be stored in: " + reportPath);
			logger.info("Executing command: "+String.join(" ", validationCmd.command()));
			validationCmd.redirectErrorStream(true);
			validationCmd.redirectOutput(Redirect.appendTo(logFile));
			Process validateProcess = validationCmd.start();
			validateProcess.waitFor(); //Wait until validation is finished
			int exitValue = validateProcess.exitValue();
			
			return exitValue;
		}
		catch (InvalidPropertyException e) {
			throw new ValidatorException("Error during validation execution: property not valid, check the validation command. ", e);
		}
		catch (ConfigurationException e) {
			throw new ConfigurationException(e.toString(), e);
		}
		catch (IOException e) {
			if (cbioportalMode.equals("docker")) {
				throw new ConfigurationException("Error during validation execution: check if Docker is installed, check whether the current"
						+ " user has sufficient rights to run Docker, and if the configured working directory is accessible to Docker.", e);
			} else {
				throw new ConfigurationException("Check if portal source is correctly set in application.properties. Configured portal source is: "+portalSource, e);
			}
		}
		catch (Exception e) {
			throw new Exception("Error during validation execution. ", e);
		}
	}
	
	public void copyToResource(File filePath, String resourceOut) throws IOException {
		String resourcePath = resourceOut+"/"+filePath.getName();
		Resource resource;
		if (resourcePath.startsWith("file:")) {
			resource = new FileSystemResource(resourcePath.replace("file:", ""));
		}
		else {
			resource = this.resourcePatternResolver.getResource(resourcePath);
        }
		WritableResource writableResource = (WritableResource) resource;
        try (OutputStream outputStream = writableResource.getOutputStream(); 
            InputStream inputStream = new FileInputStream(filePath)) {
                IOUtils.copy(inputStream, outputStream);
		}
    }

}
