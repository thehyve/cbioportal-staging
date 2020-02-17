package org.cbioportal.staging.services.resource;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public interface IResourceProvider {

    public Resource[] list(Resource dir) throws ResourceCollectionException;
    public Resource[] list(Resource dir, boolean recursive) throws ResourceCollectionException;
    public Resource[] list(Resource dir, boolean recursive, boolean filterDirs) throws ResourceCollectionException;

}
