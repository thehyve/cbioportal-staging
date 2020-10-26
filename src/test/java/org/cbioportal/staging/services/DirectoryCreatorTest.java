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
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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

	@MockBean
	private ResourceUtils utils;

	private Resource etlDir = mock(Resource.class);
	private Resource transformDir = mock(Resource.class);
	private Resource untransformedDir = mock(Resource.class);
	private Study studyStub = new Study("studyId", "version", "timestamp", null, null);

	@Before
	public void init() {
		ReflectionTestUtils.setField(directoryCreator, "dirFormat", "job");
	}

	@Test(expected = DirectoryCreatorException.class)
	public void etlWorkingDir_doesNotExist() throws DirectoryCreatorException {

		when(etlDir.exists()).thenReturn(false);
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
		ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

		directoryCreator.createStudyExtractDir(studyStub);
	}

	@Test(expected = DirectoryCreatorException.class)
	public void etlWorkingDir_isFile()
			throws DirectoryCreatorException, ResourceCollectionException, ResourceUtilsException {

		when(etlDir.exists()).thenReturn(true);
		when(utils.isFile(isA(Resource.class))).thenReturn(true);
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
        ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

		directoryCreator.createStudyExtractDir(studyStub);
    }

    @Test
	public void etlWorkingDir_isCorrect() throws DirectoryCreatorException, ResourceUtilsException {

		when(etlDir.exists()).thenReturn(true);
		when(utils.isFile(isA(Resource.class))).thenReturn(false);
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
        ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

        Resource expectedStudyDir = TestUtils.createMockResource("file:/studyId", 0);
		when(utils.createDirResource(isA(Resource.class), isA(String.class))).thenReturn(expectedStudyDir);

        Resource generatedStudyDir = directoryCreator.createStudyExtractDir(studyStub);

        assertEquals(expectedStudyDir, generatedStudyDir);
	}

    @Test
	public void etlWorkingDir_calledWithStudyIdAndVersion() throws DirectoryCreatorException, ResourceUtilsException {

		ReflectionTestUtils.setField(directoryCreator, "dirFormat", "version");

		when(etlDir.exists()).thenReturn(true);
		when(utils.isFile(isA(Resource.class))).thenReturn(false);
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
        ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

        Resource expectedStudyDir = TestUtils.createMockResource("file:/studyId", 0);
		when(utils.createDirResource(isA(Resource.class), isA(String.class))).thenReturn(expectedStudyDir);

        Resource generatedStudyDir = directoryCreator.createStudyExtractDir(studyStub);

        assertEquals(expectedStudyDir, generatedStudyDir);
    }

    @Test
	public void etlWorkingDir_calledWithStudyIdAndTimeStamp() throws DirectoryCreatorException, ResourceUtilsException {

		ReflectionTestUtils.setField(directoryCreator, "dirFormat", "id");

		when(etlDir.exists()).thenReturn(true);
		when(utils.isFile(isA(Resource.class))).thenReturn(false);
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
        ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

        Resource expectedStudyDir = TestUtils.createMockResource("file:/studyId", 0);
		when(utils.createDirResource(isA(Resource.class), isA(String.class))).thenReturn(expectedStudyDir);

        Resource generatedStudyDir = directoryCreator.createStudyExtractDir(studyStub);

        assertEquals(expectedStudyDir, generatedStudyDir);
    }
    @Test
	public void transformationDir_transformationDirNull() throws DirectoryCreatorException, ResourceCollectionException, ResourceUtilsException {

        when(etlDir.exists()).thenReturn(false);
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
        ReflectionTestUtils.setField(directoryCreator, "transformationDir", null);

		Resource transDir = TestUtils.createMockResource("file:/staging/", 0);
		when(utils.createDirResource(untransformedDir, "staging")).thenReturn(transDir);

        Resource transformedDir = directoryCreator.createTransformedStudyDir(studyStub, untransformedDir);

        assertEquals(transDir, transformedDir);
	}

	@Test(expected = DirectoryCreatorException.class)
	public void transformationDir_isFile() throws DirectoryCreatorException, ResourceCollectionException, ResourceUtilsException {

		when(etlDir.exists()).thenReturn(true);
		when(utils.isFile(isA(Resource.class))).thenReturn(true);
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
        ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

		directoryCreator.createTransformedStudyDir(studyStub, untransformedDir);
    }

	@Test
	public void transformationDir_isCorrect() throws DirectoryCreatorException, ResourceUtilsException {


		when(etlDir.exists()).thenReturn(false);
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
		ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

		Resource transDir = TestUtils.createMockResource("file:/transf", 0);
		when(utils.createDirResource(isA(Resource.class), isA(String.class))).thenReturn(transDir);

		Resource transformedDir = directoryCreator.createTransformedStudyDir(studyStub, untransformedDir);

        assertEquals(transDir, transformedDir);
	}

    @Test
	public void transformationDir_calledWithStudyIdAndVersion() throws DirectoryCreatorException, ResourceUtilsException {

		ReflectionTestUtils.setField(directoryCreator, "dirFormat", "version");

		when(etlDir.exists()).thenReturn(false);
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
        ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

		Resource transDir = TestUtils.createMockResource("file:/transf", 0);
		when(utils.createDirResource(isA(Resource.class), isA(String.class))).thenReturn(transDir);

        Resource transformedDir = directoryCreator.createTransformedStudyDir(studyStub, untransformedDir);

        assertEquals(transDir, transformedDir);
    }

    @Test
	public void transformationDir_calledWithStudyIdAndTimeStamp() throws DirectoryCreatorException, ResourceUtilsException {

		ReflectionTestUtils.setField(directoryCreator, "dirFormat", "id");

		when(etlDir.exists()).thenReturn(false);
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
        ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

		Resource transDir = TestUtils.createMockResource("file:/transf", 0);
		when(utils.createDirResource(isA(Resource.class), isA(String.class))).thenReturn(transDir);

        Resource transformedDir = directoryCreator.createTransformedStudyDir(studyStub, untransformedDir);

        assertEquals(transDir, transformedDir);
    }
}
