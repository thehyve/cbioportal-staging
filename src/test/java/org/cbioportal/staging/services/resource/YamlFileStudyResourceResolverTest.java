package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.pivovarit.function.ThrowingFunction;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.yaml.snakeyaml.Yaml;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
    "scan.location=file:/tmp"
})
@SpringBootTest(classes = {YamlFileStudyResourceResolver.class, ResourceUtils.class})
public class YamlFileStudyResourceResolverTest {

    @MockBean
    private Yaml yamlParser;

    @Autowired
    private YamlFileStudyResourceResolver resourceResolver;

    @Before
    public void init() {
        List<String> study1Files = new ArrayList<>();
        study1Files.add("files/dummy1.txt");
        study1Files.add("files/dummy2.txt");

        List<String> study2Files = new ArrayList<>();
        study2Files.add("files/dummy3.txt");
        study2Files.add("files/dummy4.txt");

        Map<String, List<String>> map = new HashMap<>();
        map.put("study1", study1Files);
        map.put("study2", study2Files);

        when(yamlParser.load(any(InputStream.class))).thenReturn(map);
    }

    @Test
    public void testResolveResources_success() throws ResourceCollectionException {
        Resource[] files = new Resource[1];
        files[0] = TestUtils.createMockResource("list_of_studies", "yaml", 0);
        Map<String,Resource[]> result = resourceResolver.resolveResources(files);
        assertEquals(2, result.entrySet().size());
        assertEquals(2, result.get("study1").length);
        assertEquals(2, result.get("study2").length);
        List<String> filesStudy1 = Stream.of(result.get("study1"))
            .map(ThrowingFunction.unchecked(e -> e.getURL().toString()))
            .collect(Collectors.toList());
        List<String> filesStudy2 = Stream.of(result.get("study2"))
            .map(ThrowingFunction.unchecked(e -> e.getURL().toString()))
            .collect(Collectors.toList());
        assert(filesStudy1.contains("file:/tmp/files/dummy1.txt"));
        assert(filesStudy1.contains("file:/tmp/files/dummy2.txt"));
        assert(filesStudy2.contains("file:/tmp/files/dummy3.txt"));
        assert(filesStudy2.contains("file:/tmp/files/dummy4.txt"));
    }

    @Test
    public void testResolveResources_emptyArg() throws ResourceCollectionException {
        assert(resourceResolver.resolveResources(new Resource[0]).isEmpty());
    }

}