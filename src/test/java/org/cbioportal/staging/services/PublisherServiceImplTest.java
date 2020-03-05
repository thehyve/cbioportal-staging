package org.cbioportal.staging.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.verify;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.PublisherException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.publish.PublisherServiceImpl;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.cbioportal.staging.services.resource.filesystem.FileSystemResourceProvider;
import org.cbioportal.staging.services.resource.filesystem.IFileSystemGateway;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { PublisherServiceImpl.class, FileSystemResourceProvider.class,
        ResourceUtils.class }, properties = { "central.share.location=file:/fake-share/",
                "transformation.directory=file:/transf-dir/" })
public class PublisherServiceImplTest {

    @Value("${central.share.location:}")
    private Resource centralShareLocation;

    @Autowired
    private PublisherServiceImpl publisherService;

    @SpyBean
    private FileSystemResourceProvider resourceProvider;

	@MockBean
	private IFileSystemGateway gateway;

    @Captor
    ArgumentCaptor<Resource> remoteDestinationDir;

    Resource fakePublishedLogFile = TestUtils.createMockResource("file:/fake-share/log1.txt", 0);

    @Before
    public void init() throws ResourceCollectionException {
        doReturn(fakePublishedLogFile).when(resourceProvider).copyToRemote(isA(Resource.class), isA(Resource.class));
    }

    @Test
    public void testPublish_success()
            throws ResourceCollectionException, PublisherException, ResourceUtilsException, IOException {

        Map<String,Resource> logFiles = new HashMap<>();
        Resource fakeLogFile = TestUtils.createMockResource("file:/transf-dir/sub_dir/log1.txt", 0);
        logFiles.put("dummy_study", fakeLogFile);
        Map<String,Resource> publishedLogFiles = publisherService.publishFiles(logFiles);

        // verify the correct path of the destination dir used for copying
        verify(resourceProvider).copyToRemote(remoteDestinationDir.capture(), eq(fakeLogFile));
        assertEquals("file:/fake-share/sub_dir/", remoteDestinationDir.getValue().getURL().toString());

        assertTrue(publishedLogFiles.containsKey("dummy_study"));
        assertEquals(fakePublishedLogFile, publishedLogFiles.get("dummy_study"));

    }

    @Test
    public void testPublish_subDirEvaluation()
            throws ResourceCollectionException, PublisherException, ResourceUtilsException, IOException {

        Map<String,Resource> logFiles = new HashMap<>();
        Resource fakeLogFile = TestUtils.createMockResource("file:/transf-dir/sub_dir/log1.txt", 0);
        logFiles.put("dummy_study", fakeLogFile);
        publisherService.publishFiles(logFiles);

        // verify the correct path of the destination dir used for copying
        verify(resourceProvider).copyToRemote(remoteDestinationDir.capture(), eq(fakeLogFile));
        assertEquals("file:/fake-share/sub_dir/", remoteDestinationDir.getValue().getURL().toString());

    }

    @Test(expected = PublisherException.class)
    public void testPublish_nullLogFiles() throws ResourceCollectionException, PublisherException {
        publisherService.publishFiles(null);
    }

}