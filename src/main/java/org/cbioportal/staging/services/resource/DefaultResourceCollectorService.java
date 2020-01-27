package org.cbioportal.staging.services.resource;

import java.nio.file.Paths;
import java.util.Map;

import com.amazonaws.services.s3.model.AmazonS3Exception;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * DefaultResouceCollector
 */
@Component
public class DefaultResourceCollectorService implements IResourceCollectorService {

    private static final Logger logger = LoggerFactory.getLogger(DefaultResourceCollectorService.class);

	@Autowired
    private IResourceProvider resourceProvider;

    @Autowired
    private IStudyResourceResolver resourceStrategy;

    @Autowired
    private IResourceFilter resourceFilter;

    @Autowired
    private ResourceUtils utils;

    @Override
    public Map<String,Resource[]> getResources(String scanLocation) throws ConfigurationException, ResourceCollectionException {

        if (scanLocation == null) {
            throw new ConfigurationException("scan location is null.");
        }

        Map<String,Resource[]> resources;
        String trimmedScanLocation = utils.trimDir(scanLocation);

        try {
            logger.info("Scanning location for files: " + trimmedScanLocation);
            Resource[] scannedResources = resourceProvider.list(Paths.get(trimmedScanLocation));
            logger.info("Found " + scannedResources.length + " files");

            Map<String,Resource[]> resolvedResources = resourceStrategy.resolveResources(scannedResources);

            resources = resourceFilter.filterResources(resolvedResources);

		} catch (ResourceCollectionException e) {
			throw e;
        } catch (AmazonS3Exception e) {
			throw new ResourceCollectionException("Cannot reach Amazon S3 resource at scan.location: " + trimmedScanLocation);
		} catch (Exception e) {
			throw new ResourceCollectionException("Error while retrieving resources from scan.location: " + trimmedScanLocation);
        }

        return resources;
    }
}