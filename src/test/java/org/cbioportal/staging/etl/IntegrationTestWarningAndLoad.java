package org.cbioportal.staging.etl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.Map;

import org.cbioportal.staging.app.App;
import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.LoaderException;
import org.cbioportal.staging.exceptions.PublisherException;
import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

@SuppressWarnings("unchecked")
@RunWith(SpringRunner.class)
@TestPropertySource(locations = "classpath:e2e_studies/e2e_integration_test.properties")
@SpringBootTest(
    classes = App.class,
    properties = {"scan.location=classpath:e2e_studies/es_1", "validation.level=WARNING"}
)
public class IntegrationTestWarningAndLoad {

    @MockBean
    private EmailReportingService emailServiceImpl;

    @MockBean
    private IRestarter restarterService;

    @Autowired
    private ScheduledScanner scheduledScanner;

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

    @After
    public void cleanUp() throws ResourceUtilsException {
        ignoreSet.resetAndDeleteFile();
    }

    @Test
    public void throwValidationWarningsAndFail_es1() throws TemplateNotFoundException, MalformedTemplateNameException, ParseException,
    IOException, TemplateException, InterruptedException, ConfigurationException, ReporterException,
    ValidatorException, LoaderException, RestarterException, PublisherException, ResourceCollectionException {

        doNothing().when(restarterService).restart();

        boolean exitValue = scheduledScanner.scan();

        assert(exitValue);

        verify(transformationService, never()).transform(any(), any(), any());
        verify(validatorService, times(1)).validate(any(), any(), any());
        verify(loaderService, never()).load(any(), any());
        verify(restarterService, never()).restart();
        verify(publisherService, times(1)).publish(anyString(), any(Map.class)); // transformation step skipped, not called
        verify(ignoreSet, never()).appendResources(any(Resource[].class));
        verify(authorizerService, never()).authorizeStudies(anySet());

        verify(emailServiceImpl, never()).reportStudyFileNotFound(any(Map.class),anyInt());
        verify(emailServiceImpl, never()).reportTransformedStudies(any(Map.class),any(Map.class));
        verify(emailServiceImpl, times(1)).reportValidationReport(any(Map.class),anyString(),any(Map.class));
        verify(emailServiceImpl, never()).reportStudiesLoaded(any(Map.class),any(Map.class));
        verify(emailServiceImpl, never()).reportGenericError(any(),any());
    }

}