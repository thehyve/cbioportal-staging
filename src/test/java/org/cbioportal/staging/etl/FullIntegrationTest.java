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
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {org.cbioportal.staging.etl.Extractor.class,
        org.cbioportal.staging.etl.Transformer.class,
        org.cbioportal.staging.etl.Loader.class,
        org.cbioportal.staging.etl.Restarter.class,
        org.cbioportal.staging.etl.Validator.class,
        org.cbioportal.staging.etl.EmailServiceMockupImpl.class,
        org.cbioportal.staging.services.ValidationServiceImpl.class,
        org.cbioportal.staging.services.LoaderServiceImpl.class,
        org.cbioportal.staging.services.TransformerServiceImpl.class,
        org.cbioportal.staging.services.RestarterServiceImpl.class,
		org.cbioportal.staging.etl.ScheduledScannerServiceMockupImpl.class,
        org.cbioportal.staging.etl.ETLProcessRunner.class,
        org.cbioportal.staging.app.ScheduledScanner.class})
@SpringBootTest
@Import(MyTestConfiguration.class)
@TestPropertySource(locations="classpath:full_integration_test.properties")


public class FullIntegrationTest {

    @Autowired
    private Extractor extractor;

    @Autowired
    private Transformer transformer;

    @Autowired
    private Validator validator;

    @Autowired
    private Loader loader;

    @Autowired
    private EmailServiceMockupImpl emailService;

    @Autowired
    private ETLProcessRunner etlProcessRunner;

    @Autowired
    private ScheduledScanner scheduledScanner;

    @Before
    public void setUp() throws Exception {
        emailService.reset();
    }

    @Rule
    public TemporaryFolder etlWorkingDir = new TemporaryFolder();

    @Test
    /**
     * This test assumes local cBioPortal + mySql containers are running.
     * 
     */
    public void allStudiesLoaded() {
        //set mockups and input parameters for all services
        ReflectionTestUtils.setField(extractor, "scanLocation", "file:src/test/resources/integration");
        ReflectionTestUtils.setField(extractor, "etlWorkingDir", etlWorkingDir.getRoot());

        ReflectionTestUtils.setField(transformer, "emailService", emailService);

        ReflectionTestUtils.setField(validator, "emailService", emailService);

        ReflectionTestUtils.setField(loader, "emailService", emailService);

        ReflectionTestUtils.setField(etlProcessRunner, "transformer", transformer);
        ReflectionTestUtils.setField(etlProcessRunner, "validator", validator);
        ReflectionTestUtils.setField(etlProcessRunner, "loader", loader);

        ReflectionTestUtils.setField(scheduledScanner, "scanLocation", "file:src/test/resources/integration");
        ReflectionTestUtils.setField(scheduledScanner, "etlProcessRunner", etlProcessRunner);

        scheduledScanner.scan();

        //correct emails are sent
        //check that the correct email is sent
        assertEquals(false, emailService.isEmailGenericErrorSent());
        assertEquals(false, emailService.isEmailStudyErrorSent());
        assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
        assertEquals(true, emailService.isEmailValidationReportSent());
        assertEquals(true, emailService.isEmailStudiesLoadedSent());

        //assert that the loader reports the studies that are passed by the validation (changes to mockup)
        List<String> expected = new ArrayList<String>();
        expected.add("study1");
        expected.add("study2");
        expected.add("study3");
        //TODO - add this method to real loader service as well assertEquals(expected.size(), loaderService.getLoadedStudies().size());
    }
}
