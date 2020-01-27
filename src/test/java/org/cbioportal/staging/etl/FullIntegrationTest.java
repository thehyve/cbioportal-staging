package org.cbioportal.staging.etl;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
@TestPropertySource(locations="classpath:integration_test.properties")
@SpringBootTest
public class FullIntegrationTest {

    @Before
    public void setUp() throws Exception {
        emailService.reset();
    }

    @Test
    /**
     * This test assumes local cBioPortal + mySql containers are running.
     *
     */
    public void allStudiesLoaded() {

        //mock transformation (for now... TODO - later replace by real one):
        ReflectionTestUtils.setField(restarter, "restarterService", restarterService);

        //mock email service:
        ReflectionTestUtils.setField(extractor, "emailService", emailService);
        ReflectionTestUtils.setField(transformer, "emailService", emailService);
        ReflectionTestUtils.setField(validator, "emailService", emailService);
        ReflectionTestUtils.setField(loader, "emailService", emailService);


        ReflectionTestUtils.setField(etlProcessRunner, "extractor", extractor);
        ReflectionTestUtils.setField(etlProcessRunner, "transformer", transformer);
        ReflectionTestUtils.setField(etlProcessRunner, "validator", validator);
        ReflectionTestUtils.setField(etlProcessRunner, "loader", loader);
        ReflectionTestUtils.setField(etlProcessRunner, "restarter", restarter);
        ReflectionTestUtils.setField(etlProcessRunner, "publisher", publisher);
        ReflectionTestUtils.setField(etlProcessRunner, "studyPublishCommandPrefix", "null");

        ReflectionTestUtils.setField(scheduledScanner, "etlProcessRunner", etlProcessRunner);
        ReflectionTestUtils.setField(scheduledScanner, "S3PREFIX", "file:");
        ReflectionTestUtils.setField(scheduledScanner, "scanLocation", "file:src/test/resources/integration");
        ReflectionTestUtils.setField(scheduledScanner, "emailService", emailService);

        scheduledScanner.scan();

        //correct emails are sent
        //check that the correct email is sent
        assertEquals(false, emailService.isEmailGenericErrorSent());
        assertEquals(false, emailService.isEmailStudyErrorSent());
        assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
        assertEquals(false, emailService.isEmailTransformedStudiesSent()); //Study is already transformed
        assertEquals(true, emailService.isEmailValidationReportSent());
        assertEquals(false, emailService.isEmailStudiesLoadedSent());

        //assert that the loader reports the studies that are passed by the validation (changes to mockup)
        List<String> expected = new ArrayList<String>();
        expected.add("study1");
        expected.add("study2");
        expected.add("study3");
        //TODO - add this method to real loader service as well assertEquals(expected.size(), loaderService.getLoadedStudies().size());
    }

    @Test
    /**
     * This test assumes local cBioPortal + mySql containers are running.
     *
     */
    public void allLocalStudiesLoaded() {

        //mock transformation (for now... TODO - later replace by real one):
        ReflectionTestUtils.setField(restarter, "restarterService", restarterService);

        //mock email service:
        ReflectionTestUtils.setField(localExtractor, "emailService", emailService);
        ReflectionTestUtils.setField(transformer, "emailService", emailService);
        ReflectionTestUtils.setField(validator, "emailService", emailService);
        ReflectionTestUtils.setField(loader, "emailService", emailService);

        ReflectionTestUtils.setField(etlProcessRunner, "localExtractor", localExtractor);
        ReflectionTestUtils.setField(etlProcessRunner, "transformer", transformer);
        ReflectionTestUtils.setField(etlProcessRunner, "validator", validator);
        ReflectionTestUtils.setField(etlProcessRunner, "loader", loader);
        ReflectionTestUtils.setField(etlProcessRunner, "restarter", restarter);

        ReflectionTestUtils.setField(scheduledScanner, "etlProcessRunner", etlProcessRunner);
        ReflectionTestUtils.setField(scheduledScanner, "scanLocation", "file:src/test/resources/local_integration");

        scheduledScanner.scan();

        //correct emails are sent
        //check that the correct email is sent
        assertEquals(false, emailService.isEmailGenericErrorSent());
        assertEquals(false, emailService.isEmailStudyErrorSent());
        assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
        assertEquals(false, emailService.isEmailTransformedStudiesSent()); //Study is already transformed
        assertEquals(true, emailService.isEmailValidationReportSent());
        assertEquals(false, emailService.isEmailStudiesLoadedSent());

        //assert that the loader reports the studies that are passed by the validation (changes to mockup)
        List<String> expected = new ArrayList<String>();
        expected.add("study1");
        expected.add("study2");
        expected.add("study3");
        //TODO - enable assertions after test is fixed - currently test depends on a running database / cbioDB
        //assertEquals(expected.size(), loaderService.getLoadedStudies().size());

    }
}
