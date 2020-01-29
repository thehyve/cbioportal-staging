package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

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
    public void getMostRecent_success() {
        Resource[] resources = createResources("prefix", "yaml").toArray(new Resource[0]);
        Resource selectedResource = utils.getMostRecent(resources);
        assertEquals(selectedResource, resources[resources.length - 1]);
    }

    @Test
    public void filterFiles() throws Exception {
        List<Resource> resources = createResources("prefix", "yaml");
        Resource target = TestUtils.createResource("dummy", "txt", 0);
        resources.add(target);
        Resource[] selectedResources = utils.filterFiles(resources.toArray(new Resource[0]), "dummy", "txt");
        assert (selectedResources.length == 1);
        assertEquals(selectedResources[0], target);
    }

    private List<Resource> createResources(String prefix, String extension) {
        List<Resource> resources = new ArrayList<>();
        IntStream.range(0,4).forEach(i -> {
            resources.add(TestUtils.createResource(prefix, extension, i));
        });
        return resources;
    }

}