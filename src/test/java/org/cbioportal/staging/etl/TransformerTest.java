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
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {org.cbioportal.staging.etl.Transformer.class,
		org.cbioportal.staging.etl.EmailServiceMockupImpl.class,
		org.cbioportal.staging.etl.TransformerServiceMockupImpl.class})
@SpringBootTest
@Import(MyTestConfiguration.class)

public class TransformerTest {
	
	@Autowired
	private Transformer transformer;

	@Autowired
	private TransformerServiceMockupImpl transformationService;
			
	// @Before
	// public void setUp() throws Exception {
	// 	emailService.reset();
	// 	validationService.reset();
	// }
	
	@Test
	public void studyWithTransformation() {
		File studyPath = new File("src/test/resources/transformer_tests/study1/");
		boolean skipTransformation = transformer.skipTransformation(studyPath);
		
		//Build the expected outcome and check that is the same as the function output
		assertEquals(false, skipTransformation);
	}

	@Test
	public void studyWithNoTransformation() {
		File studyPath = new File("src/test/resources/transformer_tests/study2/");
		boolean skipTransformation = transformer.skipTransformation(studyPath);
		
		//Build the expected outcome and check that is the same as the function output
		assertEquals(true, skipTransformation);
	}

	@Test
	public void studyWithTransformationFakeMetaStudy() {
		File studyPath = new File("src/test/resources/transformer_tests/study3/");
		boolean skipTransformation = transformer.skipTransformation(studyPath);
		
		//Build the expected outcome and check that is the same as the function output
		assertEquals(false, skipTransformation);
	}
	
}
