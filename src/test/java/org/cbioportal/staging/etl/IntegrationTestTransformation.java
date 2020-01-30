package org.cbioportal.staging.etl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Map;

import org.cbioportal.staging.app.App;
import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.services.EmailServiceImpl;
import org.cbioportal.staging.services.RestarterService;
import org.cbioportal.staging.services.TransformerServiceImpl;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class)
@TestPropertySource(locations = "classpath:e2e_studies/e2e_integration_test.properties", properties = {
        "scan.location=classpath:e2e_studies/es_0_tar", "skip.transformation=false",
        "transformation.command.script=classpath:e2e_studies/es_0_tar/unzip.py" })
public class IntegrationTestTransformation {

    @MockBean
    private EmailServiceImpl emailServiceImpl;

    @MockBean
    private RestarterService restarterService;

    @Autowired
    private ScheduledScanner scheduledScanner;

    @Autowired
    private TransformerServiceImpl transformerService;

    @Test
    public void transformSuccessful_es0() throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, IOException, TemplateException, InterruptedException, ConfigurationException {

        doNothing().when(restarterService).restart();

        boolean exitValue = scheduledScanner.scan();

        assert(exitValue);
        verify(restarterService, times(1)).restart();
        verify(emailServiceImpl, never()).emailStudyFileNotFound(any(Map.class),anyInt());
        verify(emailServiceImpl, times(1)).emailTransformedStudies(any(Map.class),any(Map.class));
        verify(emailServiceImpl, times(1)).emailValidationReport(any(Map.class),anyString(),any(Map.class));
        verify(emailServiceImpl, times(1)).emailStudiesLoaded(any(Map.class),any(Map.class));
        verify(emailServiceImpl, never()).emailGenericError(any(),any());
    }

    @Test
    public void transformFailure_es0() throws TemplateNotFoundException, MalformedTemplateNameException, ParseException,
            IOException, TemplateException, InterruptedException, ConfigurationException {

        doNothing().when(restarterService).restart();

        // wire in a failing transformation script
        ReflectionTestUtils.setField(transformerService,
            "transformationCommandScript", "classpath:e2e_studies/es_0_tar/exit1.py");

        boolean exitValue = scheduledScanner.scan();

        assert(exitValue);
        verify(restarterService, never()).restart();
        verify(emailServiceImpl, never()).emailStudyFileNotFound(any(Map.class),anyInt());
        verify(emailServiceImpl, times(1)).emailTransformedStudies(any(Map.class),any(Map.class));
        verify(emailServiceImpl, never()).emailValidationReport(any(Map.class),anyString(),any(Map.class));
        verify(emailServiceImpl, never()).emailStudiesLoaded(any(Map.class),any(Map.class));
        verify(emailServiceImpl, never()).emailGenericError(any(),any());
    }

}
