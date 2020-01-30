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
import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.etl.Transformer.ExitStatus;
import org.cbioportal.staging.exceptions.ValidatorException;
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
        org.cbioportal.staging.etl.PublisherServiceMockupImpl.class,
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
	public void studyHasPassedValidationNoWarnings() throws ValidatorException {
		boolean result = validator.hasStudyPassed("study", "WARNING", ExitStatus.SUCCESS);

		//Build the expected outcome and check that is the same as the function output
		assertEquals(true, result);
	}

	@Test
	public void studyHasPassedValidationWithWarnings() throws ValidatorException {
		boolean result = validator.hasStudyPassed("study", "ERROR", ExitStatus.WARNINGS);

		//Build the expected outcome and check that is the same as the function output
		assertEquals(true, result);
	}

	@Test
	public void studyHasFailedValidationWarningsWarning() throws ValidatorException {
		boolean result = validator.hasStudyPassed("study", "WARNING", ExitStatus.WARNINGS);

		//Build the expected outcome and check that is the same as the function output
		assertEquals(false, result);
	}

	@Test
	public void studyHasFailedValidationWarningsError() throws ValidatorException {
		boolean result = validator.hasStudyPassed("study", "WARNING", ExitStatus.ERRORS);

		//Build the expected outcome and check that is the same as the function output
		assertEquals(false, result);
	}

	@Test
	public void studyHasPassedFailedWithErrors() throws ValidatorException {
		boolean result = validator.hasStudyPassed("study", "ERROR", ExitStatus.ERRORS);

		//Build the expected outcome and check that is the same as the function output
		assertEquals(false, result);
	}

	@Test(expected=IllegalArgumentException.class)
	public void studyHasPassedWrongLevel() throws ValidatorException {
		ReflectionTestUtils.setField(validator, "emailService", emailService);

		boolean result = validator.hasStudyPassed("study", "WRONG_LEVEL", ExitStatus.ERRORS);

		//Build the expected outcome and check that is the same as the function output
		assertEquals(null, result);
	}

	@Test
	public void studyPassedValidation() throws ValidatorException {
		ReflectionTestUtils.setField(validator, "validationService", validationService);
		ReflectionTestUtils.setField(validationService, "throwError", false);
		ReflectionTestUtils.setField(validationService, "exitStatus", ExitStatus.WARNINGS);

        ReflectionTestUtils.setField(validator, "validationLevel", "ERROR");

        Map<String, File> studies = new HashMap<String, File>();
        studies.put("lgg_ucsf_2014", new File("/path"));
        Map<String, ExitStatus> validatedStudies = validator.validate(studies, "", "");
        Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
        expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.WARNINGS);
		assertEquals(expectedValidatedStudies, validatedStudies); //The study has passed validation,
	}

	@Test
	public void studyFailedValidation() throws ValidatorException {
		ReflectionTestUtils.setField(validator, "validationService", validationService);
		ReflectionTestUtils.setField(validationService, "throwError", false);
		ReflectionTestUtils.setField(validationService, "exitStatus", ExitStatus.ERRORS);

		ReflectionTestUtils.setField(validator, "validationLevel", "ERROR");

        Map<String, File> studies = new HashMap<String, File>();
        studies.put("lgg_ucsf_2014", new File("/path"));
        Map<String, ExitStatus> validatedStudies = validator.validate(studies, "", "");
        Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
        expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.ERRORS);
		assertEquals(expectedValidatedStudies, validatedStudies); //The study added has failed validation
	}
}
