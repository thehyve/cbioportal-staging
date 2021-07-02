package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Collection;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("unchecked")
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { ResourceIgnoreSet.class,
        org.cbioportal.staging.services.resource.ResourceIgnoreSetTest.MyTestConfiguration.class },
        properties = {"spring.main.allow-bean-definition-overriding=true"})
public class ResourceIgnoreSetTest {

    @TestConfiguration
    public static class MyTestConfiguration {

        @Bean
        @Primary
        public BufferedReader bufferedReader() throws Exception {
            BufferedReader reader = mock(BufferedReader.class);
            Mockito.when(reader.readLine()).thenReturn("file:/dummy1.txt", "file:/dummy2.txt", null);
            return reader;
        }

    }

    @SpyBean
    private ResourceUtils utils;

    @Autowired
    private ResourceIgnoreSet resourceIgnoreSet;

    @Test
    public void testGetExcludePaths() throws IOException {
        assertEquals(2, resourceIgnoreSet.size());
    }

    @Test
    public void testContains_success() {
        assert (resourceIgnoreSet.contains("file:/dummy1.txt"));
        assert (resourceIgnoreSet.contains("file:/dummy2.txt"));
    }

    @Test
    public void testContains_failure() {
        assertFalse(resourceIgnoreSet.contains("file:/dummy3.txt"));
    }

    @Test
    public void testEmpty() {
        assertFalse(resourceIgnoreSet.isEmpty());
    }

    @Test
    public void testAppendResources_success() throws IOException, ResourceCollectionException, ResourceUtilsException {

        WritableResource ignoreFile = TestUtils.createMockResource("file:/mock_ignore_file.txt", 0);
        ReflectionTestUtils.setField(resourceIgnoreSet, "ignoreFile", ignoreFile);
        doReturn(ignoreFile).when(utils).getWritableResource(isA(Resource.class));
        doNothing().when(utils).writeToFile(isA(WritableResource.class), anyString(), anyBoolean());

        String fileUrl = "file:/resource_to_be_ignored.txt";
        resourceIgnoreSet.appendResources(new Resource[] {TestUtils.createMockResource(fileUrl, 0)});

        assertTrue(resourceIgnoreSet.contains("file:/resource_to_be_ignored.txt"));
        verify(utils, times(1)).writeToFile(eq(ignoreFile), any(Collection.class), eq(true));

    }

    @Test
    public void testAppendResources_noIgnoreFileSpecified() throws IOException, ResourceCollectionException, ResourceUtilsException {

        ReflectionTestUtils.setField(resourceIgnoreSet, "ignoreFile", null);

        String fileUrl = "file:/resource_to_be_ignored.txt";
        resourceIgnoreSet.appendResources(new Resource[] {TestUtils.createMockResource(fileUrl, 0)});

        assertTrue(resourceIgnoreSet.contains("file:/resource_to_be_ignored.txt"));
        verify(utils, never()).writeToFile(any(WritableResource.class), any(Collection.class), anyBoolean());

    }

}