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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {org.cbioportal.staging.etl.Validator.class,
		org.cbioportal.staging.etl.EmailServiceMockupImpl.class,
        org.cbioportal.staging.etl.ValidationServiceMockupImpl.class})
@TestPropertySource(
	properties= {
		"central.share.location=${java.io.tmpdir}"
	}
)
@SpringBootTest
public class ValidatorTest {

	@Autowired
	private Validator validator;

	@Autowired
	private EmailServiceMockupImpl emailService;

	@Autowired
    private ValidationServiceMockupImpl validationService;

	@Before
	public void setUp() throws Exception {
		emailService.reset();
        validationService.reset();
	}

	@Test
	public void studyHasPassedValidationNoWarnings() {
		boolean result = validator.hasStudyPassed("study", "WARNING", 0);

		//Build the expected outcome and check that is the same as the function output
		assertEquals(true, result);
	}

	@Test
	public void studyHasPassedValidationWithWarnings() {
		boolean result = validator.hasStudyPassed("study", "ERROR", 3);

		//Build the expected outcome and check that is the same as the function output
		assertEquals(true, result);
	}

	@Test
	public void studyHasFailedValidationWarningsWarning() {
		boolean result = validator.hasStudyPassed("study", "WARNING", 3);

		//Build the expected outcome and check that is the same as the function output
		assertEquals(false, result);
	}

	@Test
	public void studyHasFailedValidationWarningsError() {
		boolean result = validator.hasStudyPassed("study", "WARNING", 1);

		//Build the expected outcome and check that is the same as the function output
		assertEquals(false, result);
	}

	@Test
	public void studyHasPassedFailedWithErrors() {
		boolean result = validator.hasStudyPassed("study", "ERROR", 1);

		//Build the expected outcome and check that is the same as the function output
		assertEquals(false, result);
	}

	@Test(expected=IllegalArgumentException.class)
	public void studyHasPassedWrongLevel() {
		ReflectionTestUtils.setField(validator, "emailService", emailService);

		boolean result = validator.hasStudyPassed("study", "WRONG_LEVEL", 1);

		//Build the expected outcome and check that is the same as the function output
		assertEquals(null, result);
	}

	@Test
	public void studyPassedValidation() throws Exception {
		ReflectionTestUtils.setField(validator, "emailService", emailService);
		ReflectionTestUtils.setField(validator, "validationService", validationService);
		ReflectionTestUtils.setField(validationService, "throwError", false);
		ReflectionTestUtils.setField(validationService, "exitStatus", 3);

		ReflectionTestUtils.setField(validator, "etlWorkingDir", "src/test/resources/validator_tests");
        ReflectionTestUtils.setField(validator, "validationLevel", "ERROR");

        Map<String, String> filesPaths = new HashMap<String, String>();
        Map<String, File> studies = new HashMap<String, File>();
        studies.put("lgg_ucsf_2014", new File("/path"));
        String date = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
		Map<String, File> validatedStudies = validator.validate(date, studies, filesPaths);
		assertEquals(studies, validatedStudies); //The study passed has passed validation,

		//Check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(true, emailService.isEmailValidationReportSent());
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(false, emailService.isEmailGenericErrorSent());
	}

	@Test
	public void studyFailedValidation() throws Exception {
		ReflectionTestUtils.setField(validator, "emailService", emailService);
		ReflectionTestUtils.setField(validator, "validationService", validationService);
		ReflectionTestUtils.setField(validationService, "throwError", false);
		ReflectionTestUtils.setField(validationService, "exitStatus", 1);

		ReflectionTestUtils.setField(validator, "etlWorkingDir", "src/test/resources/validator_tests");
		ReflectionTestUtils.setField(validator, "validationLevel", "ERROR");

		Map<String, String> filesPaths = new HashMap<String, String>();
        Map<String, File> studies = new HashMap<String, File>();
        studies.put("lgg_ucsf_2014", new File("/path"));
        String date = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
		Map<String, File> validatedStudies = validator.validate(date, studies, filesPaths);
		assertEquals(0, validatedStudies.size()); //The study added has failed validation, is not going to be loaded

		//Check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(true, emailService.isEmailValidationReportSent());
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(false, emailService.isEmailGenericErrorSent());
	}

	@Test
	public void validationErrorEmailSent() throws Exception {
		ReflectionTestUtils.setField(validator, "emailService", emailService);
		ReflectionTestUtils.setField(validator, "validationService", validationService);
		ReflectionTestUtils.setField(validationService, "throwError", true);

		ReflectionTestUtils.setField(validator, "etlWorkingDir", "src/test/resources/validator_tests");


		Map<String, String> filesPaths = new HashMap<String, String>();
        Map<String, File> studies = new HashMap<String, File>();
        studies.put("lgg_ucsf_2014", new File("/path"));
        String date = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
		Map<String, File> validatedStudies = validator.validate(date, studies, filesPaths);
		assertEquals(0, validatedStudies.size());

		//Check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(false, emailService.isEmailValidationReportSent());
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(true, emailService.isEmailGenericErrorSent());
	}
}
