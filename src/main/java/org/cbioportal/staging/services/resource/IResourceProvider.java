package org.cbioportal.staging.services.resource;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public interface IResourceProvider {

    public Resource getResource(String url) throws ResourceCollectionException;
    public Resource[] list(Resource dir) throws ResourceCollectionException;
    public Resource[] list(Resource dir, boolean recursive) throws ResourceCollectionException;
    public Resource[] list(Resource dir, boolean recursive, boolean filterDirs) throws ResourceCollectionException;
    public Resource copyFromRemote(Resource destinationDir, Resource remoteResource) throws ResourceCollectionException;
    public Resource copyToRemote(Resource destinationDir, Resource localResource) throws ResourceCollectionException;

}
