/*
* Copyright (c) 2018 The Hyve B.V.
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as
* published by the Free Software Foundation, either version 3 of the
* License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package org.cbioportal.staging.etl;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.LoaderException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.etl.ILoaderService;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.cbioportal.staging.services.resource.Study;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = org.cbioportal.staging.etl.Loader.class)
public class LoaderTest {

    @Autowired
    private Loader loader;

    @MockBean
    private ILoaderService loaderService;

    @MockBean
    private ResourceUtils utils;

    @Before
    public void init() throws ResourceCollectionException, ResourceUtilsException {
        when(utils.createFileResource(isA(Resource.class), any(String.class))).thenReturn(null);
    }

    @Test
    public void studySuccessfullyLoaded() throws LoaderException, ResourceCollectionException {

        when(loaderService.load(isA(Resource.class), any())).thenReturn(ExitStatus.SUCCESS);

        Study dummyStudy = new Study("lgg_ucsf_2014", null, null, TestUtils.createMockResource("file:/test/path", 0), null);
        Study[] studies = TestUtils.studyList(dummyStudy);

        Map<Study, ExitStatus> loadingStatus = loader.load(studies);

        Map<Study, ExitStatus> expectedLoadingStatus = new HashMap<Study, ExitStatus>();
        expectedLoadingStatus.put(dummyStudy, ExitStatus.SUCCESS);
        assertEquals(expectedLoadingStatus, loadingStatus);

        assertEquals(true, loader.areStudiesLoaded());

        Map<Study, Resource> logPaths = new HashMap<>();
        logPaths.put(dummyStudy, null);
        assertEquals(logPaths, loader.getLogFiles());
    }

    @Test
    public void studyNotLoaded() throws LoaderException, ResourceCollectionException {

        when(loaderService.load(isA(Resource.class), any())).thenReturn(ExitStatus.ERROR);

        Study dummyStudy = new Study("lgg_ucsf_2014", null, null, TestUtils.createMockResource("file:/test/path", 0), null);
        Study[] studies = TestUtils.studyList(dummyStudy);

        Map<Study, ExitStatus> loadingStatus = loader.load(studies);
        Map<Study, ExitStatus> expectedLoadingStatus = new HashMap<Study, ExitStatus>();
        expectedLoadingStatus.put(dummyStudy, ExitStatus.ERROR);
        assertEquals(expectedLoadingStatus, loadingStatus);

        assertEquals(false, loader.areStudiesLoaded());

        Map<Study, Resource> logPaths = new HashMap<>();
        logPaths.put(dummyStudy, null);
        assertEquals(logPaths, loader.getLogFiles());
    }

    @Test
    public void multipleStudiesAllLoaded() throws LoaderException, ResourceCollectionException {

        when(loaderService.load(isA(Resource.class), any())).thenReturn(ExitStatus.SUCCESS);

        Study dummyStudy = new Study("lgg_ucsf_2014", null, null, TestUtils.createMockResource("file:/test/path", 0), null);
        Study dummyStudy2 = new Study("study_2", null, null, TestUtils.createMockResource("file:/test/path2", 1), null);
        Study[] studies = TestUtils.studyList(dummyStudy, dummyStudy2);

        Map<Study, ExitStatus> loadingStatus = loader.load(studies);
        Map<Study, ExitStatus> expectedLoadingStatus = new HashMap<Study, ExitStatus>();
        expectedLoadingStatus.put(dummyStudy, ExitStatus.SUCCESS);
        expectedLoadingStatus.put(dummyStudy2, ExitStatus.SUCCESS);
        assertEquals(expectedLoadingStatus, loadingStatus);

        assertEquals(true, loader.areStudiesLoaded());

        Map<Study, Resource> logPaths = new HashMap<>();
        logPaths.put(dummyStudy, null);
        logPaths.put(dummyStudy2, null);
        assertEquals(logPaths, loader.getLogFiles());
    }

    @Test
    public void multipleStudiesLoadedWithErrors() throws LoaderException, ResourceCollectionException {

        when(loaderService.load(isA(Resource.class), any())).thenReturn(ExitStatus.SUCCESS, ExitStatus.ERROR);

        Study dummyStudy = new Study("lgg_ucsf_2014", null, null, TestUtils.createMockResource("file:/test/path", 0), null);
        Study dummyStudy2 = new Study("study_with_errors", null, null, TestUtils.createMockResource("file:/test/path2", 1), null);
        Study[] studies = TestUtils.studyList(dummyStudy, dummyStudy2);

        Map<Study, ExitStatus> loadingStatus = loader.load(studies);
        Map<Study, ExitStatus> expectedLoadingStatus = new HashMap<Study, ExitStatus>();
        expectedLoadingStatus.put(dummyStudy, ExitStatus.SUCCESS);
        expectedLoadingStatus.put(dummyStudy2, ExitStatus.ERROR);
        assertEquals(expectedLoadingStatus, loadingStatus);

        assertEquals(true, loader.areStudiesLoaded());

        Map<Study, Resource> logPaths = new HashMap<>();
        logPaths.put(dummyStudy, null);
        logPaths.put(dummyStudy2, null);
        assertEquals(logPaths, loader.getLogFiles());
    }

    @Test
    public void multipleStudiesAllErrors() throws LoaderException, ResourceCollectionException {

        when(loaderService.load(isA(Resource.class), any())).thenReturn(ExitStatus.ERROR);

        Study dummyStudy = new Study("lgg_ucsf_2014", null, null, TestUtils.createMockResource("file:/test/path", 0), null);
        Study dummyStudy2 = new Study("study_2", null, null, TestUtils.createMockResource("file:/test/path2", 1), null);
        Study[] studies = TestUtils.studyList(dummyStudy, dummyStudy2);

        Map<Study, ExitStatus> loadingStatus = loader.load(studies);
        Map<Study, ExitStatus> expectedLoadingStatus = new HashMap<Study, ExitStatus>();
        expectedLoadingStatus.put(dummyStudy, ExitStatus.ERROR);
        expectedLoadingStatus.put(dummyStudy2, ExitStatus.ERROR);
        assertEquals(expectedLoadingStatus, loadingStatus);

        assertEquals(false, loader.areStudiesLoaded());

        Map<Study, Resource> logPaths = new HashMap<>();
        logPaths.put(dummyStudy, null);
        logPaths.put(dummyStudy2, null);
        assertEquals(logPaths, loader.getLogFiles());
    }
}
