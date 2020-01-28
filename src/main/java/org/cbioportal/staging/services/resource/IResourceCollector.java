package org.cbioportal.staging.services.resource;

import java.util.Map;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.springframework.core.io.Resource;

/**
 * ResourceCollector
 */
public interface IResourceCollector {

    Map<String,Resource[]> getResources(Resource scanLocation) throws ResourceCollectionException, ConfigurationException;

}