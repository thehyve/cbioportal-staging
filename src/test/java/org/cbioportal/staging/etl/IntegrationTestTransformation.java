package org.cbioportal.staging.etl;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anySet;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import org.cbioportal.staging.app.App;
import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.LoaderException;
import org.cbioportal.staging.exceptions.PublisherException;
import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.RestarterException;
import org.cbioportal.staging.exceptions.ValidatorException;
import org.cbioportal.staging.services.AuthorizerServiceImpl;
import org.cbioportal.staging.services.IRestarter;
import org.cbioportal.staging.services.LoaderServiceImpl;
import org.cbioportal.staging.services.PublisherServiceImpl;
import org.cbioportal.staging.services.TransformerServiceImpl;
import org.cbioportal.staging.services.ValidatorServiceImpl;
import org.cbioportal.staging.services.reporting.EmailReportingService;
import org.cbioportal.staging.services.resource.ResourceIgnoreSet;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

@SuppressWarnings("unchecked")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class)
@TestPropertySource(locations = "classpath:e2e_studies/e2e_integration_test.properties", properties = {
        "scan.location=classpath:e2e_studies/es_0_tar", "skip.transformation=false",
        "transformation.command.script=classpath:e2e_studies/es_0_tar/unzip.py" })
public class IntegrationTestTransformation {

    @MockBean
    private EmailReportingService emailServiceImpl;

    @MockBean
    private IRestarter restarterService;

    @Autowired
    private ScheduledScanner scheduledScanner;

    @Autowired
    private TransformerServiceImpl transformerService;

    @SpyBean
    private PublisherServiceImpl publisherService;

    @SpyBean
    private ValidatorServiceImpl validatorService;

    @SpyBean
    private TransformerServiceImpl transformationService;

    @SpyBean
    private LoaderServiceImpl loaderService;

    @SpyBean
    private ResourceIgnoreSet ignoreSet;

    @MockBean
    private AuthorizerServiceImpl authorizerService;

    @Value("${scan.ignore.file:}")
    private File ignoreFile;

    @After
    public void cleanUp() {
        ignoreFile.delete();
    }

    @Test
    public void transformSuccessful_es0()
            throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException,
            TemplateException, InterruptedException, ConfigurationException, ReporterException, ValidatorException,
            LoaderException, RestarterException, PublisherException, ResourceCollectionException {

        doNothing().when(restarterService).restart();

        boolean exitValue = scheduledScanner.scan();

        assert(exitValue);

        verify(transformationService, times(1)).transform(any(), any(), any());
        verify(validatorService, times(1)).validate(any(), any(), any());
        verify(loaderService, times(1)).load(any(), any());
        verify(restarterService, times(1)).restart();
        verify(publisherService, times(3)).publish(anyString(), any(Map.class));
        verify(ignoreSet, times(1)).appendResources(any(Resource[].class));
        verify(authorizerService, times(1)).authorizeStudies(anySet());

        verify(emailServiceImpl, never()).reportStudyFileNotFound(any(Map.class),anyInt());
        verify(emailServiceImpl, times(1)).reportTransformedStudies(any(Map.class),any(Map.class));
        verify(emailServiceImpl, times(1)).reportValidationReport(any(Map.class),anyString(),any(Map.class));
        verify(emailServiceImpl, times(1)).reportStudiesLoaded(any(Map.class),any(Map.class));
        verify(emailServiceImpl, never()).reportGenericError(any(),any());
    }

    @Test
    public void transformFailure_es0() throws  TemplateNotFoundException, MalformedTemplateNameException, ParseException,
    IOException, TemplateException, InterruptedException, ConfigurationException, ReporterException,
    ValidatorException, LoaderException, RestarterException, PublisherException, ResourceCollectionException {

        // wire in a failing transformation script
        ReflectionTestUtils.setField(transformerService,
            "transformationCommandScript", "classpath:e2e_studies/es_0_tar/exit1.py");

        doNothing().when(restarterService).restart();

        boolean exitValue = scheduledScanner.scan();

        assert(exitValue);

        verify(transformationService, times(1)).transform(any(), any(), any());
        verify(validatorService, never()).validate(any(), any(), any());
        verify(loaderService, never()).load(any(), any());
        verify(restarterService, never()).restart();
        verify(publisherService, times(1)).publish(anyString(), any(Map.class));
        verify(ignoreSet, never()).appendResources(any(Resource[].class));
        verify(authorizerService, never()).authorizeStudies(anySet());

        verify(emailServiceImpl, never()).reportStudyFileNotFound(any(Map.class),anyInt());
        verify(emailServiceImpl, times(1)).reportTransformedStudies(any(Map.class),any(Map.class));
        verify(emailServiceImpl, never()).reportValidationReport(any(Map.class),anyString(),any(Map.class));
        verify(emailServiceImpl, never()).reportStudiesLoaded(any(Map.class),any(Map.class));
        verify(emailServiceImpl, never()).reportGenericError(any(),any());
    }

}
