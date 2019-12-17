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

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {org.cbioportal.staging.etl.LocalExtractor.class,
		org.cbioportal.staging.etl.EmailServiceMockupImpl.class})
@SpringBootTest
@Import(MyTestConfiguration.class)

public class LocalExtractorTest {
    
    @Autowired
	private LocalExtractor localExtractor;
	
	@Autowired
	private EmailServiceMockupImpl emailService;
		
	@Before
    public void setUp() throws Exception {
        emailService.reset();
    }
	
	@Rule
    public TemporaryFolder etlWorkingDir = new TemporaryFolder();

	@Test
	public void extractInWorkingDirFilesNotFound() throws InterruptedException, IOException, ConfigurationException {
		ReflectionTestUtils.setField(localExtractor, "emailService", emailService);
		ReflectionTestUtils.setField(localExtractor, "etlWorkingDir", etlWorkingDir.getRoot().toString());
		ReflectionTestUtils.setField(localExtractor, "timeRetry", 0);

		//Run extractor step:
        Integer id = localExtractor.getNewId(etlWorkingDir.getRoot());
        ArrayList<File> directories = new ArrayList<File>();
        directories.add(new File("/non/existing/path"));
		localExtractor.extractInWorkingDir(directories, id);

		//check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(true, emailService.isEmailStudyFileNotFoundSent()); //Email is sent since a file does not exist
		assertEquals(false, emailService.isEmailValidationReportSent());
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(false, emailService.isEmailGenericErrorSent());		
    }

    @Test
	public void extractInWorkingDirFilesFound() throws InterruptedException, IOException, ConfigurationException {
		ReflectionTestUtils.setField(localExtractor, "emailService", emailService);
		ReflectionTestUtils.setField(localExtractor, "etlWorkingDir", etlWorkingDir.getRoot().toString());
		ReflectionTestUtils.setField(localExtractor, "timeRetry", 0);

		//Run extractor step:
        Integer id = localExtractor.getNewId(etlWorkingDir.getRoot());
        File relativeDirectory = new File("src/test/resources/transformer_tests/study2");
        ArrayList<File> directories = new ArrayList<File>();
        directories.add(new File(relativeDirectory.getAbsolutePath()));
		Map<String, File> result = localExtractor.extractInWorkingDir(directories, id);

		//check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(false, emailService.isEmailValidationReportSent());
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(false, emailService.isEmailGenericErrorSent());
		
        //Build the expected outcome and check that is the same as the function output
		Map<String, File> expectedResult = new HashMap<String, File>();
		expectedResult.put("study2", new File(etlWorkingDir.getRoot().toString()+"/"+id+"/study2"));
		assertEquals(expectedResult, result);
    }

    @Test
	public void extractWithoutWorkingDir() throws InterruptedException, IOException, ConfigurationException {
		ReflectionTestUtils.setField(localExtractor, "emailService", emailService);

		//Run extractor step:
        File relativeDirectory = new File("src/test/resources/transformer_tests/study2");
        ArrayList<File> directories = new ArrayList<File>();
        directories.add(new File(relativeDirectory.getAbsolutePath()));
		Map<String, File> result = localExtractor.extractWithoutWorkingDir(directories);
		
        //Build the expected outcome and check that is the same as the function output
		Map<String, File> expectedResult = new HashMap<String, File>();
		expectedResult.put("study2", new File(relativeDirectory.getAbsolutePath()));
		assertEquals(expectedResult, result);
    }
}
