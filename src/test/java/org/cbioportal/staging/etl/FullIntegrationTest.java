package org.cbioportal.staging.etl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Map;

import org.cbioportal.staging.app.App;
import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.services.EmailServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
    "scan.location=file:src/test/resources/e2e_studies/es_0",
    "etl.working.dir=file:/tmp/staging-integration-test/etl-working-dir",
    "cbioportal.mode=docker",
    "cbioportal.docker.image=cbioportal/cbioportal:3.1.4",
    "cbioportal.docker.network=cbio-net",
    "cbioportal.docker.properties=/tmp/staging-integration-test/portal.properties",
    "central.share.location=file:/tmp/staging-integration-test/share",
    "skip.transformation=true",
    "cloud.aws.region.static=eu-central-1"
})
@SpringBootTest(classes = App.class)
public class FullIntegrationTest {

    @MockBean
    private EmailServiceImpl emailServiceImpl;

    @Autowired
    private ScheduledScanner scheduledScanner;

    @Test
    public void loadSuccessful_es0() throws TemplateNotFoundException, MalformedTemplateNameException, ParseException,
            IOException, TemplateException {
        boolean exitValue = scheduledScanner.scan();
        assert(exitValue);
        verify(emailServiceImpl, never()).emailStudyFileNotFound(any(Map.class),anyInt());
        verify(emailServiceImpl, never()).emailStudyError(anyString(),any(Exception.class));
        verify(emailServiceImpl, never()).emailTransformedStudies(any(Map.class),any(Map.class));
        verify(emailServiceImpl, never()).emailValidationReport(any(Map.class),anyString(),any(Map.class));
        verify(emailServiceImpl, times(1)).emailStudiesLoaded(any(Map.class),any(Map.class));
        verify(emailServiceImpl, never()).emailGenericError(any(),any());
    }

}
