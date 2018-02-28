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

import java.io.File;
import java.io.IOException;
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

import freemarker.core.ParseException;
import freemarker.template.MalformedTemplateNameException;
import freemarker.template.TemplateException;
import freemarker.template.TemplateNotFoundException;

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
	public void studyLoaded() throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, IOException, TemplateException {
		ReflectionTestUtils.setField(loader, "emailService", emailService);
		ReflectionTestUtils.setField(loader, "loaderService", loaderService);
		ReflectionTestUtils.setField(loaderService, "testFile", "src/test/resources/loader_tests/example.log");
		File etlWorkingDir = new File("src/test/resources/loader_tests");
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
