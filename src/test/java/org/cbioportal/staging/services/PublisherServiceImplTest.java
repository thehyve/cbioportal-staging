package org.cbioportal.staging.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.DirectoryCreatorException;
import org.cbioportal.staging.exceptions.PublisherException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.directory.DirectoryCreatorByJob;
import org.cbioportal.staging.services.directory.IDirectoryCreator;
import org.cbioportal.staging.services.publish.PublisherServiceImpl;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { PublisherServiceImpl.class, DirectoryCreatorByJob.class }, properties = {
        "central.share.location=file:/fake-share/" })
public class PublisherServiceImplTest {

    @Autowired
    private PublisherServiceImpl publisherService;

    @MockBean
    private IDirectoryCreator directoryCreator;

    @SpyBean
    private ResourceUtils utils;

    @Test
    public void testPublish_success()
            throws ResourceCollectionException, PublisherException, DirectoryCreatorException, ResourceUtilsException {

        Resource fakeShareLocation = TestUtils.createMockResource("file:/fake-share/", 0);
        doReturn(fakeShareLocation).when(utils).createDirResource(isA(Resource.class), eq("dummy-date"));
        when(directoryCreator.getCentralShareLocationPath(isA(Resource.class), anyString())).thenReturn(fakeShareLocation);

        Resource fakePublishedLogFile = TestUtils.createMockResource("file:/fake-share/log1.txt", 0);
        doReturn(fakePublishedLogFile).when(utils).copyResource(isA(Resource.class), isA(Resource.class), anyString());

        Map<String,Resource> logFiles = new HashMap<>();
        Resource fakeLogFile = TestUtils.createMockResource("file:/logdir/log1.txt", 0);
        logFiles.put("dummy_study", fakeLogFile);
        Map<String,Resource> publishedLogFiles = publisherService.publish("dummy-date", logFiles);

        verify(utils, times(1)).copyResource(fakeShareLocation, fakeLogFile, "log1.txt");
        assert(publishedLogFiles.containsKey("dummy_study"));
        assertEquals(fakePublishedLogFile, publishedLogFiles.get("dummy_study"));

    }

    @Test(expected = PublisherException.class)
    public void testPublish_failNullDate() throws ResourceCollectionException, PublisherException {
        Map<String,Resource> logFiles = new HashMap<>();
        Resource fakeLogFile = TestUtils.createMockResource("file:/logdir/log1.txt", 0);
        logFiles.put("dummy_study", fakeLogFile);
        publisherService.publish(null, logFiles);
    }

    @Test(expected = PublisherException.class)
    public void testPublish_failLogFiles() throws ResourceCollectionException, PublisherException {
        publisherService.publish("dummy_date", null);
    }

}