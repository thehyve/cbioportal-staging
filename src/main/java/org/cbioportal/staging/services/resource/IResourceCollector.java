package org.cbioportal.staging.services.resource;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.springframework.core.io.Resource;

/**
 * ResourceCollector
 */
public interface IResourceCollector {

    Study[] getResources(Resource scanLocation) throws ResourceCollectionException, ConfigurationException;

}