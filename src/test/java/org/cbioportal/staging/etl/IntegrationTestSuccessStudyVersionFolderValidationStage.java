package org.cbioportal.staging.etl;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;


import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;
import java.io.IOException;
import org.cbioportal.staging.app.App;
import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ExtractionException;
import org.cbioportal.staging.exceptions.LoaderException;
import org.cbioportal.staging.exceptions.PublisherException;
import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
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
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@SuppressWarnings("unchecked")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class)
@TestPropertySource(
    locations = "classpath:e2e_studies/e2e_integration_test.properties",
    properties = {
        "execution.stage=VALIDATE",
        "scan.location=classpath:e2e_studies/es_0_version/",
        "scan.studyfiles.strategy=versiondir",
        // for this test the etl.working.dir is the same as the scan.location
        "etl.working.dir=classpath:e2e_studies/es_0_version/",
        "etl.dir.format=study_id/study_version"
})
public class IntegrationTestSuccessStudyVersionFolderValidationStage {

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
    private Extractor extractor;

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
    public void loadSuccessful_es0()
        throws TemplateNotFoundException, MalformedTemplateNameException, ParseException,
        IOException, TemplateException, InterruptedException, ConfigurationException,
        ReporterException,
        ValidatorException, LoaderException, RestarterException, PublisherException,
        ResourceCollectionException, TransformerException, ExtractionException {

        doNothing().when(restarterService).restart();

        boolean exitValue = scheduledScanner.scan();

        assertTrue(exitValue);

        // check the extractor stage was skipped
        verify(extractor, never()).run(any());
        verify(transformationService, times(1)).transform(any(), any(), any());
        verify(validatorService, never()).validate(any(), any(), any());
        verify(loaderService, never()).load(any(), any());

    }

}
