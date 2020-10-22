package org.cbioportal.staging.services.resource.ftp;

import java.net.MalformedURLException;
import java.util.List;
import java.util.stream.Stream;

import com.pivovarit.function.ThrowingFunction;
import com.pivovarit.function.ThrowingPredicate;

import org.apache.commons.io.IOUtils;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.resource.IResourceProvider;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value="scan.location.type", havingValue ="sftp")
@Primary
public class FtpResourceProvider implements IResourceProvider {

    @Value("${ftp.host}")
    private String ftpHost;

    @Autowired
    protected ResourceUtils utils;

    @Autowired
    private IFtpGateway ftpGateway;

    @Override
    public Resource getResource(String url) throws ResourceCollectionException {
        try {
            return new FtpResource(ftpHost, url, ftpGateway, utils);
        } catch (MalformedURLException e) {
            throw new ResourceCollectionException("Malformed URL!", e);
        }
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
    public Resource[] list(Resource dir, boolean recursive, boolean excludeDirs) throws ResourceCollectionException {

        try {
            logger.info("FTP RESOURCE PROVIDER dir:" +dir.toString());
            String remoteDir = utils.remotePath(ftpHost, dir.getURL());

            logger.info("FTP RESOURCE PROVIDER remoteDir:" +remoteDir.toString());

            List<SftpFileInfo> remoteFiles;
            if (recursive) {
                remoteFiles = ftpGateway.lsDirRecur(remoteDir);
            } else {
                remoteFiles = ftpGateway.lsDir(remoteDir);
            }

            logger.info("FTP RESOURCE PROVIDER remoteFiles:" +remoteFiles.toString());

            Stream<SftpFileInfo> remoteFilesStream = remoteFiles.stream();
            if (excludeDirs)
                remoteFilesStream = remoteFilesStream.filter(ThrowingPredicate.sneaky(e -> ! e.isDirectory()));

            return remoteFilesStream
                .map(ThrowingFunction.sneaky(e ->
                        new FtpResource(ftpHost, utils.createRemoteURL("ftp", ftpHost, e), e.isDirectory(), ftpGateway, utils) ))
                .toArray(Resource[]::new);

        } catch (Exception e) {
            throw new ResourceCollectionException("Could not read from remote directory: " + dir.getFilename(), e);
        }
    }

    @Override
    public Resource copyFromRemote(Resource destinationDir, Resource remoteResource) throws ResourceCollectionException {
        try {
            if (remoteResource.getInputStream() == null) {
                remoteResource = getResource(remoteResource.getURL().toString());
            }
            return utils.copyResource(destinationDir, remoteResource, remoteResource.getFilename());
        } catch (Exception e) {
            throw new ResourceCollectionException("Cannot copy resource from remote.", e);
        }
    }

    @Override
    public Resource copyToRemote(Resource destinationDir, Resource localResource)
            throws ResourceCollectionException {
        try {
            String remoteFilePath = ftpGateway.put(
                IOUtils.toByteArray(localResource.getInputStream()),
                utils.remotePath(ftpHost, destinationDir.getURL()),
                localResource.getFilename()
            );

            return getResource(utils.createRemoteURL("ftp", ftpHost, remoteFilePath).toString());
        } catch (Exception e) {
            throw new ResourceCollectionException("Cannot copy resource to remote.", e);
        }
    }

}
