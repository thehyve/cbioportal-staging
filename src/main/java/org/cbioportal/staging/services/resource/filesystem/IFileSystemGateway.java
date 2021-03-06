package org.cbioportal.staging.services.resource.filesystem;

import java.io.File;
import java.io.InputStream;
import java.util.List;

import org.springframework.integration.annotation.Gateway;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.messaging.handler.annotation.Header;

@MessagingGateway
public interface IFileSystemGateway {

    @Gateway(requestChannel = "local.resource.ls")
    public List<File> ls(String dir);

    @Gateway(requestChannel = "local.resource.ls.dir")
    public List<File> lsDir(String dir);

    @Gateway(requestChannel = "local.resource.ls.dir.recur")
    public List<File> lsDirRecur(String dir);

    @Gateway(requestChannel = "local.resource.get.stream")
    public InputStream getStream(String file);

    @Gateway(requestChannel = "local.resource.put")
    public String put(byte[] bytes, @Header("dest.dir") String destinationDir, @Header("filename") String fileName);

    @Gateway(requestChannel = "local.resource.rm")
    public String rm(String file);

}