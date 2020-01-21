package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.mock;

import java.io.BufferedReader;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ResourceIgnoreSetTest {

    @TestConfiguration
    static class MyTestConfiguration {

        @Bean
        @Primary
        public BufferedReader bufferedReader() throws Exception {
            BufferedReader reader = mock(BufferedReader.class);
            int[] cnt = {0}; // needed for update of value in lambda
            Mockito.doAnswer(invocation -> {
                cnt[0]++;
                if (cnt[0] == 1) {
                    return "file:/dummy1.txt";
                }
                if (cnt[0] == 2) {
                    return "file:/dummy2.txt";
                }
                return null;
            }).when(reader).readLine();
            return reader;
        }

        @Bean
        public ResourceIgnoreSet resourceIgnoreSet() {
            return new ResourceIgnoreSet();
        }

    }

    @Autowired
    private ResourceIgnoreSet resourceIgnoreSet;

    @Test
    public void testGetExcludePaths()  {
        assertEquals(2, resourceIgnoreSet.getExcludePaths().size());
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