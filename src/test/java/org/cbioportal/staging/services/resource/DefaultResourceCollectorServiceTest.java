package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.Matchers.any;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class DefaultResourceCollectorServiceTest {

    @InjectMocks
    private DefaultResourceCollectorService defaultResourceCollectorService;

    @Mock
    public IResourceProvider resourceProvider;

    @Mock
    public IStudyResourceResolver resourceStrategy;

    @Mock
    private IResourceFilter resourceFilter;

    @Before
    public void initMocks() throws ResourceCollectionException {

        MockitoAnnotations.initMocks(this);

        List<Resource> providedResources = new ArrayList<>();
        providedResources.add(TestUtils.createResource("dummy", "txt", 1));
        Mockito.when(resourceProvider.list(any())).thenReturn(providedResources.toArray(new Resource[0]), new Resource[0]); // returns empty array the second time

        Mockito.when(resourceFilter.filterResources(any())).thenAnswer(i -> i.getArguments()[0]);
    }

    @Test
    public void testGetResources_success() throws ResourceCollectionException, ConfigurationException {

        List<Resource> strategyResources = new ArrayList<>();
        strategyResources.add(TestUtils.createResource("dummy", "txt", 1));
        strategyResources.add(TestUtils.createResource("dummy", "txt", 2));
        Map<String, Resource[]> studyFiles = new HashMap<>();
        studyFiles.put("study1", strategyResources.toArray(new Resource[0]));
        Mockito.when(resourceStrategy.resolveResources(any())).thenReturn(studyFiles);

        Map<String,Resource[]> resources = defaultResourceCollectorService.getResources("scanlocation");
        assertEquals(resources.entrySet().size(), 1);
        assertNotNull(resources.entrySet().iterator().next().getKey());
        assertEquals(resources.entrySet().iterator().next().getKey(), "study1");
        assertEquals(resources.entrySet().iterator().next().getValue().length, 2);
    }

    @Test(expected = ConfigurationException.class)
    public void testGetResources_nullArgument() throws ConfigurationException, ResourceCollectionException {
        defaultResourceCollectorService.getResources(null);
    }

    @Test
    public void testGetResources_noFilesAtScanlocation() throws ConfigurationException, ResourceCollectionException {

        Mockito.when(resourceProvider.list(any())).thenReturn(new Resource[0]);

        Map<String,Resource[]> resources = defaultResourceCollectorService.getResources("scanlocation");
        assertEquals(0, resources.keySet().size());
    }

}