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
import java.io.IOException;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;

import org.cbioportal.staging.exceptions.ConfigurationException;
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
@ContextConfiguration(classes = { org.cbioportal.staging.etl.Transformer.class,
        org.cbioportal.staging.etl.EmailServiceMockupImpl.class,
        org.cbioportal.staging.etl.TransformerServiceMockupImpl.class,
        org.cbioportal.staging.etl.ValidationServiceMockupImpl.class })
@SpringBootTest
@Import(MyTestConfiguration.class)

public class TransformerTest {

    @Autowired
    private Transformer transformer;

    @Test
    public void studyWithTransformation() {
        File studyPath = new File("src/test/resources/transformer_tests/study1/");
        boolean skipTransformation = transformer.skipTransformation(studyPath);

        // Build the expected outcome and check that is the same as the function output
        assertEquals(false, skipTransformation);
    }

    //Study 1, which goes through transformation, should be successfully returned as transformed:
    @Test
    public void transformStudyWithTransformation() throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, InterruptedException, ConfigurationException, IOException, TemplateException {
        File etlWorkingDir = new File("src/test/resources/transformer_tests/");
        ReflectionTestUtils.setField(transformer, "etlWorkingDir", etlWorkingDir);
        Map<String, String> filesPaths = new HashMap<String, String>();
        String transformationCommand = "test";
        List<String> studies = new ArrayList<String>();
        studies.add("study1");
        List<String> transformedStudy = transformer.transform(1, studies, transformationCommand, filesPaths);
		
        assertEquals(1, transformedStudy.size());
        assertEquals("study1", transformedStudy.get(0));
	}

    @Test
	public void studyWithNoTransformation() {
		File studyPath = new File("src/test/resources/transformer_tests/study2/");
		boolean skipTransformation = transformer.skipTransformation(studyPath);
		
		//Build the expected outcome and check that is the same as the function output
		assertEquals(true, skipTransformation);
    }
    
    //Study 2, which skips transformation, should be successfully returned as (already) transformed:
    @Test
    public void transformStudyWithNoTransformation() throws TemplateNotFoundException, MalformedTemplateNameException,
            ParseException, InterruptedException, ConfigurationException, IOException, TemplateException {
        File etlWorkingDir = new File("src/test/resources/transformer_tests/");
        ReflectionTestUtils.setField(transformer, "etlWorkingDir", etlWorkingDir);
        Map<String, String> filesPaths = new HashMap<String, String>();
        String transformationCommand = "test";
        List<String> studies = new ArrayList<String>();
        studies.add("study2");
        List<String> transformedStudy = transformer.transform(1, studies, transformationCommand, filesPaths);
		
        assertEquals(1, transformedStudy.size());
        assertEquals("study2", transformedStudy.get(0));
	}

	@Test
	public void studyWithTransformationFakeMetaStudy() {
		File studyPath = new File("src/test/resources/transformer_tests/study3/");
		boolean skipTransformation = transformer.skipTransformation(studyPath);
		
		//Build the expected outcome and check that is the same as the function output
		assertEquals(false, skipTransformation);
    }
	
}
