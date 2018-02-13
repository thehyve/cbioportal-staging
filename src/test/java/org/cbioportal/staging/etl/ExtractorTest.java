package org.cbioportal.staging.etl;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {org.cbioportal.staging.etl.Extractor.class, 
		org.cbioportal.staging.etl.EmailServiceMockupImpl.class})
@SpringBootTest
@Import(MyTestConfiguration.class)

public class ExtractorTest {

	@Autowired
	private Extractor extractor;
	
	@Autowired
	private EmailServiceMockupImpl emailService;
	
	@Autowired
	private ResourcePatternResolver resourcePatternResolver;
	
	@Before
    public void setUp() throws Exception {
        emailService.reset();
    }
	
	@Rule
    public TemporaryFolder etlWorkingDir = new TemporaryFolder();

	@Test
	public void filesFoundAndNotFoundInYaml() throws IOException {
		ReflectionTestUtils.setField(extractor, "emailService", emailService);
		ReflectionTestUtils.setField(extractor, "scanLocation", "file:src/test/resources/extractor_tests");
		ReflectionTestUtils.setField(extractor, "etlWorkingDir", etlWorkingDir.getRoot());

		//Run extractor step:
		Resource indexFile =  this.resourcePatternResolver.getResource("file:src/test/resources/extractor_tests/list_of_studies_1.yaml");
		Pair<Integer, List<String>> result = extractor.run(indexFile);

		//check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(true, emailService.isEmailStudyFileNotFoundSent()); //Email is sent since a file does not exist
		assertEquals(false, emailService.isEmailValidationReportSent());
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(false, emailService.isEmailGenericErrorSent());
		
		//Build the expected outcome and check that is the same as the function output
		List<String> erList = new ArrayList<String>();
		erList.add("study1");
		Pair<Integer, List<String>> expectedResult = Pair.of(0, erList);
		assertEquals(expectedResult, result);
	}

	@Test
	public void allFilesFoundInYaml() throws IOException {
		ReflectionTestUtils.setField(extractor, "emailService", emailService);
		ReflectionTestUtils.setField(extractor, "scanLocation", "file:src/test/resources/extractor_tests");
		ReflectionTestUtils.setField(extractor, "etlWorkingDir", etlWorkingDir.getRoot());
		
		Resource indexFile =  this.resourcePatternResolver.getResource("file:src/test/resources/extractor_tests/list_of_studies_2.yaml");
		Pair<Integer, List<String>> result = extractor.run(indexFile);

		//check that no emails are sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(false, emailService.isEmailValidationReportSent());
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(false, emailService.isEmailGenericErrorSent());
		
		//Build the expected outcome and check that is the same as the function output
		List<String> erList = new ArrayList<String>();
		erList.add("study1");
		Pair<Integer, List<String>> expectedResult = Pair.of(0, erList);
		assertEquals(expectedResult, result);
	}
	
	@Test
	public void incorrectYaml() {
		ReflectionTestUtils.setField(extractor, "emailService", emailService);
		ReflectionTestUtils.setField(extractor, "scanLocation", "file:src/test/resources/extractor_tests");
		ReflectionTestUtils.setField(extractor, "etlWorkingDir", etlWorkingDir.getRoot());
		
		Resource indexFile =  this.resourcePatternResolver.getResource("file:src/test/resources/extractor_tests/list_of_studies_3.yaml");
		extractor.run(indexFile);

		//check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(false, emailService.isEmailValidationReportSent());
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(true, emailService.isEmailGenericErrorSent()); //Email is sent since there is a "generic" error
	}
	
	@Test
	public void notFoundYaml() {
		ReflectionTestUtils.setField(extractor, "emailService", emailService);
		ReflectionTestUtils.setField(extractor, "scanLocation", "file:src/test/resources/extractor_tests");
		ReflectionTestUtils.setField(extractor, "etlWorkingDir", etlWorkingDir.getRoot());
		
		Resource indexFile =  this.resourcePatternResolver.getResource("file:src/test/resources/extractor_tests/list_of_studies_4.yaml");
		extractor.run(indexFile);

		//check that the correct email is sent
		assertEquals(false, emailService.isEmailStudyErrorSent());
		assertEquals(false, emailService.isEmailStudyFileNotFoundSent());
		assertEquals(false, emailService.isEmailValidationReportSent());
		assertEquals(false, emailService.isEmailStudiesLoadedSent());
		assertEquals(true, emailService.isEmailGenericErrorSent()); //Email is sent since there is a "generic" error
	}
}
