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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.cbioportal.staging.exceptions.DirectoryCreatorException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { DirectoryCreatorByJob.class, org.cbioportal.staging.services.resource.ResourceUtils.class })
public class DirectoryCreatorTest {

	@Autowired
	private IDirectoryCreator directoryCreator;

	@MockBean
	private ResourceUtils utils;

    private Resource etlDir = mock(Resource.class);
    private Resource transformDir = mock(Resource.class);
    private Resource untransformedDir = mock(Resource.class);


	@Test(expected = DirectoryCreatorException.class)
	public void etlWorkingDir_doesNotExist() throws DirectoryCreatorException {

		when(etlDir.exists()).thenReturn(false);
        ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
        ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

		directoryCreator.createStudyExtractDir("mock-timestamp", "mock-studyId");
	}

	@Test(expected = DirectoryCreatorException.class)
	public void etlWorkingDir_isFile() throws DirectoryCreatorException, ResourceCollectionException {

		when(etlDir.exists()).thenReturn(true);
		when(utils.isFile(any(Resource.class))).thenReturn(true);
		ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
        ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

		directoryCreator.createStudyExtractDir("mock-timestamp", "mock-studyId");
    }

    //@Test(expected = DirectoryCreatorException.class)
	public void transformationDir_doesNotExist() throws DirectoryCreatorException, ResourceCollectionException {

        when(etlDir.exists()).thenReturn(false);
        when(utils.createDirResource(untransformedDir, "staging")).thenReturn(utils.getResource("/staging"));
        ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
        ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

        Resource transformedDir = directoryCreator.createTransformedStudyDir("mock-timestamp", "mock-studyId", untransformedDir);

        assertEquals(transformedDir, utils.getResource("/staging"));
	}

	// //@Test(expected = DirectoryCreatorException.class)
	// public void transformationDir_isFile() throws DirectoryCreatorException, ResourceCollectionException {

	// 	when(etlDir.exists()).thenReturn(true);
	// 	when(utils.isFile(any(Resource.class))).thenReturn(true);
	// 	ReflectionTestUtils.setField(directoryCreator, "etlWorkingDir", etlDir);
    //     ReflectionTestUtils.setField(directoryCreator, "transformationDir", transformDir);

	// 	directoryCreator.createTransformedStudyDir("mock-timestamp", "mock-studyId", untransformedDir);
	// }

}
