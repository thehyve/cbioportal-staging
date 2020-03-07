package org.cbioportal.staging.services.resource.ftp;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.IOException;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.FtpUtilsException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.resource.DefaultResourceProvider;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * DefaultResourceProviderTest
 */
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { DefaultResourceProvider.class, ResourceUtils.class })
public class DefaultResourceProviderTest {

    @Autowired
    private DefaultResourceProvider provider;

    @SpyBean
    private ResourceUtils utils;

    Resource scanDir = TestUtils.createMockResource("file:/scandir/", 0);
    Resource[] resources;

    @Before
    public void init() throws FtpUtilsException, ResourceUtilsException {

        Resource file1 = TestUtils.createMockResource("file:/scandir/file1.txt", 0);
        Resource file2 = TestUtils.createMockResource("file:/scandir/dir/", 0);

        when(scanDir.isFile()).thenReturn(false); // hack to prevent dir checking

        resources = new Resource[] {file1, file2};

        when(utils.getResources(anyString())).thenReturn(resources);
    }

    @Test
    public void testList_success() throws ResourceCollectionException, FtpUtilsException, IOException {
        Resource[] res = provider.list(scanDir, false, false);
        assertTrue(res.length == 2);
        assertEquals("file:/scandir/file1.txt", res[0].getURL().toString());
        assertEquals("file:/scandir/dir/", res[1].getURL().toString());
    }

    @Test
    public void testList_exludeDirs() throws ResourceCollectionException, FtpUtilsException, IOException {
        when(resources[1].getFile().isFile()).thenReturn(false);
        Resource[] res = provider.list(scanDir, false, true);
        assertTrue(res.length == 1);
        assertEquals("file:/scandir/file1.txt", res[0].getURL().toString());
    }

}