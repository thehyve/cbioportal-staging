package org.cbioportal.staging.etl;

import static org.junit.Assert.assertEquals;

import org.cbioportal.staging.app.ScheduledScanner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;
import org.cbioportal.staging.services.AuthorizerServiceImpl;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {org.cbioportal.staging.etl.Extractor.class,
		org.cbioportal.staging.etl.LocalExtractor.class,
		org.cbioportal.staging.etl.Transformer.class,
		org.cbioportal.staging.etl.Loader.class,
		org.cbioportal.staging.etl.Restarter.class,
		org.cbioportal.staging.etl.Authorizer.class,
		org.cbioportal.staging.etl.Validator.class,
		org.cbioportal.staging.etl.EmailServiceMockupImpl.class,
		org.cbioportal.staging.etl.ValidationServiceMockupImpl.class,
		org.cbioportal.staging.etl.LoaderServiceMockupImpl.class,
		org.cbioportal.staging.etl.TransformerServiceMockupImpl.class,
		org.cbioportal.staging.etl.RestarterServiceMockupImpl.class,
		org.cbioportal.staging.etl.ScheduledScannerServiceMockupImpl.class,
		org.cbioportal.staging.services.AuthorizerServiceImpl.class,
		org.cbioportal.staging.etl.ETLProcessRunner.class,
		org.cbioportal.staging.app.ScheduledScanner.class})
@SpringBootTest
@Import(MyTestConfiguration.class)

public class IntegrationTest {
	
	@Autowired
	private Extractor extractor;

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
	
	@Autowired
	private EmailServiceMockupImpl emailService;
	
	@Autowired
	private ValidationServiceMockupImpl validationService;
	
	@Autowired
	private LoaderServiceMockupImpl loaderService;
	
	@Autowired
	private TransformerServiceMockupImpl transformerService;
	
	@Autowired
	private RestarterServiceMockupImpl restarterService;
	
	@Autowired
	private AuthorizerServiceImpl authorizerService;

	@Autowired
	private ETLProcessRunner etlProcessRunner;
	
	@Autowired
	private ScheduledScanner scheduledScanner;
	
	@Before
    public void setUp() throws Exception {
        emailService.reset();
        loaderService.reset();
        validationService.reset();
    }
	
	@Rule
    public TemporaryFolder etlWorkingDir = new TemporaryFolder();

	private void initBasicMockups(String scanLocation, int validationServiceMockExitStatus) {
		//set mockups and input parameters for all services
		ReflectionTestUtils.setField(extractor, "scanLocation", scanLocation);
		ReflectionTestUtils.setField(extractor, "etlWorkingDir", etlWorkingDir.getRoot());
		
		ReflectionTestUtils.setField(transformer, "emailService", emailService);
		ReflectionTestUtils.setField(transformer, "transformerService", transformerService);
		
		ReflectionTestUtils.setField(validator, "emailService", emailService);
		ReflectionTestUtils.setField(validator, "validationService", validationService);
		ReflectionTestUtils.setField(validator, "validationLevel", "ERROR");
		ReflectionTestUtils.setField(validationService, "exitStatus", validationServiceMockExitStatus);
		
		ReflectionTestUtils.setField(loader, "emailService", emailService);
		ReflectionTestUtils.setField(loader, "loaderService", loaderService);
		//ReflectionTestUtils.setField(loaderService, "testFile", "src/test/resources/loader_tests/example.txt");
		
		ReflectionTestUtils.setField(restarter, "restarterService", restarterService);
		ReflectionTestUtils.setField(authorizer, "authorizerService", authorizerService);
		
		ReflectionTestUtils.setField(etlProcessRunner, "transformer", transformer);
		ReflectionTestUtils.setField(etlProcessRunner, "validator", validator);
		ReflectionTestUtils.setField(etlProcessRunner, "loader", loader);
		ReflectionTestUtils.setField(etlProcessRunner, "restarter", restarter);
        ReflectionTestUtils.setField(etlProcessRunner, "authorizer", authorizer);
        ReflectionTestUtils.setField(etlProcessRunner, "etlWorkingDir", etlWorkingDir.getRoot().toString());
		
		ReflectionTestUtils.setField(scheduledScanner, "scanLocation", scanLocation);
        ReflectionTestUtils.setField(scheduledScanner, "etlProcessRunner", etlProcessRunner);
	}
	
	// @Test
	// public void allStudiesLoaded() {
	// 	initBasicMockups("file:src/test/resources/integration", 3);
	// 	ReflectionTestUtils.setField(scheduledScanner, "S3PREFIX", "file:");
	// 	ReflectionTestUtils.setField(etlProcessRunner, "studyAuthorizeCommandPrefix", "null");
		
