package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = { FolderStudyVersionResourceStrategy.class },
    properties = {
        "scan.studyfiles.strategy=versiondir"
    }
)
public class FolderStudyVersionResourceStrategyTest {

    @Autowired
    private FolderStudyVersionResourceStrategy folderStudyVersionResourceStrategy;

    @MockBean
    private DefaultResourceProvider resourceProvider;

    @SpyBean
    private ResourceUtils utils;

    @Test
    public void mapOneStudyWithoutMetaFile() throws ResourceCollectionException {

        Resource r = TestUtils.createMockResource("file:/study_folder/study1", 0);
        Resource[] providedResources = new Resource[] {r};
        doReturn(providedResources).when(utils).extractDirs(eq(providedResources));

        Resource mostRecentDir = TestUtils.createMockResource("file:/study_folder/study1/version1", 1);
        Resource[] listedDirectories = new Resource[] {TestUtils.createMockResource("file:/study_folder/study1/version1", 0),
            mostRecentDir};
        when(resourceProvider.list(eq(r))).thenReturn(listedDirectories);
        doReturn(listedDirectories).when(utils).extractDirs(eq(listedDirectories));

        doReturn("timestamp").when(utils).getTimeStamp(anyString());

        Resource[] listedFiles = new Resource[] {TestUtils.createMockResource("file:/study_folder/study1/version1/meta_expression.txt", 0),
            TestUtils.createMockResource("file:/study_folder/study1/version1/data_expression.txt", 1)};
        when(resourceProvider.list(eq(mostRecentDir),anyBoolean(),anyBoolean())).thenReturn(listedFiles);

        Study[] outResult = folderStudyVersionResourceStrategy.resolveResources(providedResources);

        assertEquals(1, outResult.length);
        assert(TestUtils.has(outResult, "study1"));
        assertEquals("timestamp",  outResult[0].getTimestamp());
        assertEquals("version1",  outResult[0].getVersion());
        assertEquals(2, outResult[0].getResources().length);

    }

    @Test
    public void mapOneStudyWithMetaFile() throws ResourceCollectionException, ResourceUtilsException {

        Resource r = TestUtils.createMockResource("file:/study_folder/study1", 0);
        Resource[] providedResources = new Resource[] {r};
        doReturn(providedResources).when(utils).extractDirs(eq(providedResources));

        Resource mostRecentDir = TestUtils.createMockResource("file:/study_folder/study1/version1", 1);
        Resource[] listedDirectories = new Resource[] {TestUtils.createMockResource("file:/study_folder/study1/version1", 0),
            mostRecentDir};
        when(resourceProvider.list(eq(r))).thenReturn(listedDirectories);
        doReturn(listedDirectories).when(utils).extractDirs(eq(listedDirectories));

        doReturn("timestamp").when(utils).getTimeStamp(anyString());

        Map<String, String> study_id = new HashMap<String, String>();
        study_id.put("cancer_study_identifier", "study_1_meta_study_id");
        doReturn(study_id).when(utils).readMetaFile(any((Resource.class)));

        Resource[] listedFiles = new Resource[] {TestUtils.createMockResource("file:/study_folder/study1/version1/meta_study.txt", 0),
            TestUtils.createMockResource("file:/study_folder/study1/version1/data_expression.txt", 1)};
        when(resourceProvider.list(eq(mostRecentDir),anyBoolean(),anyBoolean())).thenReturn(listedFiles);

        Study[] outResult = folderStudyVersionResourceStrategy.resolveResources(providedResources);

        assertEquals(1, outResult.length);
        assert(TestUtils.has(outResult, "study_1_meta_study_id"));
        assertEquals("timestamp",  outResult[0].getTimestamp());
        assertEquals("version1",  outResult[0].getVersion());
        assertEquals(2, outResult[0].getResources().length);

    }

    @Test
    public void studyWithNoVersions() throws ResourceCollectionException {

        Resource r = TestUtils.createMockResource("file:/study_folder/study1", 0);
        Resource[] providedResources = new Resource[] {r};
        doReturn(providedResources).when(utils).extractDirs(eq(providedResources));

        Resource mostRecentDir = TestUtils.createMockResource("file:/study_folder/study1", 1);
        Resource[] listedFiles = new Resource[] {TestUtils.createMockResource("file:/study_folder/study1", 0),
            mostRecentDir};
        when(resourceProvider.list(eq(r))).thenReturn(listedFiles);

        Resource[] listedDirectories = new Resource[] {};
        doReturn(listedDirectories).when(utils).extractDirs(eq(listedFiles));

        Study[] outResult = folderStudyVersionResourceStrategy.resolveResources(providedResources);

        assertEquals(0, outResult.length);

    }

    @Test
    public void noResources() throws ResourceCollectionException {

        Resource[] providedResources = new Resource[] {};
        doReturn(providedResources).when(utils).extractDirs(eq(providedResources));

        Study[] outResult = folderStudyVersionResourceStrategy.resolveResources(providedResources);

        assertEquals(0, outResult.length);

    }

}