package org.cbioportal.staging.etl;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.ProcessBuilder.Redirect;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cbioportal.staging.app.EmailServiceImpl;
import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.exceptions.LoaderException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

@Component
public class Loader {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);
	
	@Autowired
	EmailServiceImpl emailService;
	
	@Value("${etl.working.dir:file:///tmp}")
	private File etlWorkingDir;
	
	@Value("${cbioportal.mode}")
	private String cbioportalMode;
	
	@Value("${cbioportal.docker.image}")
	private String cbioportalDockerImage;
	
	@Value("${cbioportal.docker.network}")
	private String cbioportalDockerNetwork;
	
	@Value("${central.share.location}")
	private File centralShareLocation;
	
	void load(Integer id, List<String> studies) {
		try {
			if (!centralShareLocation.exists()) {
				throw new Exception("The central share location directory specified in application.properties do not exist: "+centralShareLocation.toString()+
						". Stopping process...");
			} else {
				Map<String, String> statusStudies = new HashMap<String, String>();
				//Get studies from appropriate staging folder
				File originPath = new File(etlWorkingDir.toPath()+"/"+id+"/staging");
				for (String study : studies) {
					try {
						logger.info("Starting loading of study "+study);
						File studyPath = new File(originPath+"/"+study);
						String portalHome = System.getenv("PORTAL_HOME");
						ProcessBuilder loaderCmd;
						if (cbioportalMode.equals("local")) {
							loaderCmd = new ProcessBuilder("./cbioportalImporter.py", "-s", studyPath.toString());
						} else if (cbioportalMode.equals("docker")) {
							if (!cbioportalDockerImage.equals("") && !cbioportalDockerNetwork.equals("")) {
								loaderCmd = new ProcessBuilder ("docker", "run", "-i", "--rm", "--net", cbioportalDockerNetwork,
										"-v", studyPath.toString()+":/study:ro", cbioportalDockerImage,
										"cbioportalImporter.py", "-s", "/study");
							} else {
								throw new Exception("cbioportal.mode is 'docker', but no Docker image or network has been specified in the application.properties.");
							}
						} else {
							throw new Exception("cbioportal.mode is not 'local' or 'docker'. Value encountered: "+cbioportalMode+
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
						logger.info("Loading of study "+study+" finished.");
						statusStudies.put(logFile.getAbsolutePath(), "<b><font style=\"color: #04B404\">SUCCESSFULLY LOADED</font></b>");
					} catch (LoaderException e) {
						//tell about error, continue with next study
						logger.error(e.getMessage()+". The app will continue with the next study.");
						statusStudies.put(study, "<b><font style=\"color: #FF0000\">ERRORS</font></b>");
						e.printStackTrace();
					}
				}
				emailService.emailStudiesLoaded(statusStudies);
			}
		} catch (Exception e) {
			logger.error("An error not expected occurred. Stopping process...");
			try {
				emailService.emailGenericError("An error not expected occurred. Stopping process...", e);
			} catch (TemplateNotFoundException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (MalformedTemplateNameException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (ParseException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			} catch (TemplateException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			e.printStackTrace();
			System.exit(-1); //Stop app
		}
	}
}
