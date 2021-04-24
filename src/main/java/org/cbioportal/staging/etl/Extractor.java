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

import com.pivovarit.function.ThrowingFunction;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.DirectoryCreatorException;
import org.cbioportal.staging.exceptions.ExtractionException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.directory.IDirectoryCreator;
import org.cbioportal.staging.services.resource.IResourceProvider;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.cbioportal.staging.services.resource.Study;
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

	@Autowired
	private IResourceProvider resourceProvider;

	Map<String, List<String>> filesNotFound = new HashMap<>();

	public Study[] run(Study[] studies) throws ExtractionException {

		if (studies == null) {
			throw new ExtractionException("Argument 'resources' is null.");
		}

		filesNotFound.clear();
		List<Study> out = new ArrayList<>();

		try {
			for (Study study : studies) {

				String studyId = study.getStudyId();
				Resource studyDir = directoryCreator.createStudyExtractDir(study);

				String remoteBasePath = getBasePathResources(study.getResources()).replaceAll("/+", "/");

				List<String> errorFiles = new ArrayList<>();
				List<Resource> files = new ArrayList<>();
				for (Resource remoteResource : study.getResources()) {

					logger.debug("Resource info: " + remoteResource.getFilename() + " " + utils.getURI(remoteResource) + " " + remoteResource.getDescription());
					String fullOriginalFilePath = utils.getURI(remoteResource).toString();
					fullOriginalFilePath = fullOriginalFilePath.replaceAll("/+", "/");

					String path = utils.trimPathLeft(fullOriginalFilePath.replaceFirst(remoteBasePath, ""));
					path = path.contains("/") ? path.substring(0, path.lastIndexOf("/")) : "";
					logger.debug("Building local resource path: remoteBasePath=" + remoteBasePath + " fullOriginalPath=" + fullOriginalFilePath + " path=" + path);

					String studyDirStr = utils.trimPathRight(utils.getURI(studyDir).toString());
					Resource targetDir = resourceProvider.getResource(studyDirStr + "/" +  path);
					Resource localResource = attemptCopyResource(targetDir, remoteResource);
					if (localResource == null) {
						errorFiles.add(fullOriginalFilePath);
					} else {
						files.add(localResource);
					}
				}

				// register successfully extracted study
				if (errorFiles.isEmpty()) {
					out.add(new Study(study.getStudyId(), study.getVersion(), study.getTimestamp(), studyDir,
							files.toArray(new Resource[0])));
				} else {
					logger.error("Extractor finished with error files: " + errorFiles.stream().collect(
							Collectors.joining(", ")));
					filesNotFound.put(studyId, errorFiles);
				}
			}

		} catch (ConfigurationException e) {
			throw new ExtractionException(e.getMessage(), e);
		} catch (InterruptedException e) {
			throw new ExtractionException("Timeout for resource downloads was interrupted.", e);
		} catch (ResourceCollectionException e) {
			throw new ExtractionException("Cannot copy Resource.", e);
		} catch (DirectoryCreatorException e) {
			throw new ExtractionException("Cannot create directory.", e);
		} catch (IOException e) {
			throw new ExtractionException("Cannot read URI from resource.", e);
		}

		logger.info("Extractor step finished");
		return out.toArray(new Study[0]);
	}

	private Resource attemptCopyResource(Resource destination, Resource remoteResource)
			throws InterruptedException, ResourceCollectionException {
		int i = 1;
		int times = 5;
		Resource r = null;
		while (i++ <= times) {
			try {
				logger.info("Copying resource " + utils.getURI(remoteResource) + " to "+ destination);
				r = resourceProvider.copyFromRemote(destination, remoteResource);
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
					.map(ThrowingFunction.unchecked(e -> utils.getURI(e).toString()))
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
