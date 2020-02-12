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

import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.DirectoryCreatorException;
import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.exceptions.TransformerException;
import org.cbioportal.staging.services.DirectoryCreatorByJob;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.IDirectoryCreator;
import org.cbioportal.staging.services.TransformerServiceImpl;
import org.cbioportal.staging.services.resource.DefaultResourceProvider;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { Transformer.class, TransformerServiceImpl.class, ResourceUtils.class,
        DefaultResourceProvider.class, DirectoryCreatorByJob.class })
public class TransformerTest {

    @Autowired
    private Transformer transformer;

    @MockBean
    private TransformerServiceImpl transformerService;

    @MockBean
    private ResourceUtils utils;

    @MockBean
    private DefaultResourceProvider provider;

    @MockBean
    private IDirectoryCreator directoryCreator;

    @Before
    public void init() throws ResourceCollectionException, IOException, TransformerException, ReporterException,
            ConfigurationException, DirectoryCreatorException, ResourceUtilsException {
        // mock utils.ensuredirs -> do nothing
        doNothing().when(utils).ensureDirs(isA(Resource.class));

        // mock utils.getResource() -> return input
        when(utils.createDirResource(isA(Resource.class),anyString())).thenAnswer(i -> i.getArguments()[0]);

        // mock utils.copyDirectory -> do nothing, check called
        doNothing().when(utils).copyDirectory(isA(Resource.class),isA(Resource.class));

        // mock transformerService.transform -> do nothing, check called
        when(transformerService.transform(any(),any(),any())).thenReturn(ExitStatus.SUCCESS);

        // TODO
        when(directoryCreator.createTransformedStudyDir(anyString(), anyString(),isA(Resource.class))).thenReturn(null);

        // mock utils.createLogFiles -> return Resource mock that has getFile()
        Resource logFile = TestUtils.createMockResource("file:/dummy_study_folder/log_file.txt", 0);
        when(logFile.getFile()).thenReturn(null);
        when(utils.createFileResource(isA(Resource.class), anyString())).thenReturn(logFile);

    }

    @Test
    public void testTransform_studyWithMetaStudyFile() throws ResourceCollectionException, TransformerException, ReporterException, ConfigurationException, IOException, ResourceUtilsException {

        // mock provider.list() -> return resource list that contains a meta_study file
        Resource[] studyFiles = new Resource[] {TestUtils.createMockResource("file:/dummy_study_folder/meta_study.txt", 0)};
        when(provider.list(isA(Resource.class))).thenReturn(studyFiles);

        Map<String, ExitStatus> exitStatus = transformer.transform("dummy-timestamp", dummyStudyInput(), "");

        verify(utils, times(1)).copyDirectory(any(),any());
        verify(transformerService, never()).transform(isA(Resource.class),isA(Resource.class),isA(Resource.class));
        assert(exitStatus.containsKey("dummy_study") && exitStatus.get("dummy_study") == ExitStatus.SKIPPED);
        assert(transformer.getLogFiles().containsKey("dummy_study loading log"));
        assert(transformer.getValidStudies().containsKey("dummy_study"));
    }

    @Test
    public void testTransform_studyThatNeedsTransformation() throws ResourceCollectionException, TransformerException, ReporterException, ConfigurationException, IOException, ResourceUtilsException {

        // mock provider.list() -> return resource list that does not contain a meta_study file
        Resource[] studyFiles = new Resource[] {TestUtils.createMockResource("file:/dummy_study_folder/file_that_needs_transformation.txt", 0)};
        when(provider.list(isA(Resource.class))).thenReturn(studyFiles);

        Map<String, ExitStatus> exitStatus = transformer.transform("dummy-timestamp", dummyStudyInput(), "");

        verify(utils, never()).copyDirectory(any(),any());
        verify(transformerService, times(1)).transform(any(),any(),any());
        assert(exitStatus.containsKey("dummy_study") && exitStatus.get("dummy_study") == ExitStatus.SUCCESS);
        assert(transformer.getLogFiles().containsKey("dummy_study loading log"));
        assert(transformer.getValidStudies().containsKey("dummy_study"));
    }

    @Test
    public void testTransform_studyWithWarnings()
            throws ResourceCollectionException, TransformerException, ReporterException, ConfigurationException, IOException {

        // mock provider.list() -> return resource list that does not contain a meta_study file
        Resource[] studyFiles = new Resource[] {TestUtils.createMockResource("file:/dummy_study_folder/file_that_needs_transformation.txt", 0)};
        when(provider.list(isA(Resource.class))).thenReturn(studyFiles);

        when(transformerService.transform(any(),any(),any())).thenReturn(ExitStatus.WARNING);

        Map<String, ExitStatus> exitStatus = transformer.transform("dummy-timestamp", dummyStudyInput(), "");

        assert(exitStatus.containsKey("dummy_study") && exitStatus.get("dummy_study") == ExitStatus.WARNING);
        assert(transformer.getLogFiles().containsKey("dummy_study loading log"));
        assertFalse(transformer.getValidStudies().containsKey("dummy_study"));
    }

    private Map<String,Resource> dummyStudyInput() {
        Map<String,Resource> studyPaths = new HashMap<>();
        Resource studyPath = TestUtils.createMockResource("file:/dummy_study_folder/", 0);
        studyPaths.put("dummy_study", studyPath);
        return studyPaths;
    }

}
