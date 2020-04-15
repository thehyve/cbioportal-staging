package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = { FolderStudyDefinedVersionResourceStrategy.class },
    properties = {
        "scan.studyfiles.strategy=definedversion"
    }
)
public class FolderStudyDefinedVersionResourceStrategyTest {

    @Autowired
    private FolderStudyDefinedVersionResourceStrategy folderStudyDefinedVersionResourceStrategy;

    @MockBean
    private IResourceProvider resourceProvider;

    @SpyBean
    private ResourceUtils utils;

    @Test
    public void mapOneStudy() throws ResourceCollectionException {

        Resource r = TestUtils.createMockResource("file:/study_folder/study1/version1", 0);

        ReflectionTestUtils.setField(folderStudyDefinedVersionResourceStrategy, "scanLocation", r);
        ReflectionTestUtils.setField(folderStudyDefinedVersionResourceStrategy, "studyId", "study1");

        Resource[] providedResources = new Resource[] {r};

        doReturn("timestamp").when(utils).getTimeStamp(anyString());

        Study[] outResult = folderStudyDefinedVersionResourceStrategy.resolveResources(providedResources);

        assertEquals(1, outResult.length);
        assertTrue(TestUtils.has(outResult, "study1"));
        assertEquals("timestamp",  outResult[0].getTimestamp());
        assertEquals("version1",  outResult[0].getVersion());

    }

    @Test(expected = ResourceCollectionException.class)
	public void noStudyId() throws ResourceCollectionException {

		Resource r = TestUtils.createMockResource("file:/study_folder/study1/version1", 0);

        ReflectionTestUtils.setField(folderStudyDefinedVersionResourceStrategy, "scanLocation", r);
        ReflectionTestUtils.setField(folderStudyDefinedVersionResourceStrategy, "studyId", null);

        Resource[] providedResources = new Resource[] {r};

        doReturn("timestamp").when(utils).getTimeStamp(anyString());

        folderStudyDefinedVersionResourceStrategy.resolveResources(providedResources);
    }

    @Test(expected = ResourceCollectionException.class)
	public void multipleStudyIds() throws ResourceCollectionException {

		Resource r = TestUtils.createMockResource("file:/study_folder/study1/version1", 0);

        ReflectionTestUtils.setField(folderStudyDefinedVersionResourceStrategy, "scanLocation", r);
        ReflectionTestUtils.setField(folderStudyDefinedVersionResourceStrategy, "studyId", "study1,study2,study3");

        Resource[] providedResources = new Resource[] {r};

        doReturn("timestamp").when(utils).getTimeStamp(anyString());

        folderStudyDefinedVersionResourceStrategy.resolveResources(providedResources);
    }

}