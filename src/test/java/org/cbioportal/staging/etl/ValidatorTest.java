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

import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ValidatorException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.IValidatorService;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = { "central.share.location=${java.io.tmpdir}" })
@SpringBootTest(classes = org.cbioportal.staging.etl.Validator.class)
public class ValidatorTest {

	@Autowired
	private Validator validator;

	@MockBean
    private IValidatorService validatorService;

    @MockBean
	private ResourceUtils utils;

	@Before
	public void init() throws ResourceCollectionException {
        when(utils.createFileResource(any(Resource.class), any(String.class))).thenReturn(null);
	}

	@Test
	public void studySuccessValidationWithWarningLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(Resource.class), any(Resource.class), any(Resource.class))).thenReturn(ExitStatus.SUCCESS);
        ReflectionTestUtils.setField(validator, "validationLevel", "WARNING");

        Map<String, Resource> studies = new HashMap<>();
        studies.put("lgg_ucsf_2014", null);
        Map<String, ExitStatus> validatedStudies = validator.validate(studies);
        Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
        expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.SUCCESS);
        assertEquals(expectedValidatedStudies, validatedStudies);
        assertEquals(studies, validator.getValidStudies());

        Map<String, Resource> logPaths = new HashMap<>();
        logPaths.put("lgg_ucsf_2014 validation log", null);
        logPaths.put("lgg_ucsf_2014 validation report", null);
        assertEquals(logPaths, validator.getLogAndReportFiles());
    }

    @Test
    public void studyWarningValidationWithWarningLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(Resource.class), any(Resource.class), any(Resource.class))).thenReturn(ExitStatus.WARNINGS);
        ReflectionTestUtils.setField(validator, "validationLevel", "WARNING");

        Map<String, Resource> studies = new HashMap<>();
        studies.put("lgg_ucsf_2014", null);
        Map<String, ExitStatus> validatedStudies = validator.validate(studies);
        Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
        expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.WARNINGS);
        assertEquals(expectedValidatedStudies, validatedStudies);
        assertEquals(new HashMap<>(), validator.getValidStudies());

        Map<String, Resource> logPaths = new HashMap<>();
        logPaths.put("lgg_ucsf_2014 validation log", null);
        logPaths.put("lgg_ucsf_2014 validation report", null);
        assertEquals(logPaths, validator.getLogAndReportFiles());
    }

    @Test
    public void studyErrorValidationWithWarningLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(Resource.class), any(Resource.class), any(Resource.class))).thenReturn(ExitStatus.ERRORS);
        ReflectionTestUtils.setField(validator, "validationLevel", "WARNING");

        Map<String, Resource> studies = new HashMap<>();
        studies.put("lgg_ucsf_2014", null);
        Map<String, ExitStatus> validatedStudies = validator.validate(studies);
        Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
        expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.ERRORS);
        assertEquals(expectedValidatedStudies, validatedStudies);
        assertEquals(new HashMap<>(), validator.getValidStudies());

        Map<String, Resource> logPaths = new HashMap<>();
        logPaths.put("lgg_ucsf_2014 validation log", null);
        logPaths.put("lgg_ucsf_2014 validation report", null);
        assertEquals(logPaths, validator.getLogAndReportFiles());
    }

    @Test
	public void studySuccessValidationWithErrorLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(Resource.class), any(Resource.class), any(Resource.class))).thenReturn(ExitStatus.SUCCESS);
        ReflectionTestUtils.setField(validator, "validationLevel", "ERROR");

        Map<String, Resource> studies = new HashMap<>();
        studies.put("lgg_ucsf_2014", null);
        Map<String, ExitStatus> validatedStudies = validator.validate(studies);
        Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
        expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.SUCCESS);
        assertEquals(expectedValidatedStudies, validatedStudies);
        assertEquals(studies, validator.getValidStudies());

        Map<String, Resource> logPaths = new HashMap<>();
        logPaths.put("lgg_ucsf_2014 validation log", null);
        logPaths.put("lgg_ucsf_2014 validation report", null);
        assertEquals(logPaths, validator.getLogAndReportFiles());
    }

    @Test
    public void studyWarningValidationWithErrorLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(Resource.class), any(Resource.class), any(Resource.class))).thenReturn(ExitStatus.WARNINGS);
        ReflectionTestUtils.setField(validator, "validationLevel", "ERROR");

        Map<String, Resource> studies = new HashMap<>();
        studies.put("lgg_ucsf_2014", null);
        Map<String, ExitStatus> validatedStudies = validator.validate(studies);
        Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
        expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.WARNINGS);
        assertEquals(expectedValidatedStudies, validatedStudies);
        assertEquals(studies, validator.getValidStudies());

        Map<String, Resource> logPaths = new HashMap<>();
        logPaths.put("lgg_ucsf_2014 validation log", null);
        logPaths.put("lgg_ucsf_2014 validation report", null);
        assertEquals(logPaths, validator.getLogAndReportFiles());
    }

    @Test
    public void studyErrorValidationWithErrorLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(Resource.class), any(Resource.class), any(Resource.class))).thenReturn(ExitStatus.ERRORS);
        ReflectionTestUtils.setField(validator, "validationLevel", "WARNING");

        Map<String, Resource> studies = new HashMap<>();
        studies.put("lgg_ucsf_2014", null);
        Map<String, ExitStatus> validatedStudies = validator.validate(studies);
        Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
        expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.ERRORS);
        assertEquals(expectedValidatedStudies, validatedStudies);
        Map<String, Resource> validStudies = validator.getValidStudies();
        Map<String, Resource> expectedValidStudies = new HashMap<>();
        assertEquals(expectedValidStudies, validStudies);

        Map<String, Resource> logPaths = new HashMap<>();
        logPaths.put("lgg_ucsf_2014 validation log", null);
        logPaths.put("lgg_ucsf_2014 validation report", null);
        assertEquals(logPaths, validator.getLogAndReportFiles());
    }

    @Test
	public void multipleStudiesValidationWithWarningLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(Resource.class), any(Resource.class), any(Resource.class))).thenReturn(ExitStatus.SUCCESS, ExitStatus.WARNINGS, ExitStatus.ERRORS);
        ReflectionTestUtils.setField(validator, "validationLevel", "WARNING");

        Map<String, Resource> studies = new HashMap<>();
        studies.put("lgg_ucsf_2014", null);
        studies.put("study1", null);
        studies.put("study2", null);
        Map<String, ExitStatus> validatedStudies = validator.validate(studies);
        Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
        expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.SUCCESS);
        expectedValidatedStudies.put("study1", ExitStatus.WARNINGS);
        expectedValidatedStudies.put("study2", ExitStatus.ERRORS);
        assertEquals(expectedValidatedStudies, validatedStudies);

        Map<String, ExitStatus> expectedValidStudies = new HashMap<String, ExitStatus>();
        expectedValidStudies.put("lgg_ucsf_2014", null);
        assertEquals(expectedValidStudies, validator.getValidStudies());

        Map<String, Resource> logPaths = new HashMap<>();
        logPaths.put("lgg_ucsf_2014 validation log", null);
        logPaths.put("lgg_ucsf_2014 validation report", null);
        logPaths.put("study1 validation log", null);
        logPaths.put("study1 validation report", null);
        logPaths.put("study2 validation log", null);
        logPaths.put("study2 validation report", null);
        assertEquals(logPaths, validator.getLogAndReportFiles());
    }

    @Test
	public void multipleStudiesValidationWithErrorLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(Resource.class), any(Resource.class), any(Resource.class))).thenReturn(ExitStatus.SUCCESS, ExitStatus.WARNINGS, ExitStatus.ERRORS);
        ReflectionTestUtils.setField(validator, "validationLevel", "ERROR");

        Map<String, Resource> studies = new HashMap<>();
        studies.put("lgg_ucsf_2014", null);
        studies.put("study1", null);
        studies.put("study2", null);
        Map<String, ExitStatus> validatedStudies = validator.validate(studies);
        Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
        expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.SUCCESS);
        expectedValidatedStudies.put("study1", ExitStatus.WARNINGS);
        expectedValidatedStudies.put("study2", ExitStatus.ERRORS);
        assertEquals(expectedValidatedStudies, validatedStudies);

        Map<String, ExitStatus> expectedValidStudies = new HashMap<String, ExitStatus>();
        expectedValidStudies.put("lgg_ucsf_2014", null);
        expectedValidStudies.put("study1", null);
        assertEquals(expectedValidStudies, validator.getValidStudies());

        Map<String, Resource> logPaths = new HashMap<>();
        logPaths.put("lgg_ucsf_2014 validation log", null);
        logPaths.put("lgg_ucsf_2014 validation report", null);
        logPaths.put("study1 validation log", null);
        logPaths.put("study1 validation report", null);
        logPaths.put("study2 validation log", null);
        logPaths.put("study2 validation report", null);
        assertEquals(logPaths, validator.getLogAndReportFiles());
    }
}
