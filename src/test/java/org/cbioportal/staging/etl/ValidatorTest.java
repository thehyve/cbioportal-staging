/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.etl;

import static org.junit.Assert.assertEquals;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.cbioportal.staging.exceptions.ConfigurationException;
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
@ContextConfiguration(classes = {org.cbioportal.staging.etl.Validator.class, 
		org.cbioportal.staging.etl.EmailServiceMockupImpl.class,
		org.cbioportal.staging.etl.ValidationServiceMockupImpl.class})
@SpringBootTest
@Import(MyTestConfiguration.class)

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
	}
	
	@Test
	public void parseLogFile() {
		ReflectionTestUtils.setField(validator, "emailService", emailService);
		File etlWorkingDir = new File("src/test/resources/validator_tests");
		ReflectionTestUtils.setField(validator, "centralShareLocation", etlWorkingDir);
		ReflectionTestUtils.setField(validator, "etlWorkingDir", etlWorkingDir);

		File logFile = new File("src/test/resources/validator_tests/test.log"); 
		Map<String, Integer> result = validator.getMessageCounter(logFile);
		
		Map<String, Integer> expectedResult = new HashMap<String, Integer>();
		expectedResult.put("WARNING", 14);
		expectedResult.put("ERROR", 0);
		
		//Build the expected outcome and check that is the same as the function output
		assertEquals(expectedResult, result);
	}
	
	@Test
	public void parseNotFoundLogFile() {
		ReflectionTestUtils.setField(validator, "emailService", emailService);
		File etlWorkingDir = new File("src/test/resources/validator_tests");
		ReflectionTestUtils.setField(validator, "centralShareLocation", etlWorkingDir);
		ReflectionTestUtils.setField(validator, "etlWorkingDir", etlWorkingDir);

		File logFile = new File("src/test/resources/validator_tests/notfound.log"); 
		Map<String, Integer> result = validator.getMessageCounter(logFile);
		
		assertEquals(null, result); //We handle an error so the function should return null
	}
	
	@Test
	public void studyHasPassedValidationNoWarnings() {
		Map<String, Integer> messageCounter = new HashMap<String, Integer>();
		messageCounter.put("WARNING", 0);
		messageCounter.put("ERROR", 0);
		boolean result = validator.hasStudyPassed("study", "WARNING", messageCounter);
		
		//Build the expected outcome and check that is the same as the function output
		assertEquals(true, result);
	}
	
	@Test
	public void studyHasPassedValidationWithWarnings() {
		Map<String, Integer> messageCounter = new HashMap<String, Integer>();
		messageCounter.put("WARNING", 3);
		messageCounter.put("ERROR", 0);
		boolean result = validator.hasStudyPassed("study", "ERROR", messageCounter);
		
		//Build the expected outcome and check that is the same as the function output
		assertEquals(true, result);
	}
	
	@Test
	public void studyHasFailedValidationWarningsWarning() {
		Map<String, Integer> messageCounter = new HashMap<String, Integer>();
		messageCounter.put("WARNING", 3);
		messageCounter.put("ERROR", 0);
		boolean result = validator.hasStudyPassed("study", "WARNING", messageCounter);
		
		//Build the expected outcome and check that is the same as the function output
		assertEquals(false, result);
	}
	
	@Test
	public void studyHasFailedValidationWarningsError() {
		Map<String, Integer> messageCounter = new HashMap<String, Integer>();
		messageCounter.put("WARNING", 0);
		messageCounter.put("ERROR", 12);
		boolean result = validator.hasStudyPassed("study", "WARNING", messageCounter);
		
		//Build the expected outcome and check that is the same as the function output
		assertEquals(false, result);
	}
	
	@Test
	public void studyHasPassedFailedWithErrors() {
		Map<String, Integer> messageCounter = new HashMap<String, Integer>();
		messageCounter.put("WARNING", 0);
		messageCounter.put("ERROR", 3);
		boolean result = validator.hasStudyPassed("study", "ERROR", messageCounter);
		
		//Build the expected outcome and check that is the same as the function output
		assertEquals(false, result);
	}
	
	@Test(expected=IllegalArgumentException.class)
	public void studyHasPassedWrongLevel() {
		ReflectionTestUtils.setField(validator, "emailService", emailService);

		Map<String, Integer> messageCounter = new HashMap<String, Integer>();
		messageCounter.put("WARNING", 0);
		messageCounter.put("ERROR", 3);
		boolean result = validator.hasStudyPassed("study", "WRONG_LEVEL", messageCounter);
		
		//Build the expected outcome and check that is the same as the function output
		assertEquals(null, result);
	}
	
	@Test(expected=IOException.class)
	public void noCentralShareLocation() throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, ConfigurationException, IOException, TemplateException {
		ReflectionTestUtils.setField(validator, "emailService", emailService);
		File notfound = new File("notfound");
		File etlWorkingDir = new File("src/test/resources/validator_tests");
		ReflectionTestUtils.setField(validator, "centralShareLocation", notfound);
		ReflectionTestUtils.setField(validator, "etlWorkingDir", etlWorkingDir);

		List<String> studies = new ArrayList<String>();
		studies.add("lgg_ucsf_2014");
		List<String> result = validator.validate(0, studies);
		assertEquals(0, result.size());
		
		//Check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(false, emailService.isEmailValidationReportSent());
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(true, emailService.isEmailGenericErrorSent()); //Email is sent since there is a "generic" error
	}
	
	@Test
	public void studyPassedValidation() throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, ConfigurationException, IOException, TemplateException {
		ReflectionTestUtils.setField(validator, "emailService", emailService);
		ReflectionTestUtils.setField(validator, "validationService", validationService);
		ReflectionTestUtils.setField(validationService, "throwError", false);
		ReflectionTestUtils.setField(validationService, "testFile", "src/test/resources/validator_tests/test.log");

		File etlWorkingDir = new File("src/test/resources/validator_tests");
		ReflectionTestUtils.setField(validator, "centralShareLocation", etlWorkingDir);
		ReflectionTestUtils.setField(validator, "etlWorkingDir", etlWorkingDir);
		ReflectionTestUtils.setField(validator, "validationLevel", "ERROR");

		List<String> studies = new ArrayList<String>();
		studies.add("lgg_ucsf_2014");
		ArrayList<String> result = validator.validate(0, studies);
		assertEquals(studies, result); //The study passed has passed validation,
		
		//Check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(true, emailService.isEmailValidationReportSent());
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(false, emailService.isEmailGenericErrorSent()); 
	}
	
	@Test
	public void studyFailedValidation() throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, ConfigurationException, IOException, TemplateException {
		ReflectionTestUtils.setField(validator, "emailService", emailService);
		ReflectionTestUtils.setField(validator, "validationService", validationService);
		ReflectionTestUtils.setField(validationService, "throwError", false);
		ReflectionTestUtils.setField(validationService, "testFile", "src/test/resources/validator_tests/test2.log");

		File etlWorkingDir = new File("src/test/resources/validator_tests");
		ReflectionTestUtils.setField(validator, "centralShareLocation", etlWorkingDir);
		ReflectionTestUtils.setField(validator, "etlWorkingDir", etlWorkingDir);
		ReflectionTestUtils.setField(validator, "validationLevel", "ERROR");

		List<String> studies = new ArrayList<String>();
		studies.add("lgg_ucsf_2014");
		ArrayList<String> result = validator.validate(0, studies);
		assertEquals(0, result.size()); //The study added has failed validation, is not going to be loaded
		
		//Check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(true, emailService.isEmailValidationReportSent());
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(false, emailService.isEmailGenericErrorSent()); 
	}
	
	@Test
	public void validationErrorEmailSent() throws TemplateNotFoundException, MalformedTemplateNameException, ParseException, ConfigurationException, IOException, TemplateException {
		ReflectionTestUtils.setField(validator, "emailService", emailService);
		ReflectionTestUtils.setField(validator, "validationService", validationService);
		ReflectionTestUtils.setField(validationService, "throwError", true);
		
		File etlWorkingDir = new File("src/test/resources/validator_tests");
		ReflectionTestUtils.setField(validator, "centralShareLocation", etlWorkingDir);
		ReflectionTestUtils.setField(validator, "etlWorkingDir", etlWorkingDir);
		

		List<String> studies = new ArrayList<String>();
		studies.add("lgg_ucsf_2014");
		ArrayList<String> result = validator.validate(0, studies);
		assertEquals(0, result.size());
		
		//Check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(false, emailService.isEmailValidationReportSent());
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(true, emailService.isEmailGenericErrorSent()); 
	}
}
