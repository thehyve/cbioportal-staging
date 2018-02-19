package org.cbioportal.staging.etl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

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

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {org.cbioportal.staging.etl.Extractor.class,
		org.cbioportal.staging.etl.Transformer.class,
		org.cbioportal.staging.etl.Loader.class,
		org.cbioportal.staging.etl.Restarter.class,
		org.cbioportal.staging.etl.Validator.class,
		org.cbioportal.staging.etl.EmailServiceMockupImpl.class,
		org.cbioportal.staging.etl.ValidationServiceMockupImpl.class,
		org.cbioportal.staging.etl.LoaderServiceMockupImpl.class,
		org.cbioportal.staging.etl.TransformerServiceMockupImpl.class,
		org.cbioportal.staging.etl.RestarterServiceMockupImpl.class,
		org.cbioportal.staging.etl.ScheduledScannerServiceMockupImpl.class,
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
	private ETLProcessRunner etlProcessRunner;
	
	@Autowired
	private ScheduledScanner scheduledScanner;
	
	@Before
    public void setUp() throws Exception {
        emailService.reset();
        loaderService.reset();
    }
	
	@Rule
    public TemporaryFolder etlWorkingDir = new TemporaryFolder();

	@Test
	public void allStudiesLoaded() {
		//set mockups and input parameters for all services
		ReflectionTestUtils.setField(extractor, "scanLocation", "file:src/test/resources/integration");
		ReflectionTestUtils.setField(extractor, "etlWorkingDir", etlWorkingDir.getRoot());
		
		ReflectionTestUtils.setField(transformer, "emailService", emailService);
		ReflectionTestUtils.setField(transformer, "transformerService", transformerService);
		
		ReflectionTestUtils.setField(validator, "emailService", emailService);
		ReflectionTestUtils.setField(validator, "validationService", validationService);
		ReflectionTestUtils.setField(validationService, "testFile", "src/test/resources/validator_tests/test.log");
		
		ReflectionTestUtils.setField(loader, "emailService", emailService);
		ReflectionTestUtils.setField(loader, "loaderService", loaderService);
		ReflectionTestUtils.setField(loaderService, "testFile", "src/test/resources/loader_tests/example.log");
		
		ReflectionTestUtils.setField(restarter, "restarterService", restarterService);
		
		ReflectionTestUtils.setField(etlProcessRunner, "transformer", transformer);
		ReflectionTestUtils.setField(etlProcessRunner, "validator", validator);
		ReflectionTestUtils.setField(etlProcessRunner, "loader", loader);
		ReflectionTestUtils.setField(etlProcessRunner, "restarter", restarter);
		
		ReflectionTestUtils.setField(scheduledScanner, "scanLocation", "file:src/test/resources/integration");
		ReflectionTestUtils.setField(scheduledScanner, "etlProcessRunner", etlProcessRunner);
		
		scheduledScanner.scan();
		
		//correct emails are sent
		//check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(true, emailService.isEmailValidationReportSent());
		assertEquals(true, emailService.isEmailStudiesLoadedSent());
		assertEquals(false, emailService.isEmailGenericErrorSent());
		
		//assert that the loader reports the studies that are passed by the validation (changes to mockup)
		List<String> expected = new ArrayList<String>();
		expected.add("study1");
		expected.add("study2");
		expected.add("study3");
		assertEquals(expected.size(), loaderService.getLoadedStudies().size());
	}
	
	@Test
	public void noStudiesLoaded() {
		//set mockups and input parameters for all services
		ReflectionTestUtils.setField(extractor, "scanLocation", "file:src/test/resources/integration");
		ReflectionTestUtils.setField(extractor, "etlWorkingDir", etlWorkingDir.getRoot());
		
		ReflectionTestUtils.setField(transformer, "emailService", emailService);
		ReflectionTestUtils.setField(transformer, "transformerService", transformerService);
		
		ReflectionTestUtils.setField(validator, "emailService", emailService);
		ReflectionTestUtils.setField(validator, "validationService", validationService);
		ReflectionTestUtils.setField(validationService, "testFile", "src/test/resources/validator_tests/test2.log");
		
		ReflectionTestUtils.setField(loader, "emailService", emailService);
		ReflectionTestUtils.setField(loader, "loaderService", loaderService);
		ReflectionTestUtils.setField(loaderService, "testFile", "src/test/resources/loader_tests/example.log");
		
		ReflectionTestUtils.setField(restarter, "restarterService", restarterService);
		
		ReflectionTestUtils.setField(etlProcessRunner, "transformer", transformer);
		ReflectionTestUtils.setField(etlProcessRunner, "validator", validator);
		ReflectionTestUtils.setField(etlProcessRunner, "loader", loader);
		ReflectionTestUtils.setField(etlProcessRunner, "restarter", restarter);
		
		ReflectionTestUtils.setField(scheduledScanner, "scanLocation", "file:src/test/resources/integration");
		ReflectionTestUtils.setField(scheduledScanner, "etlProcessRunner", etlProcessRunner);
		
		scheduledScanner.scan();
		
		//correct emails are sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(true, emailService.isEmailValidationReportSent());
		assertEquals(true, emailService.isEmailStudiesLoadedSent());
		assertEquals(false, emailService.isEmailGenericErrorSent());
		
		//assert that the loader reports the studies that are passed by the validation (changes to mockup)
		List<String> expected = new ArrayList<String>(); //No studies are loaded
		assertEquals(expected.size(), loaderService.getLoadedStudies().size());
	}
	
	@Test
	public void validationError() {
		//set mockups and input parameters for all services
		ReflectionTestUtils.setField(extractor, "scanLocation", "file:src/test/resources/integration");
		ReflectionTestUtils.setField(extractor, "etlWorkingDir", etlWorkingDir.getRoot());
		
		ReflectionTestUtils.setField(transformer, "emailService", emailService);
		ReflectionTestUtils.setField(transformer, "transformerService", transformerService);
		
		ReflectionTestUtils.setField(validator, "emailService", emailService);
		ReflectionTestUtils.setField(validator, "validationService", validationService);
		ReflectionTestUtils.setField(validationService, "testFile", "src/test/resources/validator_tests/none.log");
		
		ReflectionTestUtils.setField(loader, "emailService", emailService);
		ReflectionTestUtils.setField(loader, "loaderService", loaderService);
		ReflectionTestUtils.setField(loaderService, "testFile", "src/test/resources/loader_tests/example.log");
		
		ReflectionTestUtils.setField(restarter, "restarterService", restarterService);
		
		ReflectionTestUtils.setField(etlProcessRunner, "transformer", transformer);
		ReflectionTestUtils.setField(etlProcessRunner, "validator", validator);
		ReflectionTestUtils.setField(etlProcessRunner, "loader", loader);
		ReflectionTestUtils.setField(etlProcessRunner, "restarter", restarter);
		
		ReflectionTestUtils.setField(scheduledScanner, "scanLocation", "file:src/test/resources/integration");
		ReflectionTestUtils.setField(scheduledScanner, "etlProcessRunner", etlProcessRunner);
		
		scheduledScanner.scan();
		
		//check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(false, emailService.isEmailValidationReportSent()); 
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(true, emailService.isEmailGenericErrorSent());
		
		//no studies are loaded due to the error
		List<String> expected = new ArrayList<String>();
		assertEquals(expected.size(), loaderService.getLoadedStudies().size());
	}
	
	@Test
	public void noScanLocation() {
		//set mockups and input parameters for all services
		ReflectionTestUtils.setField(extractor, "scanLocation", "file:src/notfound");
		ReflectionTestUtils.setField(extractor, "etlWorkingDir", etlWorkingDir.getRoot());
		
		ReflectionTestUtils.setField(transformer, "emailService", emailService);
		ReflectionTestUtils.setField(transformer, "transformerService", transformerService);
		
		ReflectionTestUtils.setField(validator, "emailService", emailService);
		ReflectionTestUtils.setField(validator, "validationService", validationService);
		ReflectionTestUtils.setField(validationService, "testFile", "src/test/resources/validator_tests/test.log");
		
		ReflectionTestUtils.setField(loader, "emailService", emailService);
		ReflectionTestUtils.setField(loader, "loaderService", loaderService);
		ReflectionTestUtils.setField(loaderService, "testFile", "src/test/resources/loader_tests/example.log");
		
		ReflectionTestUtils.setField(restarter, "restarterService", restarterService);
		
		ReflectionTestUtils.setField(etlProcessRunner, "transformer", transformer);
		ReflectionTestUtils.setField(etlProcessRunner, "validator", validator);
		ReflectionTestUtils.setField(etlProcessRunner, "loader", loader);
		ReflectionTestUtils.setField(etlProcessRunner, "restarter", restarter);
		
		ReflectionTestUtils.setField(scheduledScanner, "scanLocation", "file:src/notfound");
		ReflectionTestUtils.setField(scheduledScanner, "etlProcessRunner", etlProcessRunner);
		
		scheduledScanner.scan();
		
		//no emails sent, since no yaml file is found in the scan location
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(false, emailService.isEmailValidationReportSent()); 
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(false, emailService.isEmailGenericErrorSent());
		
		//no studies are loaded
		List<String> expected = new ArrayList<String>();
		assertEquals(expected.size(), loaderService.getLoadedStudies().size());
	}
}
