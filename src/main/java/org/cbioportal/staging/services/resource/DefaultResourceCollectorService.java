package org.cbioportal.staging.services.resource;

import java.util.Map;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * DefaultResouceCollector
 *
 * Complies a list of resources per study on the file system indicated by
 * 'scan.location' property.
 *
 */
@Component
public class DefaultResourceCollectorService implements IResourceCollector {

    private static final Logger logger = LoggerFactory.getLogger(DefaultResourceCollectorService.class);

    @Autowired
    private IResourceProvider resourceProvider;

    @Autowired
    private IStudyResourceStrategy resourceStrategy;

    @Autowired
    private IResourceFilter resourceFilter;

    @Override
    public Map<String, Resource[]> getResources(Resource scanLocation)
            throws ConfigurationException, ResourceCollectionException {

        if (scanLocation == null) {
            throw new ConfigurationException("scan location is null.");
        }

        Map<String,Resource[]> resources;

        try {

            logger.info("Scanning for files at: " + scanLocation.getURL().toString());
            Resource[] scannedResources = resourceProvider.list(scanLocation);
            logger.info("Found " + scannedResources.length + " files");

            Map<String,Resource[]> resolvedResources = resourceStrategy.resolveResources(scannedResources);

            resources = resourceFilter.filterResources(resolvedResources);

        } catch (ResourceCollectionException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceCollectionException("Error while retrieving resources from scan.location: " + scanLocation.getFilename());
        }

        return resources;
    }
}