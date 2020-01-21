package org.cbioportal.staging.services.resource;

import java.io.IOException;
import java.nio.file.Path;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class DefaultResourceProvider implements IResourceProvider {

    @Autowired
    private ResourcePatternResolver resourcePatternResolver;

    @Override
    public Resource[] list(Path dir) throws ResourceCollectionException {
        String path = dir.toAbsolutePath().toString();
        String wildCardPath = path + "/*";
        try {
            return resourcePatternResolver.getResources(wildCardPath);
        } catch (IOException e) {
            throw new ResourceCollectionException("Could not read from remote directory: " + path, e);
        }
    }

}