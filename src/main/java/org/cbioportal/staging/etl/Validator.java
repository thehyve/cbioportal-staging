package org.cbioportal.staging.etl;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ProcessBuilder.Redirect;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.cbioportal.staging.app.EmailService;
import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.exceptions.ValidatorException;
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
public class Validator {
	private static final Logger logger = LoggerFactory.getLogger(ScheduledScanner.class);
	
	@Autowired
	EmailService emailService;
	
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
	
	@Value("${validation.level:Errors}")
	private String validationLevel;
	
	ArrayList<String> validate(Integer id, List<String> studies) {
		try {
			ArrayList<String> studiesPassed = new ArrayList<String>();
			if (!centralShareLocation.exists()) {
				throw new Exception("The central share location directory specified in application.properties do not exist: "+centralShareLocation.toString()+
						". Stopping process...");
			} else {
				try {
					//Get studies from appropriate staging folder
					File originPath = new File(etlWorkingDir.toPath()+"/"+id+"/staging");
					Map<Pair<String,String>,List<Integer>> validatedStudies = new HashMap<Pair<String,String>,List<Integer>>();
					for (String study : studies) {
						logger.info("Starting validation of study "+study);
						File studyPath = new File(originPath+"/"+study);
						String portalHome = System.getenv("PORTAL_HOME");
						String reportTimeStamp = new SimpleDateFormat("yyyy_MM_dd_HH.mm.ss").format(new Date());
						String reportName = study+"_validation_report_"+reportTimeStamp+".html";
						String reportPath = centralShareLocation.toString()+"/"+reportName;
						File portalInfoPath = new File(etlWorkingDir.toPath()+"/portalInfo/");
						ProcessBuilder validationCmd;
						ProcessBuilder portalInfoCmd;
						if (cbioportalMode.equals("local")) {
							validationCmd = new ProcessBuilder("./validateData.py", "-s", studyPath.toString(), "-p", portalInfoPath.toString(), "-html", reportPath, "-v");
							portalInfoCmd = new ProcessBuilder("./dumpPortalInfo.pl", portalInfoPath.toString());
						} else if (cbioportalMode.equals("docker")) {
							if (!cbioportalDockerImage.equals("") && !cbioportalDockerNetwork.equals("")) {
								validationCmd = new ProcessBuilder ("docker", "run", "-i", "--rm",
										"-v", studyPath.toString()+":/study:ro", "-v", centralShareLocation.toString()+"/:/outdir",
										"-v", etlWorkingDir.toPath()+"/portalinfo:/portalinfo:ro", cbioportalDockerImage,
										"validateData.py", "-p", "/portalinfo", "-s", "/study", "--html=/outdir/"+reportName);
								portalInfoCmd = new ProcessBuilder("docker", "run", "--rm", "--net", cbioportalDockerNetwork,
										"-v", portalInfoPath.toString()+":/portalinfo", "-w", "/cbioportal/core/src/main/scripts",
										cbioportalDockerImage, "./dumpPortalInfo.pl", "/portalinfo");
							} else {
								throw new Exception("cbioportal.mode is 'docker', but no Docker image or network has been specified in the application.properties.");
							}
						} else {
							throw new Exception("cbioportal.mode is not 'local' or 'docker'. Value encountered: "+cbioportalMode+
									". Please change the mode in the application.properties.");
						}
						
						//Check if portalInfo exists, otherwise create it
						if (!portalInfoPath.exists()) {
							logger.info("portalInfo does not exist. Creating it...");
							portalInfoCmd.directory(new File(portalHome+"/core/src/main/scripts"));
							Process pInfo = portalInfoCmd.start();
							BufferedReader reader = new BufferedReader(new InputStreamReader(pInfo.getErrorStream()));
							String line = null;
							while ((line = reader.readLine()) != null)
							{
								logger.info(line);
							}
							pInfo.waitFor();
							logger.info("Dump portalInfo finished. Continuing validation...");
						}
						
						//Apply validation command
						validationCmd.directory(new File(portalHome+"/core/src/main/scripts/importer"));
						String logTimeStamp = new SimpleDateFormat("yyyy_MM_dd_HH.mm.ss").format(new Date());
						File logFile = new File(centralShareLocation.toString()+"/"+study+"_validation_log_"+logTimeStamp+".log");
						validationCmd.redirectErrorStream(true);
						validationCmd.redirectOutput(Redirect.appendTo(logFile));
						Process validateProcess = validationCmd.start();
						validateProcess.waitFor(); //Wait until validation is finished
						BufferedReader validationReader = new BufferedReader(new FileReader(logFile));
						String valReadLine = null;
						Map<String, Integer> studyCounter = new HashMap<String, Integer>();
						studyCounter.put("Errors", 0);
						studyCounter.put("Warnings", 0);
						while ((valReadLine = validationReader.readLine()) != null) {
							if (valReadLine.indexOf("WARNING") != -1) {
								studyCounter.put("Warnings", studyCounter.get("Warnings")+1);
								}
							if (valReadLine.indexOf("ERROR") != -1) {
								studyCounter.put("Errors", studyCounter.get("Errors")+1);
								}
						}
						validationReader.close();
						if (studyCounter.get(validationLevel) == 0) {
							studiesPassed.add(study);
						}
						List<Integer> errorsAndWarnings = new ArrayList<Integer>();
						errorsAndWarnings.add(studyCounter.get("Warnings"));
						errorsAndWarnings.add(studyCounter.get("Errors"));
						Pair<String, String> studyData = Pair.of(reportPath, logFile.getAbsolutePath());
						validatedStudies.put(studyData, errorsAndWarnings);
						logger.info("Validation of study "+study+" finished. Errors: "+studyCounter.get("Errors")+", Warnings: "+studyCounter.get("Warnings"));
					}
					emailService.emailValidationReport(validatedStudies, validationLevel);
					return studiesPassed;
					
				} catch (ValidatorException e) {
					//tell about error, continue with next study
					logger.error(e.getMessage()+". The app will continue with the next study.");
					emailService.emailGenericError(e.getMessage()+". The app will continue with the next study.", e);
					e.printStackTrace();
				} catch (Exception e) {
					logger.error("An error not expected occurred. Stopping process...");
					emailService.emailGenericError("An error not expected occurred. Stopping process...", e);
					e.printStackTrace();
					System.exit(-1); //Stop app
				}
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
		return null;
	}
}
