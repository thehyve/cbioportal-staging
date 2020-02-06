package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@ActiveProfiles("studydir")
public class FolderStudyResourceResolverTest {

    @TestConfiguration
    public static class MyTestConfiguration {
        @Bean
        public FolderStudyResourceResolver folderStudyResourceResolver() {
            return new FolderStudyResourceResolver();
        }
    }

    @Autowired
    private FolderStudyResourceResolver folderStudyResourceResolver;

    @MockBean
    private DefaultResourceProvider resourceProvider;

    @MockBean
    private ResourceUtils utils;

    @Test
    public void testDetectStudyIdFromPath() throws ResourceCollectionException, FileNotFoundException, IOException {

        List<Resource> providedResources = new ArrayList<>();
        providedResources.add(TestUtils.createMockResource("file:/study_folder/not_a_study_meta_file.txt", 0));
        when(resourceProvider.list(any(),anyBoolean())).thenReturn(providedResources.toArray(new Resource[0]));

        Resource[] studyDirs = new Resource[] {TestUtils.createMockResource("file:/study_folder/", 1)};
        Map<String,String> metaFileContents = new HashMap<>();
        metaFileContents.put("cancer_study_identifier", "dummy_study_id_1");
        when(utils.extractDirs(any())).thenReturn(studyDirs);
        when(utils.trimDir(anyString())).thenReturn("file:/study_folder");

        Map<String,Resource[]> resources = folderStudyResourceResolver.resolveResources(studyDirs);

        assertEquals(1, resources.size());
        assert(resources.containsKey("study_folder"));
    }

    @Test
    public void testDetectStudyIdFromMetaFile() throws ResourceCollectionException, FileNotFoundException, IOException {

        List<Resource> providedResources = new ArrayList<>();
        providedResources.add(TestUtils.createMockResource("file:/study_folder/meta_study.txt", 0));
        when(resourceProvider.list(any(),anyBoolean())).thenReturn(providedResources.toArray(new Resource[0]));

        Resource[] studyDirs = new Resource[] {TestUtils.createMockResource("file:/study_folder/", 1)};
        Map<String,String> metaFileContents = new HashMap<>();
        metaFileContents.put("cancer_study_identifier", "dummy_study_id_1");
        when(utils.readMetaFile(any())).thenReturn(metaFileContents);
        when(utils.extractDirs(any())).thenReturn(studyDirs);
        when(utils.trimDir(anyString())).thenReturn("file:/study_folder");

        Map<String,Resource[]> resources = folderStudyResourceResolver.resolveResources(studyDirs);

        assertEquals(1, resources.size());
        assert(resources.containsKey("dummy_study_id_1"));
    }

}