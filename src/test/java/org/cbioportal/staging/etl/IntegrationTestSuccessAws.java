package org.cbioportal.staging.etl;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.doReturn;


import com.amazonaws.services.s3.AmazonS3;
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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@SuppressWarnings("unchecked")
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = App.class,
    properties = {
        "scan.location.type=aws",
        "scan.location=s3://mybucket/my-scan-location/",
        "transformation.skip=false",
        "transformation.command.script=classpath:e2e_studies/es_0_tar/unzip.py",
        "scan.studyfiles.strategy=studydir"
    }
)
@TestPropertySource(locations = "classpath:e2e_studies/e2e_integration_test.properties")
public class IntegrationTestSuccessAws {

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

    @MockBean
    private AmazonS3 amazonS3;

    @SpyBean
    private ResourcePatternResolver resourcePatternResolver;

    @Before
    public void init() throws ResourceCollectionException, IOException, ResourceUtilsException {

        // Not this test does not use a real S3. Instead the interactions with S3 bucket are simulated.
        SimpleStorageResource fakeS3resource = new SimpleStorageResource(
            amazonS3, "mybucket", "my-scan-location/study1/study.tar.gz", null
        );
        Resource[] fakeResources = {fakeS3resource};
        Resource realStudyResource = resourcePatternResolver.getResource("classpath:e2e_studies/es_0_tar/test_study_es_0.tar.gz");
        Resource[] realResources = {realStudyResource};
        doReturn(fakeResources).when(resourcePatternResolver).getResources("s3://mybucket/my-scan-location/**/*");
        doReturn(realResources).when(resourcePatternResolver).getResources("s3://mybucket/my-scan-location/study1/**/*");
    }

    @After
    public void cleanUp() throws ResourceUtilsException {
        ignoreSet.resetAndDeleteFile();
        publisherService.clear();
    }

    @Test
    public void loadSuccessful_es0() throws IOException, InterruptedException, ConfigurationException, ReporterException,
            ValidatorException, LoaderException, RestarterException, PublisherException, ResourceCollectionException, TransformerException {

        doNothing().when(restarterService).restart();

        boolean exitValue = scheduledScanner.scan();

        assertTrue(exitValue);

        verify(transformationService, times(1)).transform(any(), any(), any());
        verify(validatorService, times(1)).validate(any(), any(), any());
        verify(loaderService, times(1)).load(any(), any());
        verify(restarterService, times(1)).restart();
        verify(publisherService, times(4)).publishFiles(any(Map.class));
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
