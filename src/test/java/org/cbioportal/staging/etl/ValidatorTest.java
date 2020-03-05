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
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.exceptions.ValidatorException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.etl.IValidatorService;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.cbioportal.staging.services.resource.Study;
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
    public void init() throws ResourceCollectionException, ResourceUtilsException {
        when(utils.createFileResource(isA(Resource.class), any(String.class))).thenReturn(null);
	}
	@Test
	public void studySuccessValidationWithWarningLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(), any(), any())).thenReturn(ExitStatus.SUCCESS);
        ReflectionTestUtils.setField(validator, "validationLevel", "WARNING");

        Study[] studies = dummyStudyInput("lgg_ucsf_2014");
        Map<String, ExitStatus> validatedStudies = validator.validate(studies);
        Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
        expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.SUCCESS);
        assertEquals(expectedValidatedStudies, validatedStudies);

        assertEquals(1, validator.getValidStudies().length);
        assertTrue(TestUtils.has(validator.getValidStudies(), "lgg_ucsf_2014"));

        Map<String, Resource> logPaths = new HashMap<>();
        logPaths.put("lgg_ucsf_2014 validation log", null);
        logPaths.put("lgg_ucsf_2014 validation report", null);
        assertEquals(logPaths, validator.getLogAndReportFiles());
    }

    @Test
    public void studyWarningValidationWithWarningLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(), any(), any())).thenReturn(ExitStatus.WARNING);
        ReflectionTestUtils.setField(validator, "validationLevel", "WARNING");

        Study[] studies = dummyStudyInput("lgg_ucsf_2014");
        Map<String, ExitStatus> validatedStudies = validator.validate(studies);
        Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
        expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.WARNING);
        assertEquals(expectedValidatedStudies, validatedStudies);

        assertEquals(0, validator.getValidStudies().length);

        Map<String, Resource> logPaths = new HashMap<>();
        logPaths.put("lgg_ucsf_2014 validation log", null);
        logPaths.put("lgg_ucsf_2014 validation report", null);
        assertEquals(logPaths, validator.getLogAndReportFiles());
    }

    @Test
    public void studyErrorValidationWithWarningLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(), any(), any())).thenReturn(ExitStatus.ERROR);
        ReflectionTestUtils.setField(validator, "validationLevel", "WARNING");

        Study[] studies = dummyStudyInput("lgg_ucsf_2014");
        Map<String, ExitStatus> validatedStudies = validator.validate(studies);
        Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
        expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.ERROR);
        assertEquals(expectedValidatedStudies, validatedStudies);

        assertEquals(0, validator.getValidStudies().length);

        Map<String, Resource> logPaths = new HashMap<>();
        logPaths.put("lgg_ucsf_2014 validation log", null);
        logPaths.put("lgg_ucsf_2014 validation report", null);
        assertEquals(logPaths, validator.getLogAndReportFiles());
    }

    @Test
	public void studySuccessValidationWithErrorLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(), any(), any())).thenReturn(ExitStatus.SUCCESS);
        ReflectionTestUtils.setField(validator, "validationLevel", "ERROR");

        Study[] studies = dummyStudyInput("lgg_ucsf_2014");
        Map<String, ExitStatus> validatedStudies = validator.validate(studies);
        Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
        expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.SUCCESS);
        assertEquals(expectedValidatedStudies, validatedStudies);

        assertEquals(1, validator.getValidStudies().length);
        assertTrue(TestUtils.has(validator.getValidStudies(), "lgg_ucsf_2014"));

        Map<String, Resource> logPaths = new HashMap<>();
        logPaths.put("lgg_ucsf_2014 validation log", null);
        logPaths.put("lgg_ucsf_2014 validation report", null);
        assertEquals(logPaths, validator.getLogAndReportFiles());
    }

    @Test
    public void studyWarningValidationWithErrorLevel() throws ValidatorException, ResourceCollectionException {

        ReflectionTestUtils.setField(validator, "validationLevel", "ERROR");

        Study[] studies = dummyStudyInput("lgg_ucsf_2014");
        when(validatorService.validate(any(), any(), any())).thenReturn(ExitStatus.WARNING);

        Map<String, ExitStatus> validatedStudies = validator.validate(studies);

        Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
        expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.WARNING);
        assertEquals(expectedValidatedStudies, validatedStudies);

        assertEquals(1, validator.getValidStudies().length);
        assertTrue(TestUtils.has(validator.getValidStudies(), "lgg_ucsf_2014"));

        Map<String, Resource> logPaths = new HashMap<>();
        logPaths.put("lgg_ucsf_2014 validation log", null);
        logPaths.put("lgg_ucsf_2014 validation report", null);
        assertEquals(logPaths, validator.getLogAndReportFiles());
    }

    @Test
    public void studyErrorValidationWithErrorLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(), any(), any())).thenReturn(ExitStatus.ERROR);
        ReflectionTestUtils.setField(validator, "validationLevel", "WARNING");

        Study[] studies = dummyStudyInput("lgg_ucsf_2014");
        Map<String, ExitStatus> validatedStudies = validator.validate(studies);

        Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
        expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.ERROR);
        assertEquals(expectedValidatedStudies, validatedStudies);

        Study[] validStudies = validator.getValidStudies();
        assertEquals(0, validStudies.length);

        Map<String, Resource> logPaths = new HashMap<>();
        logPaths.put("lgg_ucsf_2014 validation log", null);
        logPaths.put("lgg_ucsf_2014 validation report", null);
        assertEquals(logPaths, validator.getLogAndReportFiles());
    }

    @Test
	public void multipleStudiesValidationWithWarningLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(), any(), any())).thenReturn(ExitStatus.SUCCESS, ExitStatus.WARNING, ExitStatus.ERROR);
        ReflectionTestUtils.setField(validator, "validationLevel", "WARNING");

        Study[] studies = dummyStudyInput("lgg_ucsf_2014", "study1", "study2");
        Map<String, ExitStatus> validatedStudies = validator.validate(studies);
        Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
        expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.SUCCESS);
        expectedValidatedStudies.put("study1", ExitStatus.WARNING);
        expectedValidatedStudies.put("study2", ExitStatus.ERROR);
        assertEquals(expectedValidatedStudies, validatedStudies);

        assertEquals(1, validator.getValidStudies().length);
        assertTrue(TestUtils.has(validator.getValidStudies(), "lgg_ucsf_2014"));

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
        ReflectionTestUtils.setField(validator, "validationLevel", "ERROR");

        Study[] studies = dummyStudyInput("lgg_ucsf_2014", "study1", "study2");
        when(validatorService.validate(any(), any(), any())).thenReturn(ExitStatus.SUCCESS, ExitStatus.WARNING, ExitStatus.ERROR);

        Map<String, ExitStatus> validatedStudies = validator.validate(studies);

        Map<String, ExitStatus> expectedValidatedStudies = new HashMap<String, ExitStatus>();
        expectedValidatedStudies.put("lgg_ucsf_2014", ExitStatus.SUCCESS);
        expectedValidatedStudies.put("study1", ExitStatus.WARNING);
        expectedValidatedStudies.put("study2", ExitStatus.ERROR);
        assertEquals(expectedValidatedStudies, validatedStudies);

        assertEquals(2, validator.getValidStudies().length);
        assertTrue(TestUtils.has(validator.getValidStudies(), "lgg_ucsf_2014"));
        assertTrue(TestUtils.has(validator.getValidStudies(), "study1"));

        Map<String, Resource> logPaths = new HashMap<>();
        logPaths.put("lgg_ucsf_2014 validation log", null);
        logPaths.put("lgg_ucsf_2014 validation report", null);
        logPaths.put("study1 validation log", null);
        logPaths.put("study1 validation report", null);
        logPaths.put("study2 validation log", null);
        logPaths.put("study2 validation report", null);
        assertEquals(logPaths, validator.getLogAndReportFiles());
    }

    private Study[] dummyStudyInput(String ... ids) {
        return Stream.of(ids).map(id -> new Study(id, null, null, null, null)).toArray(Study[]::new);
    }

}
