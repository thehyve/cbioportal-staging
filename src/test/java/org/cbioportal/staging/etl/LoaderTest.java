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
import java.util.Map;
import java.util.HashMap;

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
        org.cbioportal.staging.etl.LoaderServiceMockupImpl.class,
        org.cbioportal.staging.etl.ValidationServiceMockupImpl.class})
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
        loaderService.reset();
	}
     
    @Test
	public void studyLoaded() throws Exception {
		ReflectionTestUtils.setField(loader, "emailService", emailService);
		ReflectionTestUtils.setField(loader, "loaderService", loaderService);
		ReflectionTestUtils.setField(loaderService, "throwError", false);
		ReflectionTestUtils.setField(loaderService, "exitStatus", 0);

        Map<String, File> studies = new HashMap<String, File>();
        Map<String, String> studyStatus = new HashMap<String, String>();
		studies.put("lgg_ucsf_2014", new File("test/path"));
		Boolean result = loader.load(0, studies, studyStatus);
		assertEquals(true, result); //The study has been loaded
		
		//Check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(false, emailService.isEmailValidationReportSent());
		assertEquals(true, emailService.isEmailStudiesLoadedSent());
        assertEquals(false, emailService.isEmailGenericErrorSent());
    }
    
    @Test
	public void studyNotLoaded() throws Exception {
		ReflectionTestUtils.setField(loader, "emailService", emailService);
		ReflectionTestUtils.setField(loader, "loaderService", loaderService);
		ReflectionTestUtils.setField(loaderService, "throwError", false);
		ReflectionTestUtils.setField(loaderService, "exitStatus", 1);

        Map<String, File> studies = new HashMap<String, File>();
        Map<String, String> studyStatus = new HashMap<String, String>();
		studies.put("lgg_ucsf_2014", new File("test/path"));
		Boolean result = loader.load(0, studies, studyStatus);
		assertEquals(false, result); //The study has not been loaded
		
		//Check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(false, emailService.isEmailValidationReportSent());
		assertEquals(true, emailService.isEmailStudiesLoadedSent());
        assertEquals(false, emailService.isEmailGenericErrorSent());
    } 
}
