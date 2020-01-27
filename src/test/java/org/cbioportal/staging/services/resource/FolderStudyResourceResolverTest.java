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

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
public class FolderStudyResourceResolverTest {

    @InjectMocks
    private FolderStudyResourceResolver studyResourceResolver;

    @Mock
    private DefaultResourceProvider resourceProvider;

    @Mock
    private ResourceUtils utils;

    @Before
    public void initMocks() {
        MockitoAnnotations.initMocks(this);
    }

    @Test
    public void testDetectStudyIdFromPath() throws ResourceCollectionException, FileNotFoundException, IOException {

        List<Resource> providedResources = new ArrayList<>();
        providedResources.add(TestUtils.createResource("file:/study_folder/not_a_study_meta_file.txt", 0));
        when(resourceProvider.list(any(),anyBoolean())).thenReturn(providedResources.toArray(new Resource[0]));

        Resource[] studyDirs = new Resource[] {TestUtils.createResource("file:/study_folder/", 1)};
        Map<String,String> metaFileContents = new HashMap<>();
        metaFileContents.put("cancer_study_identifier", "dummy_study_id_1");
        when(utils.extractDirs(any())).thenReturn(studyDirs);
        when(utils.trimDir(anyString())).thenReturn("file:/study_folder");

        Map<String,Resource[]> resources = studyResourceResolver.resolveResources(studyDirs);

        assertEquals(1, resources.size());
        assert(resources.containsKey("study_folder"));
    }

    @Test
    public void testDetectStudyIdFromMetaFile() throws ResourceCollectionException, FileNotFoundException, IOException {

        List<Resource> providedResources = new ArrayList<>();
        providedResources.add(TestUtils.createResource("file:/study_folder/meta_study.txt", 0));
        when(resourceProvider.list(any(),anyBoolean())).thenReturn(providedResources.toArray(new Resource[0]));

        Resource[] studyDirs = new Resource[] {TestUtils.createResource("file:/study_folder/", 1)};
        Map<String,String> metaFileContents = new HashMap<>();
        metaFileContents.put("cancer_study_identifier", "dummy_study_id_1");
        when(utils.readMetaFile(any())).thenReturn(metaFileContents);
        when(utils.extractDirs(any())).thenReturn(studyDirs);
        when(utils.trimDir(anyString())).thenReturn("file:/study_folder");

        Map<String,Resource[]> resources = studyResourceResolver.resolveResources(studyDirs);

        assertEquals(1, resources.size());
        assert(resources.containsKey("dummy_study_id_1"));
    }

}