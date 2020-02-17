package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {DefaultResourceFilter.class, ResourceUtils.class},
    properties = "scan.location=file:/scan_location"
)
public class DefaultResourceFilterTest {

    @MockBean
    private ResourceIgnoreSet resourceIgnoreSet;

    @Autowired
    private DefaultResourceFilter defaultResourceFilter;

    @Before
    public void init() {
        doAnswer(invocation -> {
            String fileName = invocation.getArgument(0, String.class);
            if (fileName.contains("dummy1")) {
                return true;
            }
            return false;
        }).when(resourceIgnoreSet).contains(anyString());
    }

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

    @Test
    public void testFilterResources_scanExtractFolder() throws ResourceCollectionException {

        List<String> includeDirs = new ArrayList<>();
        includeDirs.add("included_dir");
        ReflectionTestUtils.setField(defaultResourceFilter, "includedDirs", includeDirs);

        Map<String, Resource[]> studyFiles = createResourceMap("study1", "file:///scan_location/included_dir/dummy5.txt", "file:///scan_location/included_dir/dummy6.txt");
        Map<String, Resource[]> studyFiles2 = createResourceMap("study2", "file:///scan_location/excluded_dir/dummy7.txt", "file:///scan_location/excluded_dir/dummy8.txt");
        studyFiles.put("study2", studyFiles2.get("study2"));

        Map<String,Resource[]> filteredResources = defaultResourceFilter.filterResources(studyFiles);

        assertEquals(1, filteredResources.entrySet().size());
        assertNotNull(filteredResources.entrySet().iterator().next().getKey());
        assertEquals("study1", filteredResources.entrySet().iterator().next().getKey());
        assertEquals(2, filteredResources.entrySet().iterator().next().getValue().length);
    }

    @Test
    public void testFilterResources_nullArgument() throws ResourceCollectionException {
        Map<String,Resource[]> filteredResources = defaultResourceFilter.filterResources(null);
        assertEquals(0, filteredResources.entrySet().size());
    }

    private Map<String, Resource[]> createResourceMap(String studyId, String ... fileNames) {
        List<Resource> resources = new ArrayList<>();
        Stream.of(fileNames).forEach(e -> resources.add(TestUtils.createMockResource(e, 0)));
        Map<String, Resource[]> studyFiles = new HashMap<>();
        studyFiles.put(studyId, resources.toArray(new Resource[0]));
        return studyFiles;
    }

}