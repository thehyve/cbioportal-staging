package org.cbioportal.staging.services.resource.filesystem;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import com.pivovarit.function.ThrowingPredicate;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.resource.IResourceProvider;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "scan.location.type", havingValue = "filesystem")
public class FileSystemResourceProvider implements IResourceProvider {

    @Autowired
    protected ResourceUtils utils;

    @Autowired
    private ResourcePatternResolver resourceResolver;

    @Autowired
    private IFileSystemGateway gateway;

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
            if (utils.getFile(dir).isFile()) {
                throw new ResourceCollectionException(
                        "Scan location points to a file (should be a directory): " + path);
            }

            String remoteDir = utils.remotePath(null, dir.getURL());

            List<File> files = recursive ? gateway.lsDirRecur(remoteDir) : gateway.lsDir(remoteDir);

            if (filterDirs)
                files = files.stream().filter(ThrowingPredicate.sneaky(e -> e.isFile()))
                        .collect(Collectors.toList());

            return files.stream()
                .map(FileSystemResource::new)
                .toArray(Resource[]::new);
        } catch (Exception e) {
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
