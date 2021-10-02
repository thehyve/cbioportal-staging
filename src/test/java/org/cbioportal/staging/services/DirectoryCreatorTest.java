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
package org.cbioportal.staging.services;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import java.io.IOException;
import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.DirectoryCreatorException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.directory.DirectoryCreator;
import org.cbioportal.staging.services.directory.IDirectoryCreator;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.cbioportal.staging.services.resource.Study;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
@SpringBootTest(
	classes = { DirectoryCreator.class },
	properties = {
		"etl.working.dir=",
		"transformation.directory="
	}
)
public class DirectoryCreatorTest {

	@Autowired
	private IDirectoryCreator directoryCreator;

	@SpyBean
	private ResourceUtils utils;

	private Resource etlDir = mock(Resource.class);
	private Resource transformDir = mock(Resource.class);
	private Resource untransformedDir = mock(Resource.class);
	private Resource studyDir = mock(Resource.class);

	@Before
	public void init() {
		ReflectionTestUtils.setField(directoryCreator, "dirFormat", "timestamp/study_id");
	}

	@Test(expected = DirectoryCreatorException.class)
	public void etlWorkingDir_doesNotExist() throws DirectoryCreatorException {

		when(etlDir.exists()).thenReturn(false);
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
		ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

		directoryCreator.createStudyExtractDir(studyDir);
	}

	@Test(expected = DirectoryCreatorException.class)
	public void etlWorkingDir_isFile()
			throws DirectoryCreatorException, ResourceCollectionException, ResourceUtilsException {

		when(etlDir.exists()).thenReturn(true);
		doReturn(true).when(utils).isFile(isA(Resource.class));
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
        ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

		directoryCreator.createStudyExtractDir(studyDir);
    }

    @Test
	public void etlWorkingDir_isCorrect()
			throws DirectoryCreatorException, ResourceUtilsException, IOException {

		when(etlDir.exists()).thenReturn(true);
		doReturn(false).when(utils).isFile(isA(Resource.class));
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
        ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

        Resource expectedStudyDir = TestUtils.createMockResource("file:/studyId", 0);
		doReturn(expectedStudyDir).when(utils).createDirResource(isA(Resource.class));

        Resource generatedStudyDir = directoryCreator.createStudyExtractDir(studyDir);

        assertEquals(expectedStudyDir, generatedStudyDir);
	}

    @Test
	public void etlWorkingDir_calledWithStudyIdAndVersion()
			throws DirectoryCreatorException, ResourceUtilsException, IOException {

		ReflectionTestUtils.setField(directoryCreator, "dirFormat", "study_id/study_version");

		when(etlDir.exists()).thenReturn(true);
		doReturn(false).when(utils).isFile(isA(Resource.class));
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
        ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

        Resource expectedStudyDir = TestUtils.createMockResource("file:/studyId", 0);
		doReturn(expectedStudyDir).when(utils).createDirResource(isA(Resource.class));

        Resource generatedStudyDir = directoryCreator.createStudyExtractDir(studyDir);

        assertEquals(expectedStudyDir, generatedStudyDir);
    }

    @Test
	public void etlWorkingDir_calledWithStudyIdAndTimeStamp()
			throws DirectoryCreatorException, ResourceUtilsException, IOException {

		ReflectionTestUtils.setField(directoryCreator, "dirFormat", "study_id/timestamp");

		when(etlDir.exists()).thenReturn(true);
		doReturn(false).when(utils).isFile(isA(Resource.class));
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
        ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

        Resource expectedStudyDir = TestUtils.createMockResource("file:/studyId", 0);
		doReturn(expectedStudyDir).when(utils).createDirResource(isA(Resource.class));

        Resource generatedStudyDir = directoryCreator.createStudyExtractDir(studyDir);

        assertEquals(expectedStudyDir, generatedStudyDir);
    }

    @Test(expected = DirectoryCreatorException.class)
	public void transformationDir_transformationDirNull()
		throws DirectoryCreatorException, ResourceUtilsException, IOException {
		doThrow(new IOException()).when(utils).createDirResource(isNull());
        directoryCreator.createTransformedStudyDir(null);
	}

	@Test
	public void getStudyTransformDir_Null() throws DirectoryCreatorException, IOException {
		ReflectionTestUtils.setField(directoryCreator, "transformationDir", null);
		Study dummyStudy = new Study("dummy-study", "dummy-time", "dummy-time", new FileSystemResource("/"), null);
		Resource studyTransformDir = directoryCreator.getStudyTransformDir(dummyStudy);
		assertEquals("/staging/dummy-time/dummy-study", studyTransformDir.getFile().getAbsolutePath());
	}

	@Test
	public void getStudyTransformDir_Dir() throws DirectoryCreatorException, IOException {
		ReflectionTestUtils.setField(directoryCreator, "transformationDir", new FileSystemResource("/different-transform-dir/"));
		Study dummyStudy = new Study("dummy-study", "dummy-time", "dummy-time", new FileSystemResource("/"), null);
		Resource studyTransformDir = directoryCreator.getStudyTransformDir(dummyStudy);
		assertEquals("/different-transform-dir/dummy-time/dummy-study", studyTransformDir.getFile().getAbsolutePath());
	}

	@Test(expected = DirectoryCreatorException.class)
	public void transformationDirException()
		throws DirectoryCreatorException, ResourceUtilsException, IOException {
		doThrow(new IOException()).when(utils).createDirResource(isA(Resource.class));
		directoryCreator.createTransformedStudyDir(studyDir);
    }

	@Test
	public void transformationDir_isCorrect()
			throws DirectoryCreatorException, ResourceUtilsException, IOException {

		when(etlDir.exists()).thenReturn(false);
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
		ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

		Resource transDir = TestUtils.createMockResource("file:/transf", 0);
		doReturn(transDir).when(utils).createDirResource(isA(Resource.class));

		Resource transformedDir = directoryCreator.createTransformedStudyDir(studyDir);

        assertEquals(transDir, transformedDir);
	}

    @Test
	public void transformationDir_calledWithStudyIdAndVersion()
			throws DirectoryCreatorException, ResourceUtilsException, IOException {

		ReflectionTestUtils.setField(directoryCreator, "dirFormat", "study_id/study_version");

		when(etlDir.exists()).thenReturn(false);
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
        ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

		Resource transDir = TestUtils.createMockResource("file:/transf", 0);
		doReturn(transDir).when(utils).createDirResource(isA(Resource.class));

        Resource transformedDir = directoryCreator.createTransformedStudyDir(studyDir);

        assertEquals(transDir, transformedDir);
    }

    @Test
	public void transformationDir_calledWithStudyIdAndTimeStamp()
			throws DirectoryCreatorException, ResourceUtilsException, IOException {

		ReflectionTestUtils.setField(directoryCreator, "dirFormat", "study_id/timestamp");

		when(etlDir.exists()).thenReturn(false);
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
        ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

		Resource transDir = TestUtils.createMockResource("file:/transf", 0);
		doReturn(transDir).when(utils).createDirResource(isA(Resource.class));

        Resource transformedDir = directoryCreator.createTransformedStudyDir(studyDir);

        assertEquals(transDir, transformedDir);
    }
}
