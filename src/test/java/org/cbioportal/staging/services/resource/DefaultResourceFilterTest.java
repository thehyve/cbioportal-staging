package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;

import java.util.ArrayList;
import java.util.List;
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

        Study[] studyFiles = createResourceMap("study1", "file:///dummy1.txt", "file:///dummy2.txt");

        Study[] filteredResources = defaultResourceFilter.filterResources(studyFiles);
        assertEquals(1, filteredResources.length);
        assertEquals("study1", filteredResources[0].getStudyId());
        assertEquals(1, filteredResources[0].getResources().length);
    }

    @Test
    public void testFilterResources_notFiltered() throws ResourceCollectionException {

        Study[] studyFiles = createResourceMap("study1", "file:///dummy3.txt", "file:///dummy4.txt");
        Study[] filteredResources = defaultResourceFilter.filterResources(studyFiles);
        assertEquals(1, filteredResources.length);
        assertEquals("study1", filteredResources[0].getStudyId());
        assertEquals(2, filteredResources[0].getResources().length);
    }

    @Test
    public void testFilterResources_scanExtractFolder() throws ResourceCollectionException {

        List<String> includeDirs = new ArrayList<>();
        includeDirs.add("included_dir");
        ReflectionTestUtils.setField(defaultResourceFilter, "includedDirs", includeDirs);

        Study[] studyFiles1 = createResourceMap("study1", "file:///scan_location/included_dir/dummy5.txt", "file:///scan_location/included_dir/dummy6.txt");
        Study[] studyFiles2 = createResourceMap("study2", "file:///scan_location/excluded_dir/dummy7.txt", "file:///scan_location/excluded_dir/dummy8.txt");
        Study[] studyFiles = new Study[] {studyFiles1[0], studyFiles2[0]};

        Study[] filteredResources = defaultResourceFilter.filterResources(studyFiles);

        assertEquals(1, filteredResources.length);
        assertEquals("study1", filteredResources[0].getStudyId());
        assertEquals(2, filteredResources[0].getResources().length);
    }

    @Test
    public void testFilterResources_nullArgument() throws ResourceCollectionException {
        Study[] filteredResources = defaultResourceFilter.filterResources(null);
        assertEquals(0, filteredResources.length);
    }

    private Study[] createResourceMap(String studyId, String ... fileNames) {
        List<Resource> resources = new ArrayList<>();
        Stream.of(fileNames).forEach(e -> resources.add(TestUtils.createMockResource(e, 0)));
        return TestUtils.studyList(new Study(studyId, null, null, null, resources.toArray(new Resource[0])));
    }

}