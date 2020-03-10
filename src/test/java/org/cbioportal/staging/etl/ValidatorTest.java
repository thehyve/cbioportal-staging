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

        Study dummyStudy = new Study("dummy-study", "dummy-time", "dummy-time", null, null);
        Map<Study, ExitStatus> validatedStudies = validator.validate(new Study[] {dummyStudy});
        Map<Study, ExitStatus> expectedValidatedStudies = new HashMap<Study, ExitStatus>();
        expectedValidatedStudies.put(dummyStudy, ExitStatus.SUCCESS);
        assertEquals(expectedValidatedStudies, validatedStudies);

        assertEquals(1, validator.getValidStudies().length);
        assertTrue(TestUtils.has(validator.getValidStudies(), dummyStudy.getStudyId()));

        Map<Study, Resource> logPaths = new HashMap<>();
        logPaths.put(dummyStudy, null);
        assertEquals(logPaths, validator.getLogFiles());

        Map<Study, Resource> reportPaths = new HashMap<>();
        reportPaths.put(dummyStudy, null);
        assertEquals(logPaths, validator.getReportFiles());
    }

    @Test
    public void studyWarningValidationWithWarningLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(), any(), any())).thenReturn(ExitStatus.WARNING);
        ReflectionTestUtils.setField(validator, "validationLevel", "WARNING");

        Study dummyStudy = new Study("dummy-study", "dummy-time", "dummy-time", null, null);
        Map<Study, ExitStatus> validatedStudies = validator.validate(new Study[] {dummyStudy});
        Map<Study, ExitStatus> expectedValidatedStudies = new HashMap<Study, ExitStatus>();
        expectedValidatedStudies.put(dummyStudy, ExitStatus.WARNING);
        assertEquals(expectedValidatedStudies, validatedStudies);

        assertEquals(0, validator.getValidStudies().length);

        Map<Study, Resource> logPaths = new HashMap<>();
        logPaths.put(dummyStudy, null);
        assertEquals(logPaths, validator.getLogFiles());

        Map<Study, Resource> reportPaths = new HashMap<>();
        reportPaths.put(dummyStudy, null);
        assertEquals(logPaths, validator.getReportFiles());
    }

    @Test
    public void studyErrorValidationWithWarningLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(), any(), any())).thenReturn(ExitStatus.ERROR);
        ReflectionTestUtils.setField(validator, "validationLevel", "WARNING");

        Study dummyStudy = new Study("dummy-study", "dummy-time", "dummy-time", null, null);
        Map<Study, ExitStatus> validatedStudies = validator.validate(new Study[] {dummyStudy});
        Map<Study, ExitStatus> expectedValidatedStudies = new HashMap<Study, ExitStatus>();
        expectedValidatedStudies.put(dummyStudy, ExitStatus.ERROR);
        assertEquals(expectedValidatedStudies, validatedStudies);

        assertEquals(0, validator.getValidStudies().length);

        Map<Study, Resource> logPaths = new HashMap<>();
        logPaths.put(dummyStudy, null);
        assertEquals(logPaths, validator.getLogFiles());

        Map<Study, Resource> reportPaths = new HashMap<>();
        reportPaths.put(dummyStudy, null);
        assertEquals(logPaths, validator.getReportFiles());
    }

    @Test
	public void studySuccessValidationWithErrorLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(), any(), any())).thenReturn(ExitStatus.SUCCESS);
        ReflectionTestUtils.setField(validator, "validationLevel", "ERROR");

        Study dummyStudy = new Study("dummy-study", "dummy-time", "dummy-time", null, null);
        Map<Study, ExitStatus> validatedStudies = validator.validate(new Study[] {dummyStudy});
        Map<Study, ExitStatus> expectedValidatedStudies = new HashMap<Study, ExitStatus>();
        expectedValidatedStudies.put(dummyStudy, ExitStatus.SUCCESS);
        assertEquals(expectedValidatedStudies, validatedStudies);

        assertEquals(1, validator.getValidStudies().length);
        assertTrue(TestUtils.has(validator.getValidStudies(), dummyStudy.getStudyId()));

        Map<Study, Resource> logPaths = new HashMap<>();
        logPaths.put(dummyStudy, null);
        assertEquals(logPaths, validator.getLogFiles());

        Map<Study, Resource> reportPaths = new HashMap<>();
        reportPaths.put(dummyStudy, null);
        assertEquals(logPaths, validator.getReportFiles());
    }

    @Test
    public void studyWarningValidationWithErrorLevel() throws ValidatorException, ResourceCollectionException {

        ReflectionTestUtils.setField(validator, "validationLevel", "ERROR");

        Study dummyStudy = new Study("dummy-study", "dummy-time", "dummy-time", null, null);
        when(validatorService.validate(any(), any(), any())).thenReturn(ExitStatus.WARNING);

        Map<Study, ExitStatus> validatedStudies = validator.validate(new Study[] {dummyStudy});

        Map<Study, ExitStatus> expectedValidatedStudies = new HashMap<Study, ExitStatus>();
        expectedValidatedStudies.put(dummyStudy, ExitStatus.WARNING);
        assertEquals(expectedValidatedStudies, validatedStudies);

        assertEquals(1, validator.getValidStudies().length);
        assertTrue(TestUtils.has(validator.getValidStudies(), dummyStudy.getStudyId()));

        Map<Study, Resource> logPaths = new HashMap<>();
        logPaths.put(dummyStudy, null);
        assertEquals(logPaths, validator.getLogFiles());

        Map<Study, Resource> reportPaths = new HashMap<>();
        reportPaths.put(dummyStudy, null);
        assertEquals(logPaths, validator.getReportFiles());
    }

    @Test
    public void studyErrorValidationWithErrorLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(), any(), any())).thenReturn(ExitStatus.ERROR);
        ReflectionTestUtils.setField(validator, "validationLevel", "WARNING");

        Study dummyStudy = new Study("dummy-study", "dummy-time", "dummy-time", null, null);
        Map<Study, ExitStatus> validatedStudies = validator.validate(new Study[] {dummyStudy});

        Map<Study, ExitStatus> expectedValidatedStudies = new HashMap<Study, ExitStatus>();
        expectedValidatedStudies.put(dummyStudy, ExitStatus.ERROR);
        assertEquals(expectedValidatedStudies, validatedStudies);

        Study[] validStudies = validator.getValidStudies();
        assertEquals(0, validStudies.length);

        Map<Study, Resource> logPaths = new HashMap<>();
        logPaths.put(dummyStudy, null);
        assertEquals(logPaths, validator.getLogFiles());

        Map<Study, Resource> reportPaths = new HashMap<>();
        reportPaths.put(dummyStudy, null);
        assertEquals(logPaths, validator.getReportFiles());
    }

    @Test
	public void multipleStudiesValidationWithWarningLevel() throws ValidatorException, ResourceCollectionException {
        when(validatorService.validate(any(), any(), any())).thenReturn(ExitStatus.SUCCESS, ExitStatus.WARNING, ExitStatus.ERROR);
        ReflectionTestUtils.setField(validator, "validationLevel", "WARNING");

        Study dummyStudy = new Study("dummy-study", "dummy-time", "dummy-time", null, null);
        Study dummyStudy2 = new Study("dummy-study-2", "dummy-time", "dummy-time", null, null);
        Study dummyStudy3 = new Study("dummy-study-3", "dummy-time", "dummy-time", null, null);
        Map<Study, ExitStatus> validatedStudies = validator.validate(new Study[] {dummyStudy, dummyStudy2, dummyStudy3});
        Map<Study, ExitStatus> expectedValidatedStudies = new HashMap<Study, ExitStatus>();
        expectedValidatedStudies.put(dummyStudy, ExitStatus.SUCCESS);
        expectedValidatedStudies.put(dummyStudy2, ExitStatus.WARNING);
        expectedValidatedStudies.put(dummyStudy3, ExitStatus.ERROR);
        assertEquals(expectedValidatedStudies, validatedStudies);

        assertEquals(1, validator.getValidStudies().length);
        assertTrue(TestUtils.has(validator.getValidStudies(), dummyStudy.getStudyId()));

        Map<Study, Resource> logPaths = new HashMap<>();
        logPaths.put(dummyStudy, null);
        logPaths.put(dummyStudy2, null);
        logPaths.put(dummyStudy3, null);
        assertEquals(logPaths, validator.getLogFiles());

        Map<Study, Resource> reportPaths = new HashMap<>();
        reportPaths.put(dummyStudy, null);
        reportPaths.put(dummyStudy2, null);
        reportPaths.put(dummyStudy3, null);
        assertEquals(logPaths, validator.getReportFiles());
    }

    @Test
	public void multipleStudiesValidationWithErrorLevel() throws ValidatorException, ResourceCollectionException {
        ReflectionTestUtils.setField(validator, "validationLevel", "ERROR");

        Study dummyStudy = new Study("dummy-study", "dummy-time", "dummy-time", null, null);
        Study dummyStudy2 = new Study("dummy-study-2", "dummy-time", "dummy-time", null, null);
        Study dummyStudy3 = new Study("dummy-study-3", "dummy-time", "dummy-time", null, null);
        when(validatorService.validate(any(), any(), any())).thenReturn(ExitStatus.SUCCESS, ExitStatus.WARNING, ExitStatus.ERROR);

        Map<Study, ExitStatus> validatedStudies = validator.validate(new Study[] {dummyStudy, dummyStudy2, dummyStudy3});

        Map<Study, ExitStatus> expectedValidatedStudies = new HashMap<Study, ExitStatus>();
        expectedValidatedStudies.put(dummyStudy, ExitStatus.SUCCESS);
        expectedValidatedStudies.put(dummyStudy2, ExitStatus.WARNING);
        expectedValidatedStudies.put(dummyStudy3, ExitStatus.ERROR);
        assertEquals(expectedValidatedStudies, validatedStudies);

        assertEquals(2, validator.getValidStudies().length);
        assertTrue(TestUtils.has(validator.getValidStudies(), dummyStudy.getStudyId()));
        assertTrue(TestUtils.has(validator.getValidStudies(), dummyStudy2.getStudyId()));

        Map<Study, Resource> logPaths = new HashMap<>();
        logPaths.put(dummyStudy, null);
        logPaths.put(dummyStudy2, null);
        logPaths.put(dummyStudy3, null);
        assertEquals(logPaths, validator.getLogFiles());

        Map<Study, Resource> reportPaths = new HashMap<>();
        reportPaths.put(dummyStudy, null);
        reportPaths.put(dummyStudy2, null);
        reportPaths.put(dummyStudy3, null);
        assertEquals(logPaths, validator.getReportFiles());
    }

}
