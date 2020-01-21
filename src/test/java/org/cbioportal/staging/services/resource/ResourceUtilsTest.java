package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class ResourceUtilsTest {

    @Test
    public void getMostRecent_success() {
        Resource[] resources = createResources("prefix", "yaml").toArray(new Resource[0]);
        Resource selectedResource = ResourceUtils.getMostRecent(resources);
        assertEquals(selectedResource, resources[resources.length-1]);
    }

    @Test
    public void filterFiles() throws Exception {
        List<Resource> resources = createResources("prefix", "yaml");
        Resource target = TestUtils.createResource("dummy", "txt", 0);
        resources.add(target);
        Resource[] selectedResources = ResourceUtils.filterFiles(resources.toArray(new Resource[0]), "dummy", "txt");
        assert(selectedResources.length == 1);
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