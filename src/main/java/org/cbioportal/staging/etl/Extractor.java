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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.pivovarit.function.ThrowingFunction;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.DirectoryCreatorException;
import org.cbioportal.staging.exceptions.ExtractionException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.IDirectoryCreator;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/*
	Copies all files from scan.location to the etl.working.directory.
*/
@Component
class Extractor {
	private static final Logger logger = LoggerFactory.getLogger(Extractor.class);

	@Value("${scan.retry.time:5}")
	private Integer timeRetry;

	@Autowired
    private ResourceUtils utils;

    @Autowired
	private IDirectoryCreator directoryCreator;

	Map<String, List<String>> filesNotFound = new HashMap<>();

	public Map<String, Resource> run(Map<String, Resource[]> resources, String timestamp) throws ExtractionException {

		if (resources == null) {
			throw new ExtractionException("Argument 'resources' is null.");
		}

		if (timestamp == null) {
			throw new ExtractionException("Argument 'timestamp' is null.");
		}

		filesNotFound.clear();
		Map<String, Resource> out = new HashMap<>();

		try {
			for (Entry<String, Resource[]> studyResources : resources.entrySet()) {

                String studyId = studyResources.getKey();
                Resource studyDir = directoryCreator.createStudyExtractDir(timestamp, studyId);

				String remoteBasePath = getBasePathResources(studyResources.getValue());

				List<String> errorFiles = new ArrayList<>();
				for (Resource remoteResource : studyResources.getValue()) {

					String fullOriginalFilePath = remoteResource.getURL().toString();
					String remoteFilePath = fullOriginalFilePath.replaceFirst(remoteBasePath, "");

					Resource localResource = attemptCopyResource(studyDir, remoteResource, remoteFilePath);
					if (localResource == null) {
						errorFiles.add(fullOriginalFilePath);
					}
				}

				// register successfully extracted study
				if (errorFiles.isEmpty()) {
					out.put(studyId, studyDir);
				} else {
					filesNotFound.put(studyId, errorFiles);
				}
			}

		} catch (IOException e) {
			throw new ExtractionException("Cannot access working ELT directory.", e);
		} catch (ConfigurationException e) {
			throw new ExtractionException(e.getMessage(), e);
		} catch (InterruptedException e) {
			throw new ExtractionException("Timeout for resource downloads was interrupted.", e);
		} catch (ResourceCollectionException e) {
			throw new ExtractionException("Cannot copy Resource.", e);
		} catch (DirectoryCreatorException e) {
            throw new ExtractionException("Cannot create directory.", e);
        }

		logger.info("Extractor step finished");
		return out;
	}

	private Resource attemptCopyResource(Resource destination, Resource resource, String remoteFilePath)
			throws InterruptedException, ResourceCollectionException {
		int i = 1;
		int times = 5;
		Resource r = null;
		while (i++ <= times) {
			try {
				logger.info("Copying resource " + resource.getURL() + " to "+ destination);
				r = utils.copyResource(destination, resource, remoteFilePath);
				logger.info("File has been copied successfully to "+ destination);
				break;
			} catch (IOException f) {
				if (i < times) {
					TimeUnit.MINUTES.sleep(timeRetry);
				}
			}
		}
		return r;
	}

	private String getBasePathResources(Resource[] resources) throws ConfigurationException {
		List<String> paths = Stream.of(resources)
			.map(ThrowingFunction.unchecked(e -> e.getURL().toString()))
			.collect(Collectors.toList());
		return utils.getBasePath(paths);
	}

	public Map<String, List<String>> errorFiles() {
		return filesNotFound;
	}

	public Integer getTimeRetry() {
		return timeRetry;
	}

}
