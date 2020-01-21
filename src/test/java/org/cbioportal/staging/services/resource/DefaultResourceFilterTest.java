package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;

import java.io.BufferedReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class DefaultResourceFilterTest {

    @TestConfiguration
    static class MyTestConfiguration {

        @Bean
        public ResourceIgnoreSet resourceIgnoreSet() {
            ResourceIgnoreSet ignoreSet = mock(ResourceIgnoreSet.class);
            Mockito.doAnswer(invocation -> {
                String fileName = invocation.getArgumentAt(0, String.class);
                if (fileName.contains("dummy1")) {
                    return true;
                }
                return false;
            }).when(ignoreSet).contains(anyString());
            return ignoreSet;
        }

        @Bean
        public DefaultResourceFilter resourceFilterImpl() {
            return new DefaultResourceFilter();
        }

        @Bean
        public BufferedReader bufferedReader() {
            return mock(BufferedReader.class);
        }

    }

    @Autowired
    private DefaultResourceFilter defaultResourceFilter;

    @Test
    public void testFilterResources_removesFileIngnoreFile() throws ResourceCollectionException {

        Map<String, Resource[]> studyFiles = createResourceMap("study1", "file:///dummy1.txt", "file:///dummy2.txt");

        Map<String,Resource[]> filteredResources = defaultResourceFilter.filterResources(studyFiles);
        assertEquals(1, filteredResources.entrySet().size());
        assertNotNull(filteredResources.entrySet().iterator().next().getKey());
        assertEquals("study1", filteredResources.entrySet().iterator().next().getKey());
        assertEquals(1, filteredResources.entrySet().iterator().next().getValue().length);
    }

    @Test
    public void testFilterResources_notFiltered() throws ResourceCollectionException {

        Map<String, Resource[]> studyFiles = createResourceMap("study1", "file:///dummy3.txt", "file:///dummy4.txt");
        Map<String,Resource[]> filteredResources = defaultResourceFilter.filterResources(studyFiles);
        assertEquals(1, filteredResources.entrySet().size());
        assertNotNull(filteredResources.entrySet().iterator().next().getKey());
        assertEquals("study1", filteredResources.entrySet().iterator().next().getKey());
        assertEquals(2, filteredResources.entrySet().iterator().next().getValue().length);
    }

    private Map<String, Resource[]> createResourceMap(String studyId, String ... fileNames) {
        List<Resource> resources = new ArrayList<>();
        Stream.of(fileNames).forEach(e -> resources.add(TestUtils.createResource(e, 0)));
        Map<String, Resource[]> studyFiles = new HashMap<>();
        studyFiles.put(studyId, resources.toArray(new Resource[0]));
        return studyFiles;
    }

}