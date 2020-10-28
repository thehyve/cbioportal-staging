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
import org.cbioportal.staging.services.directory.DirectoryCreator;
import org.cbioportal.staging.services.directory.IDirectoryCreator;
import org.cbioportal.staging.services.publish.PublisherServiceImpl;
import org.cbioportal.staging.services.resource.IResourceProvider;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.cbioportal.staging.services.resource.Study;
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
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { PublisherServiceImpl.class,
        ResourceUtils.class, DirectoryCreator.class }, properties = { "central.share.location=file:/fake-share/",
                "transformation.directory=file:/transf-dir/", "etl.dir.format:timestamp/study_id" })
public class PublisherServiceImplTest {

    @Value("${central.share.location:}")
    private Resource centralShareLocation;

    @Autowired
    private PublisherServiceImpl publisherService;

    @SpyBean
    private FileSystemResourceProvider resourceProvider;

    @SpyBean
    private IDirectoryCreator directoryCreator;

	@MockBean
    private IFileSystemGateway gateway;
    
    @Captor
    ArgumentCaptor<Resource> remoteDestinationDir;

    Resource fakePublishedLogFile = TestUtils.createMockResource("file:/fake-share/dummy-study/log1.txt", 0);

    @Before
    public void init() throws ResourceCollectionException {
        doReturn(fakePublishedLogFile).when(resourceProvider).copyToRemote(isA(Resource.class), isA(Resource.class));
        ReflectionTestUtils.setField(directoryCreator, "dirFormat", "timestamp/study_id");
    }

    @Test
    public void testPublish_success()
            throws ResourceCollectionException, PublisherException, ResourceUtilsException, IOException {

        Map<Study,Resource> logFiles = new HashMap<>();
        Resource fakeLogFile = TestUtils.createMockResource("file:/transf-dir/dummy-study/log1.txt", 0);
        Study dummyStudy = new Study("dummy-study", "dummy-version", "dummy-time", null, null);
        logFiles.put(dummyStudy, fakeLogFile);
        Map<Study,Resource> publishedLogFiles = publisherService.publishFiles(logFiles);

        // verify the correct path of the destination dir used for copying
        verify(resourceProvider).copyToRemote(remoteDestinationDir.capture(), eq(fakeLogFile));
        assertEquals("file:/fake-share/dummy-time/dummy-study", remoteDestinationDir.getValue().getURL().toString());

        assertTrue(publishedLogFiles.containsKey(dummyStudy));
        assertEquals(fakePublishedLogFile, publishedLogFiles.get(dummyStudy));

    }

    @Test
    public void testPublish_success_with_version()
            throws ResourceCollectionException, PublisherException, ResourceUtilsException, IOException {
        
        ReflectionTestUtils.setField(directoryCreator, "dirFormat", "study_id/study_version");

        Map<Study,Resource> logFiles = new HashMap<>();
        Resource fakeLogFile = TestUtils.createMockResource("file:/transf-dir/dummy-study/log1.txt", 0);
        Study dummyStudy = new Study("dummy-study", "dummy-version", "dummy-time", null, null);
        logFiles.put(dummyStudy, fakeLogFile);
        Map<Study,Resource> publishedLogFiles = publisherService.publishFiles(logFiles);

        // verify the correct path of the destination dir used for copying
        verify(resourceProvider).copyToRemote(remoteDestinationDir.capture(), eq(fakeLogFile));
        assertEquals("file:/fake-share/dummy-study/dummy-version", remoteDestinationDir.getValue().getURL().toString());

        assertTrue(publishedLogFiles.containsKey(dummyStudy));
        assertEquals(fakePublishedLogFile, publishedLogFiles.get(dummyStudy));

    }

    @Test(expected = PublisherException.class)
    public void testPublish_nullLogFiles() throws ResourceCollectionException, PublisherException {
        publisherService.publishFiles(null);
    }

}