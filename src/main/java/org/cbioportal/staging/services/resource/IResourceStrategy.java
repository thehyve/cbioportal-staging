package org.cbioportal.staging.services.resource;

import java.util.Map;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.springframework.core.io.Resource;

public interface IResourceStrategy {

    Map<String,Resource[]> resolveResources(Resource[] resources) throws ResourceCollectionException;

}
