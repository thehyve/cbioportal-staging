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

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.etl.Transformer.ExitStatus;
import org.cbioportal.staging.exceptions.LoaderException;
import org.cbioportal.staging.services.LoaderService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = org.cbioportal.staging.etl.Loader.class)
public class LoaderTest {

    @Autowired
    private Loader loader;

    @MockBean
    private LoaderService loaderService;

    @Test
    public void studyLoaded() throws LoaderException {

        when(loaderService.load(any(File.class), any(File.class))).thenReturn(ExitStatus.SUCCESS);

        Map<String, File> studies = new HashMap<String, File>();
        studies.put("lgg_ucsf_2014", new File("test/path"));
        Map<String, ExitStatus> loadedStudies = loader.load(studies, "");
        Map<String, ExitStatus> expectedLoadedStudies = new HashMap<String, ExitStatus>();
        expectedLoadedStudies.put("lgg_ucsf_2014", ExitStatus.SUCCESS);
		assertEquals(expectedLoadedStudies, loadedStudies); //The study has been loaded
    }

    @Test
    public void multipleStudiesLoaded() throws LoaderException {

        when(loaderService.load(any(File.class), any(File.class))).thenReturn(ExitStatus.SUCCESS, ExitStatus.ERRORS);

        Map<String, File> studies = new HashMap<String, File>();
        studies.put("lgg_ucsf_2014", new File("test/path"));
        studies.put("study_with_errors", new File("test/path2"));
        Map<String, ExitStatus> loadedStudies = loader.load(studies, "");
        Map<String, ExitStatus> expectedLoadedStudies = new HashMap<String, ExitStatus>();
        expectedLoadedStudies.put("lgg_ucsf_2014", ExitStatus.SUCCESS);
        expectedLoadedStudies.put("study_with_errors", ExitStatus.ERRORS);
		assertEquals(expectedLoadedStudies, loadedStudies); //The study has been loaded
    }
}