	// 	scheduledScanner.scan();
		
	// 	//correct emails are sent
	// 	//check that the correct email is sent
	// 	assertEquals(false, emailService.isEmailStudyErrorSent());
	// 	assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
	// 	assertEquals(true, emailService.isEmailValidationReportSent());
	// 	assertEquals(true, emailService.isEmailStudiesLoadedSent());
	// 	assertEquals(false, emailService.isEmailGenericErrorSent());
	// }
	
	// @Test
	// public void subsetOfStudiesLoaded() {
	// 	initBasicMockups("file:src/test/resources/local_integration", 3);
	// 	ReflectionTestUtils.setField(scheduledScanner, "scanExtractFolders", "study1,study2");
	// 	ReflectionTestUtils.setField(etlProcessRunner, "studyAuthorizeCommandPrefix", "null");
		
	// 	scheduledScanner.scan();
		
	// 	//correct emails are sent
	// 	//check that the correct email is sent
	// 	assertEquals(false, emailService.isEmailStudyErrorSent());
	// 	assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
	// 	assertEquals(true, emailService.isEmailValidationReportSent());
	// 	assertEquals(true, emailService.isEmailStudiesLoadedSent());
	// 	assertEquals(false, emailService.isEmailGenericErrorSent());
	// }
	
	
	@Test
	public void noStudiesLoaded() {
		//set mockups and input parameters for all services
		initBasicMockups("file:src/test/resources/integration", 1);
		
		ReflectionTestUtils.setField(scheduledScanner, "S3PREFIX", "file:");
		
		scheduledScanner.scan();
		
		//correct emails are sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(true, emailService.isEmailValidationReportSent());
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(false, emailService.isEmailGenericErrorSent());
	}
	
	@Test
	public void validationError() {
		//set mockups and input parameters for all services
		initBasicMockups("file:src/test/resources/integration", 1);

		ReflectionTestUtils.setField(validationService, "throwError", true);
		
		ReflectionTestUtils.setField(scheduledScanner, "S3PREFIX", "file:");
		
		scheduledScanner.scan();
		
		//check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(false, emailService.isEmailValidationReportSent()); 
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(true, emailService.isEmailGenericErrorSent());
	}
	
	@Test
	public void noScanLocation() {
		//set mockups and input parameters for all services
		initBasicMockups("file:src/notfound", 3);

		ReflectionTestUtils.setField(scheduledScanner, "S3PREFIX", "file:");
		
		scheduledScanner.scan();
		
		//no emails sent, since no yaml file is found in the scan location
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(false, emailService.isEmailValidationReportSent()); 
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(false, emailService.isEmailGenericErrorSent());
	}
	
	// @Test
	// public void studiesAreAuthorized() {
	// 	initBasicMockups("file:src/test/resources/integration", 3);
	// 	ReflectionTestUtils.setField(scheduledScanner, "S3PREFIX", "file:");
	// 	ReflectionTestUtils.setField(etlProcessRunner, "studyAuthorizeCommandPrefix", "echo");
	// 	ReflectionTestUtils.setField(authorizerService, "studyAuthorizeCommandPrefix", "echo");
		
	// 	scheduledScanner.scan();
		
	// 	//correct emails are sent
	// 	//check that the correct email is sent
	// 	assertEquals(false, emailService.isEmailStudyErrorSent());
	// 	assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
	// 	assertEquals(true, emailService.isEmailValidationReportSent());
	// 	assertEquals(true, emailService.isEmailStudiesLoadedSent());
	// 	assertEquals(false, emailService.isEmailGenericErrorSent());
	// }
	
	// @Test
	// public void studiesAreNotAuthorized() {
	// 	initBasicMockups("file:src/test/resources/integration", 3);
	// 	ReflectionTestUtils.setField(scheduledScanner, "S3PREFIX", "file:");
	// 	ReflectionTestUtils.setField(etlProcessRunner, "studyAuthorizeCommandPrefix", "ls");
	// 	ReflectionTestUtils.setField(authorizerService, "studyAuthorizeCommandPrefix", "ls");
		
	// 	scheduledScanner.scan();
		
	// 	//correct emails are sent
	// 	//check that the correct email is sent
	// 	assertEquals(false, emailService.isEmailStudyErrorSent());
	// 	assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
	// 	assertEquals(true, emailService.isEmailValidationReportSent());
	// 	assertEquals(true, emailService.isEmailStudiesLoadedSent());
	// 	assertEquals(true, emailService.isEmailGenericErrorSent());
	// }
}
