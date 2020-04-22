package org.cbioportal.staging.services.resource;

import org.cbioportal.staging.exceptions.ResourceCollectionException;

public interface IResourceFilter {

    Study[] filterResources(Study[] resources) throws ResourceCollectionException;

}
