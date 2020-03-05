package org.cbioportal.staging.services.resource.ftp;

import org.cbioportal.staging.exceptions.FtpUtilsException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.stereotype.Component;

@Component
public class FtpUtils {

    @Value("${sftp.host:localhost}")
    private String sftpHost;

    @Autowired
    private ResourceUtils utils;

    // strip protocol and host from URL
    public String remotePath(Resource path) throws FtpUtilsException {
        try {
            String pathStr = utils.getURL(path).toString();
            pathStr = utils.trimFile(utils.stripResourceTypePrefix(pathStr));
            return pathStr.replaceFirst(sftpHost, "");
        } catch (ResourceUtilsException e) {
            throw new FtpUtilsException(e);
        }
    }

    public String createRemoteResourcePath(SftpFileInfo fileInfo) {
        StringBuilder bffr = new StringBuilder();
        bffr.append(fileInfo.getRemoteDirectory())
            .append(fileInfo.getFilename());
        return createRemoteResourcePath(bffr.toString());
    }

    public String createRemoteResourcePath(String remoteFilePath) {
        StringBuilder bffr = new StringBuilder();
        bffr.append("ftp:///")
            .append(sftpHost)
            .append(remoteFilePath);
        return bffr.toString();
    }

}