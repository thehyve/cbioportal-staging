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
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.when;

import org.cbioportal.staging.etl.Transformer.ExitStatus;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ValidatorException;
import org.cbioportal.staging.services.ValidatorService;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(properties = { "central.share.location=${java.io.tmpdir}" })
@SpringBootTest(classes = org.cbioportal.staging.etl.Validator.class)
public class ValidatorTest {

	@Autowired
	private Validator validator;

	@MockBean
    private ValidatorService validatorService;

    @MockBean
	private ResourceUtils utils;

	@Before
	public void init() throws ResourceCollectionException {
        when(utils.createFileResource(any(Resource.class), any(String.class))).thenReturn(null);
	}

    //Tests for "validate" and its associate methods
	// @Test
	// public void studyPassedValidation() throws ValidatorException, ResourceCollectionException {
    //     when(utils.createLogFile(any(String.class), any(Resource.class), any(String.class))).thenReturn(null);

	// 	ReflectionTestUtils.setField(validator, "validatorService", validatorService);
	// 	ReflectionTestUtils.setField(validatorService, "exitStatus", ExitStatus.WARNINGS);

    //     ReflectionTestUtils.setField(validator, "validationLevel", "ERROR");

    //     Map<String, Resource> studies = new HashMap<>();
    //     studies.put("lgg_ucsf_2014", utils.getResource("/path"));
    //     Map<String, ExitStatus> validatedStudies = validator.validate(studies);
    //     Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
    //     expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.WARNINGS);
	// 	assertEquals(expectedValidatedStudies, validatedStudies); //The study has passed validation,
	// }

	// @Test
	// public void studyFailedValidation() throws ValidatorException, ResourceCollectionException {
    //     when(utils.createLogFile(any(String.class), any(Resource.class), any(String.class))).thenReturn(null);

	// 	ReflectionTestUtils.setField(validator, "validatorService", validatorService);
	// 	ReflectionTestUtils.setField(validatorService, "exitStatus", ExitStatus.ERRORS);

	// 	ReflectionTestUtils.setField(validator, "validationLevel", "ERROR");

    //     Map<String, Resource> studies = new HashMap<>();
    //     studies.put("lgg_ucsf_2014", utils.getResource("/path"));
    //     Map<String, ExitStatus> validatedStudies = validator.validate(studies);
    //     Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
    //     expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.ERRORS);
	// 	assertEquals(expectedValidatedStudies, validatedStudies); //The study added has failed validation
	// }

    //Tests for "hasStudyPassed" method
	@Test
	public void studyHasPassedValidationNoWarnings() throws ValidatorException, ResourceCollectionException {

		boolean result = validator.hasStudyPassed("study", "WARNING", ExitStatus.SUCCESS);

		assertEquals(true, result);
    }

    @Test
	public void studyHasFailedValidationWarningsWarning() throws ValidatorException, ResourceCollectionException {

		boolean result = validator.hasStudyPassed("study", "WARNING", ExitStatus.WARNINGS);

		assertEquals(false, result);
	}

	@Test
	public void studyHasFailedValidationWarningsError() throws ValidatorException, ResourceCollectionException {

		boolean result = validator.hasStudyPassed("study", "WARNING", ExitStatus.ERRORS);

		assertEquals(false, result);
    }

    @Test
	public void studyHasPassedValidationErrorLevel() throws ValidatorException, ResourceCollectionException {

		boolean result = validator.hasStudyPassed("study", "ERROR", ExitStatus.SUCCESS);

		assertEquals(true, result);
	}

	@Test
	public void studyHasPassedValidationWithWarnings() throws ValidatorException, ResourceCollectionException {

		boolean result = validator.hasStudyPassed("study", "ERROR", ExitStatus.WARNINGS);

		assertEquals(true, result);
	}

	@Test
	public void studyHasPassedFailedWithErrors() throws ValidatorException, ResourceCollectionException {

		boolean result = validator.hasStudyPassed("study", "ERROR", ExitStatus.ERRORS);

		assertEquals(false, result);
	}

	@Test(expected=ValidatorException.class)
	public void studyHasPassedWrongLevel() throws ValidatorException, ResourceCollectionException {

		boolean result = validator.hasStudyPassed("study", "WRONG_LEVEL", ExitStatus.ERRORS);

		assertEquals(null, result);
    }
}
