package org.cbioportal.staging.services.resource;

import java.util.Map;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.springframework.core.io.Resource;

/**
 * ResourceCollector
 */
public interface IResourceCollectorService {

    Map<String,Resource[]> getResources(String scanLocation) throws ResourceCollectionException, ConfigurationException;

}