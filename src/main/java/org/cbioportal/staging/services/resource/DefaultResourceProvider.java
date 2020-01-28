package org.cbioportal.staging.services.resource;

import java.io.IOException;

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

    @Autowired
    private ResourceUtils utils;

    @Override
    public Resource[] list(Resource dir) throws ResourceCollectionException {
        return list(dir, false);
    }

    @Override
    public Resource[] list(Resource dir, boolean recursive) throws ResourceCollectionException {

        try {
            String path = utils.trimDir(dir.getURL().toString());
            String wildCardPath = path + "/*";
            if (recursive) {
                wildCardPath += "*";
            }
            if (dir.getFile().isFile()) {
                throw new ResourceCollectionException("Scan location points to a file (should be a directory): " + path);
            }
            return resourcePatternResolver.getResources(wildCardPath);
        } catch (IOException e) {
            throw new ResourceCollectionException("Could not read from remote directory: " + dir.getFilename(), e);
        }
    }

}