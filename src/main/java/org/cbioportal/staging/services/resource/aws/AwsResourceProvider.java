package org.cbioportal.staging.services.resource.aws;

import java.util.List;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.resource.IResourceProvider;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResource;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Primary
@Component
@ConditionalOnProperty(value = "scan.location.type" , havingValue = "aws")
public class AwsResourceProvider implements IResourceProvider {

    @Autowired
    protected ResourceUtils utils;

    @Autowired
    private ResourcePatternResolver resourceResolver;

    @Autowired
    private ResourceLoader resourceLoader;

    @Autowired
    private IAwsGateway gateway;

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
            String scanLocation = ((SimpleStorageResource) dir).getS3Uri().toString();
            List<Resource> resources = recursive ? gateway.lsDirRecur(scanLocation) : gateway.lsDir(scanLocation);
            return resources.stream().toArray(Resource[]::new);
        } catch (Exception e) {
            throw new ResourceCollectionException("Could not read from remote directory: " + dir.getFilename(), e);
        }
    }

    @Override
    public Resource copyFromRemote(Resource destinationDir, Resource remoteResource)
            throws ResourceCollectionException {
        try { String fileName = remoteResource.getFilename();
            if (fileName.contains("/"))
                fileName = fileName.substring(fileName.lastIndexOf("/") + 1);
            return utils.copyResource(destinationDir, remoteResource, fileName);
        } catch (Exception e) {
            throw new ResourceCollectionException("Cannot copy resource", e);
        }
    }

    @Override
    public Resource copyToRemote(Resource destinationDir, Resource localResource) throws ResourceCollectionException {
        return copyFromRemote(destinationDir, localResource);
    }

}
