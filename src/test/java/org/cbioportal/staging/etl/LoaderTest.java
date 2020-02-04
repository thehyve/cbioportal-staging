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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.etl.Transformer.ExitStatus;
import org.cbioportal.staging.exceptions.LoaderException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.LoaderService;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.cbioportal.staging.services.resource.TestUtils;
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
    private LoaderService loaderService;

    @MockBean
    private ResourceUtils utils;

    @Before
    public void init() throws ResourceCollectionException {
        when(utils.createFileResource(any(Resource.class), any(String.class))).thenReturn(null);
    }

    @Test
    public void studySuccessfullyLoaded() throws LoaderException, ResourceCollectionException {

        when(loaderService.load(any(Resource.class), any(Resource.class))).thenReturn(ExitStatus.SUCCESS);

        Map<String, Resource> studies = new HashMap<>();
        studies.put("lgg_ucsf_2014", TestUtils.createMockResource("test/path", 0));
        Map<String, ExitStatus> loadingStatus = loader.load(studies);
        Map<String, ExitStatus> expectedLoadingStatus = new HashMap<String, ExitStatus>();
        expectedLoadingStatus.put("lgg_ucsf_2014", ExitStatus.SUCCESS);
		assertEquals(expectedLoadingStatus, loadingStatus);
    }

    @Test
    public void studyNotLoaded() throws LoaderException, ResourceCollectionException {

        when(loaderService.load(any(Resource.class), any(Resource.class))).thenReturn(ExitStatus.ERRORS);

        Map<String, Resource> studies = new HashMap<>();
        studies.put("lgg_ucsf_2014", TestUtils.createMockResource("test/path", 0));
        Map<String, ExitStatus> loadingStatus = loader.load(studies);
        Map<String, ExitStatus> expectedLoadingStatus = new HashMap<String, ExitStatus>();
        expectedLoadingStatus.put("lgg_ucsf_2014", ExitStatus.ERRORS);
		assertEquals(expectedLoadingStatus, loadingStatus);
    }

    @Test
    public void multipleStudiesAllLoaded() throws LoaderException, ResourceCollectionException {

        when(loaderService.load(any(Resource.class), any(Resource.class))).thenReturn(ExitStatus.SUCCESS);

        Map<String, Resource> studies = new HashMap<>();
        studies.put("lgg_ucsf_2014", TestUtils.createMockResource("test/path", 0));
        studies.put("study_2", TestUtils.createMockResource("test/path2", 1));
        Map<String, ExitStatus> loadingStatus = loader.load(studies);
        Map<String, ExitStatus> expectedLoadingStatus = new HashMap<String, ExitStatus>();
        expectedLoadingStatus.put("lgg_ucsf_2014", ExitStatus.SUCCESS);
        expectedLoadingStatus.put("study_2", ExitStatus.SUCCESS);
		assertEquals(expectedLoadingStatus, loadingStatus); //The study has been loaded
    }

    @Test
    public void multipleStudiesLoadedWithErrors() throws LoaderException, ResourceCollectionException {

        when(loaderService.load(any(Resource.class), any(Resource.class))).thenReturn(ExitStatus.SUCCESS, ExitStatus.ERRORS);

        Map<String, Resource> studies = new HashMap<>();
        studies.put("lgg_ucsf_2014", TestUtils.createMockResource("test/path", 0));
        studies.put("study_with_errors", TestUtils.createMockResource("test/path2", 1));
        Map<String, ExitStatus> loadingStatus = loader.load(studies);
        Map<String, ExitStatus> expectedLoadingStatus = new HashMap<String, ExitStatus>();
        expectedLoadingStatus.put("lgg_ucsf_2014", ExitStatus.SUCCESS);
        expectedLoadingStatus.put("study_with_errors", ExitStatus.ERRORS);
		assertEquals(expectedLoadingStatus, loadingStatus); //The study has been loaded
    }

    @Test
    public void multipleStudiesAllErrors() throws LoaderException, ResourceCollectionException {

        when(loaderService.load(any(Resource.class), any(Resource.class))).thenReturn(ExitStatus.ERRORS);

        Map<String, Resource> studies = new HashMap<>();
        studies.put("lgg_ucsf_2014", TestUtils.createMockResource("test/path", 0));
        studies.put("study_2", TestUtils.createMockResource("test/path2", 1));
        Map<String, ExitStatus> loadingStatus = loader.load(studies);
        Map<String, ExitStatus> expectedLoadingStatus = new HashMap<String, ExitStatus>();
        expectedLoadingStatus.put("lgg_ucsf_2014", ExitStatus.ERRORS);
        expectedLoadingStatus.put("study_2", ExitStatus.ERRORS);
		assertEquals(expectedLoadingStatus, loadingStatus); //The study has been loaded
    }
}
