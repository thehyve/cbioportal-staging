package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = ResourceUtils.class)
public class ResourceUtilsTest {

    @Autowired
    private ResourceUtils utils;

    @Test
    public void trimDir_success() {
        // TODO implement test
    }

    @Test
    public void stripResourceTypePrefix_success() {
        // TODO implement test
    }

    @Test
    public void extractDirs_success() {
        // TODO implement test
    }

    @Test
    public void readMetaFile_success() {
        // TODO implement test
    }

    @Test
    public void copyResource_success() {
        // TODO implement test
    }

    @Test
    public void getMostRecent_success() {
        Resource[] resources = createResources("prefix", "yaml").toArray(new Resource[0]);
        Resource selectedResource = utils.getMostRecent(resources);
        assertEquals(selectedResource, resources[resources.length - 1]);
    }

    @Test
    public void filterFiles() throws Exception {
        List<Resource> resources = createResources("prefix", "yaml");
        Resource target = TestUtils.createMockResource("dummy", "txt", 0);
        resources.add(target);
        Resource[] selectedResources = utils.filterFiles(resources.toArray(new Resource[0]), "dummy", "txt");
        assert (selectedResources.length == 1);
        assertEquals(selectedResources[0], target);
    }

    @Test
    public void getBasePath_identicalStrings() throws ConfigurationException {
        List<String> paths = new ArrayList<>();
        paths.add("file:/abc");
        paths.add("file:/abc");
        String common = utils.getBasePath(paths);
        assertEquals("file:", common);
    }

    @Test
    public void getBasePath_stringsDifferentLength() throws ConfigurationException {
        List<String> paths = new ArrayList<>();
        paths.add("file:/abc");
        paths.add("file:/abcdef");
        String common = utils.getBasePath(paths);
        assertEquals("file:", common);
    }

    @Test
    public void getBasePath_differentStrings() throws ConfigurationException {
        List<String> paths = new ArrayList<>();
        paths.add("file:/abc/def");
        paths.add("file:/abc/ghi");
        String common = utils.getBasePath(paths);
        assertEquals("file:/abc", common);
    }

    @Test
    public void getBasePath_includesEmptyString() throws ConfigurationException {
        List<String> paths = new ArrayList<>();
        paths.add("");
        paths.add("file:/def");
        String common = utils.getBasePath(paths);
        assertEquals("", common);
    }

    @Test
    public void getBasePath_includesNullArgument() throws ConfigurationException {
        List<String> paths = new ArrayList<>();
        paths.add(null);
        paths.add("file:/def");
        String common = utils.getBasePath(paths);
        assertEquals("file:", common);
    }

    @Test
    public void getBasePath_emptyString() throws ConfigurationException {
        List<String> paths = new ArrayList<>();
        paths.add("");
        String common = utils.getBasePath(paths);
        assertEquals("", common);
    }

    private List<Resource> createResources(String prefix, String extension) {
        List<Resource> resources = new ArrayList<>();
        IntStream.range(0,4).forEach(i -> {
            resources.add(TestUtils.createMockResource(prefix, extension, i));
        });
        return resources;
    }

}