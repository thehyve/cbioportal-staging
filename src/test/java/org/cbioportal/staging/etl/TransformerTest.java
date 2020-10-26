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

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.*;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.directory.DirectoryCreator;
import org.cbioportal.staging.services.directory.IDirectoryCreator;
import org.cbioportal.staging.services.etl.TransformerServiceImpl;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.cbioportal.staging.services.resource.Study;
import org.cbioportal.staging.services.resource.filesystem.FileSystemResourceProvider;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { Transformer.class, TransformerServiceImpl.class,
        ResourceUtils.class, DirectoryCreator.class })
public class TransformerTest {

    @Autowired
    private Transformer transformer;

    @MockBean
    private TransformerServiceImpl transformerService;

    @MockBean
    private ResourceUtils utils;

    @MockBean
    private FileSystemResourceProvider resourceProvider;

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
        when(directoryCreator.createTransformedStudyDir(any(Study.class),isA(Resource.class))).thenReturn(null);

        // mock utils.createLogFiles -> return Resource mock that has getFile()
        Resource logFile = TestUtils.createMockResource("file:/dummy_study_folder/log_file.txt", 0);
        when(logFile.getFile()).thenReturn(null);
        when(utils.createFileResource(isA(Resource.class), anyString())).thenReturn(logFile);

    }

    @Test
    public void testTransform_studyWithMetaStudyFile_checkMetaFile() throws ResourceCollectionException, TransformerException, ReporterException, ConfigurationException, IOException, ResourceUtilsException, DirectoryCreatorException {

        ReflectionTestUtils.setField(transformer, "transformationMetaFileCheck", true);
        // mock provider.list() -> return resource list that contains a meta_study file
        Resource[] studyFiles = new Resource[] {TestUtils.createMockResource("file:/dummy_study_folder/meta_study.txt", 0)};
        // when(directoryCreator.createTransformedStudyDir(isA(Study.class),isA(Resource.class))).thenReturn(studyFiles[0]);
        when(resourceProvider.list(isA(Resource.class))).thenReturn(studyFiles);
        // when(transformer.metaFileExists(null)).thenReturn(false);

		Study dummyStudy = new Study("dummy-study", "dummy-time", "dummy-time", studyFiles[0], studyFiles);


        Map<Study, ExitStatus> exitStatus = transformer.transform(new Study[] {dummyStudy});

        verify(utils, times(1)).copyDirectory(any(),any());
        verify(transformerService, never()).transform(isA(Resource.class),isA(Resource.class),isA(Resource.class));
        assertTrue(exitStatus.containsKey(dummyStudy) && exitStatus.get(dummyStudy) == ExitStatus.SKIPPED);
        assertTrue(transformer.getLogFiles().containsKey(dummyStudy));
        assertTrue(TestUtils.has(transformer.getValidStudies(), dummyStudy.getStudyId()));
    }

    @Test
    public void testTransform_studyWithMetaStudyFile_noCheckMetaFile() throws ResourceCollectionException, TransformerException, ReporterException, ConfigurationException, IOException, ResourceUtilsException, DirectoryCreatorException {

        ReflectionTestUtils.setField(transformer, "transformationMetaFileCheck", false);
        // mock provider.list() -> return resource list that contains a meta_study file
        Resource[] studyFiles = new Resource[] {TestUtils.createMockResource("file:/dummy_study_folder/meta_study.txt", 0)};
        // when(directoryCreator.createTransformedStudyDir(isA(Study.class),isA(Resource.class))).thenReturn(studyFiles[0]);
        when(resourceProvider.list(isA(Resource.class))).thenReturn(studyFiles);
        // when(transformer.metaFileExists(null)).thenReturn(false);

		Study dummyStudy = new Study("dummy-study", "dummy-time", "dummy-time", studyFiles[0], studyFiles);


        Map<Study, ExitStatus> exitStatus = transformer.transform(new Study[] {dummyStudy});

        verify(utils, never()).copyDirectory(any(),any());
        verify(transformerService, times(1)).transform(any(),any(),any()); //Skips the checking of the meta file so it attempts to transform
        assertTrue(exitStatus.containsKey(dummyStudy) && exitStatus.get(dummyStudy) == ExitStatus.SUCCESS); //Returns SUCCESS due to the mock
        assertTrue(transformer.getLogFiles().containsKey(dummyStudy));
        assertTrue(TestUtils.has(transformer.getValidStudies(), dummyStudy.getStudyId()));
    }

    @Test
    public void testTransform_studyThatNeedsTransformation() throws ResourceCollectionException, TransformerException, ReporterException, ConfigurationException, IOException, ResourceUtilsException {

        // mock provider.list() -> return resource list that does not contain a meta_study file
        Resource[] studyFiles = new Resource[] {TestUtils.createMockResource("file:/dummy_study_folder/file_that_needs_transformation.txt", 0)};
        when(resourceProvider.list(isA(Resource.class))).thenReturn(studyFiles);

        Study dummyStudy = new Study("dummy-study", "dummy-time", "dummy-time", studyFiles[0], studyFiles);

        Map<Study, ExitStatus> exitStatus = transformer.transform(new Study[] {dummyStudy});

        verify(utils, never()).copyDirectory(any(),any());
        verify(transformerService, times(1)).transform(any(),any(),any());
        assertTrue(exitStatus.containsKey(dummyStudy) && exitStatus.get(dummyStudy) == ExitStatus.SUCCESS);
        assertTrue(transformer.getLogFiles().containsKey(dummyStudy));
        assertTrue(TestUtils.has(transformer.getValidStudies(), dummyStudy.getStudyId()));
    }

    @Test
    public void testTransform_studyWithWarnings()
            throws ResourceCollectionException, TransformerException, ReporterException, ConfigurationException, IOException {

        // mock provider.list() -> return resource list that does not contain a meta_study file
        Resource[] studyFiles = new Resource[] {TestUtils.createMockResource("file:/dummy_study_folder/file_that_needs_transformation.txt", 0)};
        when(resourceProvider.list(isA(Resource.class))).thenReturn(studyFiles);

        Study dummyStudy = new Study("dummy-study", "dummy-time", "dummy-time", studyFiles[0], studyFiles);

        when(transformerService.transform(any(),any(),any())).thenReturn(ExitStatus.WARNING);

        Map<Study, ExitStatus> exitStatus = transformer.transform(new Study[] {dummyStudy});

        assertTrue(exitStatus.containsKey(dummyStudy) && exitStatus.get(dummyStudy) == ExitStatus.WARNING);
        assertTrue(transformer.getLogFiles().containsKey(dummyStudy));
        assertTrue(TestUtils.has(transformer.getValidStudies(), dummyStudy.getStudyId()));
    }

}
