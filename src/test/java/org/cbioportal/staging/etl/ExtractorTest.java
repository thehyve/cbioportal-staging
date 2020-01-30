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

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ExtractionException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = {Extractor.class, org.cbioportal.staging.services.resource.ResourceUtils.class})
public class ExtractorTest {

	@Autowired
    private Extractor extractor;

	// @Autowired
	// private ResourcePatternResolver resourcePatternResolver;

	private File etlDir = mock(File.class);

	@Test(expected = ExtractionException.class)
	public void etlWorkingDir_doesNotExist() throws IOException, InterruptedException, ConfigurationException, ExtractionException {

		// File etlDir = mock(File.class);
		when(etlDir.exists()).thenReturn(false);
		ReflectionTestUtils.setField(extractor, "etlWorkingDir", etlDir);

		Map<String,Resource[]> in = new HashMap<String,Resource[]>();
		extractor.run(in);
	}

	@Test(expected = ExtractionException.class)
	public void etlWorkingDir_isFile() throws IOException, InterruptedException, ConfigurationException, ExtractionException {

		// File etlDir = mock(File.class);
		when(etlDir.exists()).thenReturn(true);
		when(etlDir.isDirectory()).thenReturn(false);
		when(etlDir.isFile()).thenReturn(true);
		ReflectionTestUtils.setField(extractor, "etlWorkingDir", etlDir);

		Map<String,Resource[]> in = new HashMap<String,Resource[]>();
		extractor.run(in);
	}

	// test correct study/destination path

	// test correct remote base path

	// test reaction after a number of attempts

	// test handling of error files



}
