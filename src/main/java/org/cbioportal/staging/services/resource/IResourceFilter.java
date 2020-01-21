package org.cbioportal.staging.services.resource;

import java.util.Map;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.springframework.core.io.Resource;

public interface IResourceFilter {

    Map<String,Resource[]> filterResources(Map<String,Resource[]> resources) throws ResourceCollectionException;

}
