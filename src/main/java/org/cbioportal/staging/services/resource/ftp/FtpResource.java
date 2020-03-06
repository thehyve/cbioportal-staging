package org.cbioportal.staging.services.resource.ftp;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;

import org.springframework.core.io.UrlResource;

public class FtpResource extends UrlResource {

    private IFtpGateway gateway;
    private FtpUtils utils;

    public FtpResource(String url, IFtpGateway gateway, FtpUtils utils) throws MalformedURLException {
        super(url);
        this.gateway = gateway;
        this.utils = utils;
    }

    public FtpResource(URL url, IFtpGateway gateway, FtpUtils utils) throws MalformedURLException {
        super(url);
        this.gateway = gateway;
        this.utils = utils;
    }

    @Override
    public InputStream getInputStream() throws IOException {
        try {
            return gateway.getStream(utils.remotePath(this.getURL()));
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

}