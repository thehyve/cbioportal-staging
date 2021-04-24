package org.cbioportal.staging.services.resource.filesystem;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.FtpUtilsException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * DefaultResourceProviderTest
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { FileSystemResourceProvider.class, ResourceUtils.class })
public class FileSystemResourceProviderTest {

    @Autowired
    private FileSystemResourceProvider provider;

    @SpyBean
    private ResourceUtils utils;

	@MockBean
	private IFileSystemGateway gateway;

    Resource scanDir = TestUtils.createMockResource("file:/scandir/", 0);
    List<File> files;

    @Before
    public void init() throws FtpUtilsException, ResourceUtilsException, IOException {

        when(scanDir.getFile().isFile()).thenReturn(false); // hack to prevent dir checking

        File file1 = spy(new File("/scandir/file1.txt"));
        File file2 = spy(new File("/scandir/dir/"));


        files = new ArrayList<File>();
        files.add(file1);
        files.add(file2);

        when(gateway.lsDir(any())).thenReturn(files);
    }

    @Test
    public void testList_success() throws ResourceCollectionException, FtpUtilsException, IOException {
        Resource[] res = provider.list(scanDir, false, false);
        assertTrue(res.length == 2);
        assertEquals("file:/scandir/file1.txt", res[0].getURI().toString());
        assertEquals("file:/scandir/dir", res[1].getURI().toString());
    }

    @Test
    public void testList_exludeDirs() throws ResourceCollectionException, FtpUtilsException, IOException {
        when(files.get(0).isFile()).thenReturn(true);
        when(files.get(1).isFile()).thenReturn(false);
        Resource[] res = provider.list(scanDir, false, true);
        assertTrue(res.length == 1);
        assertEquals("file:/scandir/file1.txt", res[0].getURI().toString());
    }

}