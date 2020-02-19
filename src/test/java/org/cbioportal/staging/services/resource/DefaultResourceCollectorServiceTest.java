package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;

import java.util.ArrayList;
import java.util.List;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DefaultResourceCollectorService.class})
public class DefaultResourceCollectorServiceTest {

    @Autowired
    private DefaultResourceCollectorService defaultResourceCollectorService;

    @MockBean
    public DefaultResourceProvider resourceProvider;

    @MockBean
    public YamlFileStudyResourceStrategy studyResourceStrategy;

    @MockBean
    private DefaultResourceFilter resourceFilter;

    @Before
    public void initMocks() throws ResourceCollectionException {

        List<Resource> providedResources = new ArrayList<>();
        providedResources.add(TestUtils.createMockResource("dummy", "txt", 1));
        Mockito.when(resourceProvider.list(any())).thenReturn(providedResources.toArray(new Resource[0]), new Resource[0]); // returns empty array the second time

        Mockito.when(resourceFilter.filterResources(any())).thenAnswer(i -> i.getArguments()[0]);
    }

    Resource fakeScanLocation = TestUtils.createMockResource("file:/tmp", 0);

    @Test
    public void testGetResources_success() throws ResourceCollectionException, ConfigurationException {

        Resource[] strategyResources = new Resource[] {TestUtils.createMockResource("dummy", "txt", 1), TestUtils.createMockResource("dummy", "txt", 2)};
        Study[] study = new Study[] {new Study("study1", null, null, null, strategyResources)};
        Mockito.when(studyResourceStrategy.resolveResources(any())).thenReturn(study);

        Study[] resources = defaultResourceCollectorService.getResources(fakeScanLocation);
        assertEquals(1, resources.length);
        assertEquals("study1", resources[0].getStudyId());
        assertEquals(2, resources[0].getResources().length);
    }

    @Test(expected = ConfigurationException.class)
    public void testGetResources_nullArgument() throws ConfigurationException, ResourceCollectionException {
        defaultResourceCollectorService.getResources(null);
    }

    @Test
    public void testGetResources_noFilesAtScanlocation() throws ConfigurationException, ResourceCollectionException {

        Mockito.when(resourceProvider.list(any())).thenReturn(new Resource[0]);

        Study[] resources = defaultResourceCollectorService.getResources(fakeScanLocation);
        assertEquals(0, resources.length);
    }

}