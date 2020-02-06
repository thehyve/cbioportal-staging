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

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
// @SpringBootTest(classes = { Extractor.class, org.cbioportal.staging.services.resource.ResourceUtils.class })
public class ExtractorTest {

	// @Autowired
	// private Extractor extractor;

	// @MockBean
	// private ResourceUtils utils;

    // private Resource etlDir = mock(Resource.class);
    
    @Test
    public void EmptyTest()
    {
    }

	// TODO test correct study/destination path

	// TODO test correct remote base path

	// TODO test reaction after a number of attempts

	// TODO test handling of error files

}
