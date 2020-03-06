package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
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
import org.springframework.test.context.junit4.SpringRunner;
import org.yaml.snakeyaml.Yaml;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = {YamlFileStudyResourceStrategy.class, DefaultResourceProvider.class, ResourceUtils.class },
    properties = {"scan.studyfiles.strategy=yaml", "scan.location=file:/tmp"}
)
public class YamlFileStudyResourceStrategyTest {

    @MockBean
    private Yaml yamlParser;

    @Autowired
    private YamlFileStudyResourceStrategy resourceStrategy;

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
        Resource[] files = new Resource[] { TestUtils.createMockResource("list_of_studies", "yaml", 0) };
        Study[] result = resourceStrategy.resolveResources(files);
        assertEquals(2, result.length);

        Optional<Study> study1 = Stream.of(result).filter(s -> s.getStudyId().equals("study1")).findFirst();
        Optional<Study> study2 = Stream.of(result).filter(s -> s.getStudyId().equals("study2")).findFirst();

        assert(study1.isPresent());
        assertEquals(2, study1.get().getResources().length);
        List<String> filesStudy1 = Stream.of(study1.get().getResources())
            .map(ThrowingFunction.unchecked(e -> e.getURL().toString()))
            .collect(Collectors.toList());
        assert(filesStudy1.contains("file:/tmp/files/dummy1.txt"));
        assert(filesStudy1.contains("file:/tmp/files/dummy2.txt"));

        assert(study2.isPresent());
        assertEquals(2, study2.get().getResources().length);
        List<String> filesStudy2 = Stream.of(study2.get().getResources())
            .map(ThrowingFunction.unchecked(e -> e.getURL().toString()))
            .collect(Collectors.toList());
        assert(filesStudy2.contains("file:/tmp/files/dummy3.txt"));
        assert(filesStudy2.contains("file:/tmp/files/dummy4.txt"));
    }

    @Test
    public void testResolveResources_emptyArg() throws ResourceCollectionException {
        assert(resourceStrategy.resolveResources(new Resource[0]).length == 0);
    }

}