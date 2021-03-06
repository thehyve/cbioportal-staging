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
import org.cbioportal.staging.services.resource.IResourceProvider;
import org.cbioportal.staging.services.resource.ResourceIgnoreSet;
import org.cbioportal.staging.services.resource.ftp.IFtpGateway;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.Resource;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@SuppressWarnings("unchecked")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = App.class)
@TestPropertySource(
    locations = "classpath:e2e_studies/e2e_integration_test.properties",
    properties = {
        "scan.location=ftp:/localhost/studies/test_whitespace",
        "central.share.location=ftp:/localhost/share",
        "scan.location.type=sftp",
        "ftp.host=localhost",
        "ftp.port=9922",
        "ftp.user=testuser",
        "ftp.password=testuser",
        "sftp.privateKey=classpath:ftp_server/key",
        "sftp.privateKeyPassphrase=P@ssword1",
    }
)
public class IntegrationTestSftpYamlStrategyWhitespaceinUrl {

    @Autowired
    private ScheduledScanner scheduledScanner;

    @MockBean
    private IReportingService reportingService;

    @MockBean
    private IRestarter restarterService;

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

    @Autowired
    private IResourceProvider ftpProvider;

    @Autowired
    private IFtpGateway ftpGateway;

    @After
    public void cleanUp() throws ResourceUtilsException {
        ignoreSet.resetAndDeleteFile();
        publisherService.clear();

        // delete the log files from ftp server
        List<SftpFileInfo> logFiles = ftpGateway.lsDirRecur("/share/");
        logFiles.stream().forEach(e -> ftpGateway.rm("/share/" + e.getFilename()));
    }

    @Test
    public void loadSuccessful_es0()
    throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException,
    TemplateException, InterruptedException, ConfigurationException, ReporterException, ValidatorException,
    LoaderException, RestarterException, PublisherException, ResourceCollectionException, TransformerException {

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
        Resource[] remoteFiles = ftpProvider.list(ftpProvider.getResource("ftp:///localhost/share"), true);
        assertTrue(has(remoteFiles, "test_study_es_0_yaml_loading_log.txt"));
        assertTrue(has(remoteFiles, "test_study_es_0_yaml_validation_log.txt"));
        assertTrue(has(remoteFiles, "test_study_es_0_yaml_validation_report.html"));
    }

    private boolean has(Resource[] resources, String fileName) {
        return Stream.of(resources).anyMatch(s -> s.getFilename().equals(fileName));
    }

}
