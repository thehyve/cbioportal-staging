package org.cbioportal.staging.etl;

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import org.cbioportal.staging.app.App;
import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.exceptions.*;
import org.cbioportal.staging.services.authorize.AuthorizerServiceImpl;
import org.cbioportal.staging.services.command.IRestarter;
import org.cbioportal.staging.services.etl.LoaderServiceImpl;
import org.cbioportal.staging.services.etl.TransformerServiceImpl;
import org.cbioportal.staging.services.etl.ValidatorServiceImpl;
import org.cbioportal.staging.services.publish.PublisherServiceImpl;
import org.cbioportal.staging.services.report.IReportingService;
import org.cbioportal.staging.services.resource.ResourceIgnoreSet;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class)
@TestPropertySource(
    locations = "classpath:e2e_studies/e2e_integration_test.properties",
    properties = {
            "scan.location=classpath:e2e_studies/test_whitespace"
    }
)
public class IntegrationTestSuccessWhitespaceInUrl {

    @Autowired
    private ScheduledScanner scheduledScanner;

    @MockBean
    private IRestarter restarterService;

    @MockBean
    private IReportingService reportingService;

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
        publisherService.clear();
    }

    @Test
    public void loadSuccessful_es0() throws TemplateNotFoundException, MalformedTemplateNameException, ParseException,
            IOException, TemplateException, InterruptedException, ConfigurationException, ReporterException,
            ValidatorException, LoaderException, RestarterException, PublisherException, ResourceCollectionException, TransformerException {

        doNothing().when(restarterService).restart();

        boolean exitValue = scheduledScanner.scan();

        assertTrue(exitValue);

        verify(transformationService, never()).transform(any(), any(), any());
        verify(validatorService, times(1)).validate(any(), any(), any());
        verify(loaderService, times(1)).load(any(), any());
        verify(restarterService, times(1)).restart();
        verify(publisherService, times(3)).publishFiles(any(Map.class)); // transformation step skipped, not called
        verify(ignoreSet, times(1)).appendResources(any());
        verify(authorizerService, times(1)).authorizeStudies(anySet());

        verify(reportingService, never()).reportStudyFileNotFound(any(), anyInt());
        verify(reportingService, times(1)).reportSummary(any(), any(), any(), any(), any(), any(), any(), any());
        verify(reportingService, never()).reportGenericError(any(), any());

        // check files have been published
        publisherService.getPublishedFiles().stream()
            .forEach(p -> assertTrue(p.exists()));

    }

}
