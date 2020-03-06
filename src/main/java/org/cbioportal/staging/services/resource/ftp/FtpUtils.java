package org.cbioportal.staging.services.resource.ftp;

import java.net.MalformedURLException;
import java.net.URL;

import org.cbioportal.staging.exceptions.FtpUtilsException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.stereotype.Component;

@Component
public class FtpUtils {

    @Value("${ftp.host:}")
    private String ftpHost;

    @Autowired
    private ResourceUtils utils;

    /**
     * Strip protocol and host name from URL.
     *
     * The resulting path can be used by the FTP server to indicate
     * resources for upload/download.
     *
     * Example: URL['ftp:/host/file.txt'] becomes '/file.txt'.
     *
     * @param path URL of item on FTP server
     * @return String  path to item from perspective of FTP server
     * @throws FtpUtilsException
     */
    public String remotePath(URL path) throws FtpUtilsException {
        String pathStr = path.toString();

        if (!pathStr.startsWith("ftp:/"))
            throw new FtpUtilsException("Resource does not point to an ftp resource.");

        pathStr = utils.trimPathLeft(utils.stripResourceTypePrefix(pathStr));

        if (!pathStr.startsWith(ftpHost))
            throw new FtpUtilsException("Resource path does not start with ftp.host specified in properties file.");

        return pathStr.replaceFirst(ftpHost, "");
    }

    /**
     * Build URL for remote item from SftpFileInfo object.
     *
     * Example: URL['ftp:///host/root_dir/file.txt'].
     * Host name is resolved from the ftp.host application property.
     *
     * @param fileInfo file info object
     * @return URL
     * @throws FtpUtilsException
     */
    public URL createRemoteURL(SftpFileInfo fileInfo) throws FtpUtilsException {
        StringBuilder bffr = new StringBuilder();
        bffr.append(fileInfo.getRemoteDirectory()).append(fileInfo.getFilename());
        return createRemoteURL(bffr.toString());
    }

    /**
     * Convert path on FTP server to URL.
     *
     * Example: '/file.txt' becomes URL['ftp:///host/file.txt']
     * Host name is resolved from the ftp.host application property.
     *
     * @param remoteFilePath  path to item from perspective of FTP server
     * @return URL
     */
    public URL createRemoteURL(String remoteFilePath) throws FtpUtilsException {
        StringBuilder bffr = new StringBuilder();
        bffr.append("ftp:///").append(ftpHost).append(remoteFilePath);
        try {
            return new URL(bffr.toString());
        } catch (MalformedURLException e) {
            throw new FtpUtilsException(e);
        }
    }

}