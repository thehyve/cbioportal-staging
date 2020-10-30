package org.cbioportal.staging.etl;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
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
import org.cbioportal.staging.exceptions.TransformerException;
import org.cbioportal.staging.exceptions.ValidatorException;
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
import org.springframework.beans.factory.annotation.Value;
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
@SpringBootTest(classes = App.class)
@TestPropertySource(
    locations = "classpath:e2e_studies/e2e_integration_test.properties",
    properties = {
        "scan.location=classpath:e2e_studies/es_0_version/test_study_es_0/version2",
        "scan.studyfiles.strategy=versiondefined",
        "etl.dir.format=study_id/timestamp"
    }
)
public class IntegrationTestStudyVersionFolderDefinedSuccess {

    @MockBean
    private IReportingService reportingService;

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

    @Value("${scan.ignore.file:}")
    private File ignoreFile;

    @After
    public void cleanUp() {
        ignoreFile.delete();
        publisherService.clear();
    }

    @Test
    public void loadSuccessful_es0_versiondefined() throws TemplateNotFoundException, MalformedTemplateNameException, ParseException,
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
        verify(ignoreSet, times(1)).appendResources(any(Resource[].class));
        verify(authorizerService, times(1)).authorizeStudies(anySet());

        verify(reportingService, never()).reportStudyFileNotFound(any(), anyInt());
        verify(reportingService, times(1)).reportSummary(any(), any(), any(), any(), any(), any(), any(), any());
        verify(reportingService, never()).reportGenericError(any(), any());

        // check files have been published
        publisherService.getPublishedFiles().stream()
            .forEach(p -> assertTrue(p.exists()));
    }

}
