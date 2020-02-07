package org.cbioportal.staging.services.resource;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
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
    private ResourceUtils utils;

    @Override
    public Resource[] list(Resource dir) throws ResourceCollectionException {
        return list(dir, false);
    }

    @Override
    public Resource[] list(Resource dir, boolean recursive) throws ResourceCollectionException {

        try {
            String path = utils.trimDir(utils.getURL(dir).toString());
            String wildCardPath = path + "/*";
            if (recursive) {
                wildCardPath += "*";
            }
            if (utils.getFile(dir).isFile()) {
                throw new ResourceCollectionException("Scan location points to a file (should be a directory): " + path);
            }
            return utils.getResources(wildCardPath);
        } catch (ResourceUtilsException e) {
            throw new ResourceCollectionException("Could not read from remote directory: " + dir.getFilename(), e);
        }
    }

}
