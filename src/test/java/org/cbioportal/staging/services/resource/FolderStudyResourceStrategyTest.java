package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { FolderStudyResourceStrategy.class },
    properties = {"scan.studyfiles.strategy=studydir"})
public class FolderStudyResourceStrategyTest {

    @Autowired
    private FolderStudyResourceStrategy folderStudyResourceStrategy;

    @MockBean
    private IResourceProvider resourceProvider;

    @MockBean
    private ResourceUtils utils;

    @Test
    public void testDetectStudyIdFromPath() throws ResourceCollectionException, FileNotFoundException, IOException {

        List<Resource> providedResources = new ArrayList<>();
        providedResources.add(TestUtils.createMockResource("file:/study_folder/not_a_study_meta_file.txt", 0));
        when(resourceProvider.list(any(), anyBoolean(), anyBoolean())).thenReturn(providedResources.toArray(new Resource[0]));

        Resource[] studyDirs = new Resource[] { TestUtils.createMockResource("file:/study_folder/", 1) };
        Map<String, String> metaFileContents = new HashMap<>();
        metaFileContents.put("cancer_study_identifier", "dummy_study_id_1");
        when(utils.extractDirs(any())).thenReturn(studyDirs);
        when(utils.trimPathRight(anyString())).thenReturn("file:/study_folder");

        Study[] resources = folderStudyResourceStrategy.resolveResources(studyDirs);

        assertEquals(1, resources.length);
        assertTrue(TestUtils.has(resources, "study_folder"));
    }

    @Test
    public void testDetectStudyIdFromMetaFile()
            throws ResourceCollectionException, FileNotFoundException, IOException, ResourceUtilsException {

        List<Resource> providedResources = new ArrayList<>();
        providedResources.add(TestUtils.createMockResource("file:/study_folder/meta_study.txt", 0));
        when(resourceProvider.list(any(),anyBoolean(), anyBoolean())).thenReturn(providedResources.toArray(new Resource[0]));

        Resource[] studyDirs = new Resource[] {TestUtils.createMockResource("file:/study_folder/", 1)};
        Map<String,String> metaFileContents = new HashMap<>();
        metaFileContents.put("cancer_study_identifier", "dummy_study_id_1");
        when(utils.readMetaFile(any())).thenReturn(metaFileContents);
        when(utils.extractDirs(any())).thenReturn(studyDirs);
        when(utils.trimPathRight(anyString())).thenReturn("file:/study_folder");

        Study[] resources = folderStudyResourceStrategy.resolveResources(studyDirs);

        assertEquals(1, resources.length);
        assertTrue(TestUtils.has(resources, "dummy_study_id_1"));
    }

}