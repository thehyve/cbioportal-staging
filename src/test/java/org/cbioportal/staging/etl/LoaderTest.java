/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.etl;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {org.cbioportal.staging.etl.Loader.class, 
		org.cbioportal.staging.etl.EmailServiceMockupImpl.class,
		org.cbioportal.staging.etl.LoaderServiceMockupImpl.class})
@SpringBootTest
@Import(MyTestConfiguration.class)

public class LoaderTest {
	
	@Autowired
	private Loader loader;
	
	@Autowired
	private EmailServiceMockupImpl emailService;
	
	@Autowired
	private LoaderServiceMockupImpl loaderService;
	
	@Before
	public void setUp() throws Exception {
		emailService.reset();
	}
	
	@Test
	public void noCentralShareLocation() {
		ReflectionTestUtils.setField(loader, "emailService", emailService);
		File notfound = new File("notfound");
		File etlWorkingDir = new File("src/test/resources/loader_tests");
		ReflectionTestUtils.setField(loader, "centralShareLocation", notfound);
		ReflectionTestUtils.setField(loader, "etlWorkingDir", etlWorkingDir);

		List<String> studies = new ArrayList<String>();
		studies.add("lgg_ucsf_2014");
		loader.load(0, studies);
		
		//Check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(false, emailService.isEmailValidationReportSent());
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(true, emailService.isEmailGenericErrorSent()); //Email is sent since there is a "generic" error
	}
	
	@Test
	public void studyLoaded() {
		ReflectionTestUtils.setField(loader, "emailService", emailService);
		ReflectionTestUtils.setField(loader, "loaderService", loaderService);
		ReflectionTestUtils.setField(loaderService, "testFile", "src/test/resources/loader_tests/example.log");

		File etlWorkingDir = new File("src/test/resources/loader_tests");
		ReflectionTestUtils.setField(loader, "centralShareLocation", etlWorkingDir);
		ReflectionTestUtils.setField(loader, "etlWorkingDir", etlWorkingDir);

		List<String> studies = new ArrayList<String>();
		studies.add("lgg_ucsf_2014");
		loader.load(0, studies);
		
		//Check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(false, emailService.isEmailValidationReportSent());
		assertEquals(true, emailService.isEmailStudiesLoadedSent());
		assertEquals(false, emailService.isEmailGenericErrorSent()); 
	}
}
