package org.cbioportal.staging.services.resource.ftp;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.MalformedURLException;
import java.net.URL;

import org.cbioportal.staging.exceptions.FtpUtilsException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { FtpUtils.class, ResourceUtils.class }, properties = { "ftp.host=dummy_host" })
public class FtpUtilsTest {

    @Autowired
    private FtpUtils ftpUtils;

    @Test
    public void testRemotePath_success() throws FtpUtilsException, MalformedURLException {
        URL url = new URL("ftp:/dummy_host/file.txt");
        assertEquals("/file.txt", ftpUtils.remotePath(url));
    }

    @Test(expected = FtpUtilsException.class)
    public void testRemotePath_wrongHost() throws FtpUtilsException, MalformedURLException {
        URL url = new URL("ftp:/wrong_host/file.txt");
        ftpUtils.remotePath(url);
    }

    @Test(expected = FtpUtilsException.class)
    public void testRemotePath_notFtpResource() throws FtpUtilsException, MalformedURLException {
        URL url = new URL("file:/wrong_host/file.txt");
        ftpUtils.remotePath(url);
    }

    @Test
    public void createRemoteResourcePath_success() throws FtpUtilsException, MalformedURLException {
        SftpFileInfo fileInfo = mock(SftpFileInfo.class);
        when(fileInfo.getRemoteDirectory()).thenReturn("/root_dir/");
        when(fileInfo.getFilename()).thenReturn("file.txt");
        assertEquals("ftp:/dummy_host/root_dir/file.txt", ftpUtils.createRemoteURL(fileInfo).toString());
    }

}