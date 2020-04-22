/*
* Copyright (c) 2020 The Hyve B.V.
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
package org.cbioportal.staging.services.publish;

import java.util.Map;

import org.cbioportal.staging.exceptions.PublisherException;
import org.cbioportal.staging.services.resource.Study;
import org.springframework.core.io.Resource;

public interface IPublisherService {

    public Map<Study, Resource> publishFiles(Map<Study, Resource> initialLogFiles) throws PublisherException;

}
