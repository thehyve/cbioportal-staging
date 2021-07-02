package org.cbioportal.staging.services.resource;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResource;
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
public class ResourceCollectorService implements IResourceCollector {

    private static final Logger logger = LoggerFactory.getLogger(ResourceCollectorService.class);

    @Autowired
    private IResourceProvider resourceProvider;

    @Autowired
    private IStudyResourceStrategy resourceStrategy;

    @Autowired
    private IResourceFilter resourceFilter;

    @Override
    public Study[] getResources(Resource scanLocation)
            throws ConfigurationException, ResourceCollectionException {

        if (scanLocation == null) {
            throw new ConfigurationException("scan location is null.");
        }

        Study[] resources = new Study[0];

        try {
            String scanLocationString = scanLocation.getFilename();
            if (scanLocation instanceof SimpleStorageResource) {
                scanLocationString = ((SimpleStorageResource) scanLocation).getS3Uri().toString();
            }
            logger.info("Scanning for files at: " + scanLocationString);
            Resource[] scannedResources = resourceProvider.list(scanLocation);
            logger.info("Found " + scannedResources.length + " files");

            if (scannedResources.length > 0) {
                Study[] resolvedResources = resourceStrategy.resolveResources(scannedResources);
                resources = resourceFilter.filterResources(resolvedResources);
            }

        } catch (ResourceCollectionException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceCollectionException("Error while retrieving resources from scan.location: " + scanLocation.getFilename(), e);
        }

        return resources;
    }
}