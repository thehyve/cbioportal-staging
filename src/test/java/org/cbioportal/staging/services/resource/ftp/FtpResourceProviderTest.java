package org.cbioportal.staging.services.resource.ftp;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.FtpUtilsException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.Resource;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * FtpResourceProviderTest
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { FtpResourceProvider.class, FtpUtils.class, ResourceUtils.class }, properties = {
        "ftp.enable=true", "ftp.host=host" })
public class FtpResourceProviderTest {

    @Autowired
    private FtpResourceProvider provider;

    @MockBean
    private IFtpGateway ftpGateway;

    @SpyBean
    private FtpUtils ftpUtils;

    Resource scanDir = TestUtils.createMockResource("ftp:/host/test", 0);
    List<SftpFileInfo> filesInfo;

    @Before
    public void init() throws FtpUtilsException {

        doReturn("/test").when(ftpUtils).remotePath(any());

        SftpFileInfo fileInfo1 = mock(SftpFileInfo.class);
        when(fileInfo1.getRemoteDirectory()).thenReturn("/root_dir/");
        when(fileInfo1.getFilename()).thenReturn("file1.txt");
        when(fileInfo1.isDirectory()).thenReturn(false);

        SftpFileInfo fileInfo2 = mock(SftpFileInfo.class);
        when(fileInfo2.getRemoteDirectory()).thenReturn("/root_dir/");
        when(fileInfo2.getFilename()).thenReturn("dir");
        when(fileInfo2.isDirectory()).thenReturn(true);

        filesInfo = new ArrayList<>();
        filesInfo.add(fileInfo1);
        filesInfo.add(fileInfo2);

        when(ftpGateway.lsDir(anyString())).thenReturn(filesInfo);
    }

    @Test
    public void testList_success() throws ResourceCollectionException, FtpUtilsException, IOException {
        Resource[] res = provider.list(scanDir, false, false);
        assert(res.length == 2);
        assertEquals("ftp:/host/root_dir/file1.txt", res[0].getURL().toString());
        assertEquals("ftp:/host/root_dir/dir", res[1].getURL().toString());
    }

    @Test
    public void testList_exludeDirs() throws ResourceCollectionException, FtpUtilsException, IOException {
        Resource[] res = provider.list(scanDir, false, true);
        assert(res.length == 1);
        assertEquals("ftp:/host/root_dir/file1.txt", res[0].getURL().toString());
    }

}