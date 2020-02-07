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

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.DirectoryCreatorException;
import org.cbioportal.staging.exceptions.ExtractionException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.IDirectoryCreator;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { Extractor.class })
public class ExtractorTest {

	@Autowired
	private Extractor extractor;

	@SpyBean
	private ResourceUtils utils;

	@MockBean
	private IDirectoryCreator directoryCreator;

	@Test
	public void testRun_success() throws DirectoryCreatorException, ResourceCollectionException, IOException,
			ExtractionException, ResourceUtilsException {

		Resource targetDir = TestUtils.createMockResource("file:/extract-dir/dummy-study", 0);
		when(directoryCreator.createStudyExtractDir(eq("dummy-time"), eq("dummy-study"))).thenReturn(targetDir);

		Resource remoteFile1 = TestUtils.createMockResource("file:/file1.txt", 0);
		Resource remoteFile2 = TestUtils.createMockResource("file:/file2.txt", 0);
		Map<String,Resource[]> remoteResources = new HashMap<>();
		remoteResources.put("dummy-study", new Resource[] {remoteFile1, remoteFile2} );

		Resource localFile1 = TestUtils.createMockResource("file:/file1.txt", 0);
		Resource localFile2 = TestUtils.createMockResource("file:/file2.txt", 0);
		doReturn(localFile1).when(utils).copyResource(any(Resource.class), any(Resource.class), eq("/file1.txt"));
		doReturn(localFile2).when(utils).copyResource(any(Resource.class), any(Resource.class), eq("/file2.txt"));

		Map<String, Resource> extractedResources = extractor.run(remoteResources, "dummy-time");

		assert(extractor.errorFiles().isEmpty());
		assert(extractedResources.containsKey("dummy-study"));
		assert(extractedResources.get("dummy-study").getURL().toString().equals("file:/extract-dir/dummy-study"));
		verify(utils, times(2)).copyResource(any(Resource.class), any(Resource.class), anyString());

	}

	@Test
	public void testRun_fileFail()
			throws DirectoryCreatorException, ResourceCollectionException, IOException, ExtractionException,
			ResourceUtilsException {

		Resource targetDir = TestUtils.createMockResource("file:/extract-dir/dummy-study", 0);
		when(directoryCreator.createStudyExtractDir(eq("dummy-time"), eq("dummy-study"))).thenReturn(targetDir);

		Resource remoteFile1 = TestUtils.createMockResource("file:/file1.txt", 0);
		Resource remoteFile2 = TestUtils.createMockResource("file:/file2.txt", 0);
		Map<String,Resource[]> remoteResources = new HashMap<>();
		remoteResources.put("dummy-study", new Resource[] {remoteFile1, remoteFile2} );

		Resource localFile1 = TestUtils.createMockResource("file:/file1.txt", 0);
		doReturn(localFile1).when(utils).copyResource(any(Resource.class), any(Resource.class), eq("/file1.txt"));
		doReturn(null).when(utils).copyResource(any(Resource.class), any(Resource.class), eq("/file2.txt"));

		Map<String, Resource> extractedResources = extractor.run(remoteResources, "dummy-time");

		assert(extractor.errorFiles().containsKey("dummy-study"));
		assert(extractor.errorFiles().get("dummy-study").contains("file:/file2.txt"));
		assert(extractedResources.isEmpty());
		verify(utils, times(2)).copyResource(any(Resource.class), any(Resource.class), anyString());

	}

	@Test(expected = ExtractionException.class)
	public void testRun_nullResources() throws ExtractionException {
		extractor.run(null, "dummy-time");
	}

	@Test(expected = ExtractionException.class)
	public void testRun_nullTimeStamp() throws ExtractionException {
		extractor.run(new HashMap<>(), null);
	}

}
