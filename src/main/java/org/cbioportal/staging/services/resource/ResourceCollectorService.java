package org.cbioportal.staging.services.resource;

import static org.cbioportal.staging.etl.ETLProcessRunner.Stage;


import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
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
    // This one is determined by the @Primary annotation of the instantiated Strategy beans
    private IStudyResourceStrategy remoteResourceStrategy;

//    @Autowired
//    private FolderStudyResourceStrategy localResourceStrategy;

    @Autowired
    private IResourceFilter resourceFilter;

    @Value("${execution.stage:ALL}")
    private Stage executionStage;

    @Override
    public Study[] getResources(Resource scanLocation)
            throws ConfigurationException, ResourceCollectionException {

        if (scanLocation == null) {
            throw new ConfigurationException("scan location is null.");
        }

        Study[] studies = null;

        try {
            String scanLocationString = scanLocation.getFilename();
            if (scanLocation instanceof SimpleStorageResource) {
                scanLocationString = ((SimpleStorageResource) scanLocation).getS3Uri().toString();
            }

            logger.info("Scanning for files at: " + scanLocationString);
            Resource[] scannedResources = resourceProvider.list(scanLocation);
            logger.info("Found " + scannedResources.length + " files");

            if (scannedResources.length > 0) {
                Study[] resolvedResources;
//                if (executionStage == Stage.ALL || executionStage == Stage.EXTRACT) {
                    resolvedResources = remoteResourceStrategy.resolveResources(scannedResources);
//                } else {
                    // When only running TRANSFORM, VALIDATION, or LOAD step, resolve the resources
                    // using a simple local file resource strategy (no need to interpret yaml files etc.).
//                    resolvedResources = localResourceStrategy.resolveResources(scannedResources);
//                }
                studies = resourceFilter.filterResources(resolvedResources);
            }

        } catch (ResourceCollectionException e) {
            throw e;
        } catch (Exception e) {
            throw new ResourceCollectionException("Error while retrieving studies from scan.location: " + scanLocation.getFilename(), e);
        }

        return studies;
    }
}