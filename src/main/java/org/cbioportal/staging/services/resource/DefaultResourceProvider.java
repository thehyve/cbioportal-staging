package org.cbioportal.staging.services.resource;

import java.util.stream.Stream;

import com.pivovarit.function.ThrowingPredicate;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * DefaultResourceProvider
 *
 * Lists resources via expansion of paths. This class works for a local file
 * system and for S3 buckets when using the spring-cloud-aws plugin.
 *
 */
@Component
public class DefaultResourceProvider implements IResourceProvider {

    @Autowired
    protected ResourceUtils utils;

    @Autowired
    private ResourcePatternResolver resourceResolver;

    @Override
    public Resource getResource(String url) throws ResourceCollectionException {
		return resourceResolver.getResource(url);
    }

    @Override
    public Resource[] list(Resource dir) throws ResourceCollectionException {
        return list(dir, false);
    }

    @Override
    public Resource[] list(Resource dir, boolean recursive) throws ResourceCollectionException {
        return list(dir, recursive, false);
    }

    @Override
    public Resource[] list(Resource dir, boolean recursive, boolean filterDirs) throws ResourceCollectionException {

        try {
            String path = utils.trimPathRight(utils.getURL(dir).toString());
            String wildCardPath = path + "/*";
            if (recursive) {
                wildCardPath += "*/*";
            }
            if (dir.isFile() && utils.getFile(dir).isFile()) {
                throw new ResourceCollectionException("Scan location points to a file (should be a directory): " + path);
            }
            Resource[] res = utils.getResources(wildCardPath);
            if (! filterDirs)
                return res;
            return Stream.of(res).filter(ThrowingPredicate.sneaky(e -> e.getFile().isFile())).toArray(Resource[]::new);
        } catch (ResourceUtilsException e) {
            throw new ResourceCollectionException("Could not read from remote directory: " + dir.getFilename(), e);
        }
    }

    @Override
    public Resource copyFromRemote(Resource destinationDir, Resource remoteResource)
            throws ResourceCollectionException {
        try {
            return utils.copyResource(destinationDir, remoteResource, remoteResource.getFilename());
        } catch (Exception e) {
            throw new ResourceCollectionException("Cannot copy resource", e);
        }
    }

    @Override
    public Resource copyToRemote(Resource destinationDir, Resource localResource) throws ResourceCollectionException {
        return copyFromRemote(destinationDir, localResource);
    }

}
