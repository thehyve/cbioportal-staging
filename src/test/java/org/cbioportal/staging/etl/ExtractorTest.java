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
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.stream.Stream;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.DirectoryCreatorException;
import org.cbioportal.staging.exceptions.ExtractionException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.directory.IDirectoryCreator;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.cbioportal.staging.services.resource.Study;
import org.cbioportal.staging.services.resource.filesystem.FileSystemResourceProvider;
import org.cbioportal.staging.services.resource.filesystem.IFileSystemGateway;
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
	classes = { Extractor.class, ResourceUtils.class, FileSystemResourceProvider.class }
)
public class ExtractorTest {

	@Autowired
	private Extractor extractor;

	@SpyBean
	private FileSystemResourceProvider provider;

	@MockBean
	private IDirectoryCreator directoryCreator;

	@MockBean
	private IFileSystemGateway gateway;

	@Test
	public void testRun_success() throws DirectoryCreatorException, ResourceCollectionException, IOException,
			ExtractionException {

		Resource targetDir = TestUtils.createMockResource("file:/extract-dir/dummy-study", 0);
		when(directoryCreator.createStudyExtractDir(any(Resource.class))).thenReturn(targetDir);

		Resource remoteFile1 = TestUtils.createMockResource("file:/file1.txt", 0);
		Resource remoteFile2 = TestUtils.createMockResource("file:/file2.txt", 0);

		Study[] dummyStudies = new Study[] {new Study("dummy-study", "dummy-time",
		 "dummy-time", mock(Resource.class), new Resource[] {remoteFile1, remoteFile2})};

		Resource localFile1 = TestUtils.createMockResource("file:/file1.txt", 0);
		Resource localFile2 = TestUtils.createMockResource("file:/file2.txt", 0);
		doAnswer(invocation -> {
            String fileName = invocation.getArgument(1, Resource.class).getFilename();
            if (fileName.equals("file1.txt")) {
                return localFile1;
			}
			if (fileName.equals("file2.txt")) {
                return localFile2;
			}
            return null;
        }).when(provider).copyFromRemote(isA(Resource.class), isA(Resource.class));

		Study[] extractedResources = extractor.run(dummyStudies);

		assertTrue(extractor.errorFiles().isEmpty());
		assertTrue(Stream.of(extractedResources).filter(s -> s.getStudyId().equals("dummy-study")).findAny().isPresent());

		Study extractedStudy = extractedResources[0];
		assertEquals("file:/extract-dir/dummy-study", extractedStudy.getStudyDir().getURI().toString());
		verify(provider, times(2)).copyFromRemote(isA(Resource.class), isA(Resource.class));

	}

	@Test
	public void testRun_fileFail()
			throws DirectoryCreatorException, ResourceCollectionException, ExtractionException {

		Resource targetDir = TestUtils.createMockResource("file:/extract-dir/dummy-study", 0);
		when(directoryCreator.createStudyExtractDir(any(Resource.class))).thenReturn(targetDir);

		Resource remoteFile1 = TestUtils.createMockResource("file:/file1.txt", 0);
		Resource remoteFile2 = TestUtils.createMockResource("file:/file2.txt", 0);

		Study[] dummyStudies = new Study[] {new Study("dummy-study", "dummy-time",
		 "dummy-time",  mock(Resource.class), new Resource[] {remoteFile1, remoteFile2})};

		Resource localFile1 = TestUtils.createMockResource("file:/file1.txt", 0);
		doAnswer(invocation -> {
            String fileName = invocation.getArgument(1, Resource.class).getFilename();
            if (fileName.equals("file1.txt")) {
                return localFile1;
			}
            return null;
        }).when(provider).copyFromRemote(isA(Resource.class), isA(Resource.class));

		Study[] extractedResources = extractor.run(dummyStudies);

		assertTrue(extractor.errorFiles().containsKey("dummy-study"));
		assertTrue(extractor.errorFiles().get("dummy-study").contains("file:/file2.txt"));
		assertEquals(0, extractedResources.length);
		verify(provider, times(2)).copyFromRemote(isA(Resource.class), isA(Resource.class));

	}

	@Test(expected = ExtractionException.class)
	public void testRun_nullResources() throws ExtractionException {
		extractor.run(null);
	}

}
