package org.cbioportal.staging.services.resource;

import java.nio.file.Path;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.springframework.core.io.Resource;

public interface IResourceProvider {

    public Resource[] list(Path dir) throws ResourceCollectionException;
    public Resource[] list(Path dir, boolean recursive) throws ResourceCollectionException;

}
