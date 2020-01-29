package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

import java.io.BufferedReader;
import java.io.IOException;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ResourceIgnoreSet.class, org.cbioportal.staging.services.resource.ResourceIgnoreSetTest.MyTestConfiguration.class})
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

    @Autowired
    private ResourceIgnoreSet resourceIgnoreSet;

    @Test
    public void testGetExcludePaths() throws IOException {
        assertEquals(2, resourceIgnoreSet.size());
    }

    @Test
    public void testContains_success()  {
        assert(resourceIgnoreSet.contains("file:/dummy1.txt"));
        assert(resourceIgnoreSet.contains("file:/dummy2.txt"));
    }

    @Test
    public void testContains_failure()  {
        assertFalse(resourceIgnoreSet.contains("file:/dummy3.txt"));
    }

    @Test
    public void testEmpty()  {
        assertFalse(resourceIgnoreSet.isEmpty());
    }

}