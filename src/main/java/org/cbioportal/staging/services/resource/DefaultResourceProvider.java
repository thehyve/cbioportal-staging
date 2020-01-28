package org.cbioportal.staging.services.resource;

import java.io.IOException;
import java.nio.file.Path;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * DefaultResourceProvider
 * 
 * Lists resources via expansion of paths. This class works
 * for a local file system and for S3 buckets when using 
 * the spring-cloud-aws plugin.
 * 
 */
@Component
public class DefaultResourceProvider implements IResourceProvider {

    @Autowired
    private ResourcePatternResolver resourcePatternResolver;

    @Override
    public Resource[] list(Path dir) throws ResourceCollectionException {
        return list(dir, false);
    }

    @Override
    public Resource[] list(Path dir, boolean recursive) throws ResourceCollectionException {

        String path = dir.toAbsolutePath().toString();
        String wildCardPath = path + "/*";
        if (recursive) {
            wildCardPath += "*";
        }
        try {
            return resourcePatternResolver.getResources(wildCardPath);
        } catch (IOException e) {
            throw new ResourceCollectionException("Could not read from remote directory: " + path, e);
        }
    }

}