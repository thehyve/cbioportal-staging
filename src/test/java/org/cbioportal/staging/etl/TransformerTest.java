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

import org.cbioportal.staging.etl.Transformer.ExitStatus;
import org.cbioportal.staging.exceptions.TransformerException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { org.cbioportal.staging.etl.Transformer.class,
        org.cbioportal.staging.etl.EmailServiceMockupImpl.class,
        org.cbioportal.staging.etl.TransformerServiceMockupImpl.class,
        org.cbioportal.staging.etl.PublisherServiceMockupImpl.class })
@SpringBootTest
public class TransformerTest {

    @Autowired
    private Transformer transformer;

    @Test
    public void studyWithTransformation() {
        File studyPath = new File("src/test/resources/transformer_tests/study1/");
        boolean metaFileExists = transformer.metaFileExists(studyPath);

        // Build the expected outcome and check that is the same as the function output
        assertEquals(false, metaFileExists);
    }

    //Study 1, which goes through transformation, should be successfully returned as transformed:
    @Test
    public void transformStudyWithTransformation() throws TransformerException {
        String transformationCommand = "test";
        Map<String, File> studies = new HashMap<String, File>();
        studies.put("study1", new File("src/test/resources/transformer_tests/study1"));
        String date = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        Map<String, ExitStatus> transformedStudy = transformer.transform(date, studies, transformationCommand, "");

        Map<String, ExitStatus> expectedResult = new HashMap<String, ExitStatus>();
        expectedResult.put("study1", ExitStatus.SUCCESS);

        assertEquals(1, transformedStudy.size());
        assertEquals(transformedStudy, expectedResult);
	}

    @Test
	public void studyWithNoTransformation() {
		File studyPath = new File("src/test/resources/transformer_tests/study2/");
		boolean metaFileExists = transformer.metaFileExists(studyPath);

		//Build the expected outcome and check that is the same as the function output
		assertEquals(true, metaFileExists);
    }

    //Study 2, which skips transformation, should be successfully returned as (already) transformed:
    @Test
    public void transformStudyWithNoTransformation() throws TransformerException {
        String transformationCommand = "test";
        Map<String, File> studies = new HashMap<String, File>();
        studies.put("study2", new File("src/test/resources/transformer_tests/study2"));
        String date = new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
        Map<String, ExitStatus> transformedStudy = transformer.transform(date, studies, transformationCommand, "");

        Map<String, ExitStatus> expectedResult = new HashMap<String, ExitStatus>();
        expectedResult.put("study2", ExitStatus.NOTRANSF);

        assertEquals(1, transformedStudy.size());
        assertEquals(expectedResult, transformedStudy);
	}

	@Test
	public void studyWithTransformationFakeMetaStudy() {
		File studyPath = new File("src/test/resources/transformer_tests/study3/");
		boolean metaFileExists = transformer.metaFileExists(studyPath);

		//Build the expected outcome and check that is the same as the function output
		assertEquals(false, metaFileExists);
    }

}
