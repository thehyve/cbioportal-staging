package org.cbioportal.staging.services.resource.ftp;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.List;
import java.util.stream.Stream;

import com.pivovarit.function.ThrowingFunction;
import com.pivovarit.function.ThrowingPredicate;

import org.apache.commons.io.IOUtils;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.resource.DefaultResourceProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value="ftp.enable", havingValue ="true")
@Primary
public class FtpResourceProvider extends DefaultResourceProvider {

    @Value("${ftp.host}")
    private String sftpHost;

    @Autowired
    private FtpUtils ftpUtils;

    @Autowired
    private IFtpGateway ftpGateway;

    @Override
    public Resource getResource(String url) throws ResourceCollectionException {
        try {
            return new FtpResource(url, ftpGateway, ftpUtils);
        } catch (MalformedURLException e) {
            throw new ResourceCollectionException("Malformed URL!", e);
        }
    }

    @Override
    public Resource[] list(Resource dir, boolean recursive, boolean excludeDirs) throws ResourceCollectionException {

        try {

            String remoteDir = ftpUtils.remotePath(dir.getURL());

            List<SftpFileInfo> remoteFiles;
            if (recursive) {
                remoteFiles = ftpGateway.lsDirRecur(remoteDir);
            } else {
                remoteFiles = ftpGateway.lsDir(remoteDir);
            }

            Stream<SftpFileInfo> remoteFilesStream = remoteFiles.stream();
            if (excludeDirs)
                remoteFilesStream = remoteFilesStream.filter(ThrowingPredicate.sneaky(e -> ! e.isDirectory()));

            return remoteFilesStream
                .map(ThrowingFunction.sneaky(ftpUtils::createRemoteURL))
                .map(ThrowingFunction.sneaky(r -> new FtpResource(r, ftpGateway, ftpUtils)))
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
            return super.copyFromRemote(destinationDir, remoteResource);
        } catch (IOException e) {
            throw new ResourceCollectionException("Cannot copy resource from remote.", e);
        }
    }

    @Override
    public Resource copyToRemote(Resource destinationDir, Resource localResource)
            throws ResourceCollectionException {
        try {
            String remoteFilePath = ftpGateway.put(
                IOUtils.toByteArray(localResource.getInputStream()),
                ftpUtils.remotePath(destinationDir.getURL()),
                localResource.getFilename()
            );

            return getResource(ftpUtils.createRemoteURL(remoteFilePath).toString());
        } catch (Exception e) {
            throw new ResourceCollectionException("Cannot copy resource to remote.", e);
        }
    }

}
