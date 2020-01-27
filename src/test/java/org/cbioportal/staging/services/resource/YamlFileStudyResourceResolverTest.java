package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.pivovarit.function.ThrowingFunction;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.yaml.snakeyaml.Yaml;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = {
    "scan.location=file:/tmp"
})
public class YamlFileStudyResourceResolverTest {

    @TestConfiguration
    static class MyTestConfiguration {

        @Bean
        @Primary
        public Yaml yamlParser() {
            Yaml yaml = mock(Yaml.class);

            List<String> study1Files = new ArrayList<>();
            study1Files.add("files/dummy1.txt");
            study1Files.add("files/dummy2.txt");

            List<String> study2Files = new ArrayList<>();
            study2Files.add("files/dummy3.txt");
            study2Files.add("files/dummy4.txt");

            Map<String, List<String>> map = new HashMap<>();
            map.put("study1", study1Files);
            map.put("study2", study2Files);

            when(yaml.load(any(InputStream.class))).thenReturn(map);
            return yaml;
        }

        @Bean
        public YamlFileStudyResourceResolver yamlResourceStrategy() {
            return new YamlFileStudyResourceResolver();
        }

    }

    @Autowired
    private YamlFileStudyResourceResolver yamlResourceStrategy;

    @Test
    public void testResolveResources_success() throws ResourceCollectionException {
        Resource[] files = new Resource[1];
        files[0] = TestUtils.createResource("list_of_studies", "yaml", 0);
        Map<String,Resource[]> result = yamlResourceStrategy.resolveResources(files);
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

    @Test(expected = ResourceCollectionException.class)
    public void testResolveResources_emptyArg() throws ResourceCollectionException {
        yamlResourceStrategy.resolveResources(new Resource[0]);
    }

}