package org.cbioportal.staging.services.resource.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.cbioportal.staging.services.resource.ResourceUtils;
import org.springframework.core.io.UrlResource;

public class FtpResource extends UrlResource {

    private IFtpGateway gateway;
    private ResourceUtils utils;
    private String hostName;

    public FtpResource(String hostName, String url, IFtpGateway gateway, ResourceUtils utils) throws MalformedURLException {
        super(url);
        this.gateway = gateway;
        this.utils = utils;
        this.hostName = hostName;
    }

    public FtpResource(String hostName, URL url, IFtpGateway gateway, ResourceUtils utils) throws MalformedURLException {
        super(url);
        this.gateway = gateway;
        this.utils = utils;
        this.hostName = hostName;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            return gateway.getStream(utils.remotePath(hostName, this.getURL()));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}