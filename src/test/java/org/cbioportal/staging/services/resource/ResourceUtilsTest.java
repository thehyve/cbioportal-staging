package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


import com.amazonaws.services.s3.AmazonS3;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;
import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResource;
import org.springframework.core.io.Resource;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = ResourceUtils.class,
    properties = {
        "scan.location=s3:/my-scanlocation/path/",
        "scan.location.type=aws"
    }
)
public class ResourceUtilsTest {

    @Autowired
    private ResourceUtils utils;

    @MockBean
    private IResourceProvider resourceProvider;

    @Test
    public void trimDir_success() {
        assertEquals("file:/dir", utils.trimPathRight("file:/dir"));
        assertEquals("file:/dir", utils.trimPathRight("file:/dir/"));
        assertEquals("file:/dir", utils.trimPathRight("file:/dir/*"));
        assertEquals("file:/dir", utils.trimPathRight("file:/dir/**"));
        assertEquals("file:/dir", utils.trimPathRight("file:/dir//"));
        assertEquals("file:/dir", utils.trimPathRight("file:/dir//**"));
    }

    @Test
    public void trimFile_success() {
        assertEquals("file:/file.txt", utils.trimPathLeft("file:/file.txt"));
        assertEquals("file.txt", utils.trimPathLeft("/file.txt"));
        assertEquals("file.txt", utils.trimPathLeft("//file.txt"));
    }

    @Test
    public void stripResourceTypePrefix_success() {
        assertEquals("/file.txt", utils.stripResourceTypePrefix("file:/file.txt"));
        assertEquals("/file.txt", utils.stripResourceTypePrefix("s3:/file.txt"));
        assertEquals("/file.txt", utils.stripResourceTypePrefix("file:///file.txt"));
    }

    @Test
    public void getMostRecent_success() {
        Resource[] resources = createResources("prefix", "yaml").toArray(new Resource[0]);
        Resource selectedResource = utils.getMostRecent(resources);
        assertEquals(selectedResource, resources[resources.length - 1]);
    }

    @Test
    public void filterFiles() throws Exception {
        List<Resource> resources = createResources("prefix", "yaml");
        Resource target = TestUtils.createMockResource("dummy", "txt", 0);
        resources.add(target);
        Resource[] selectedResources = utils.filterFiles(resources.toArray(new Resource[0]), "dummy", "txt");
        assert (selectedResources.length == 1);
        assertEquals(selectedResources[0], target);
    }

    @Test
    public void getBasePath_identicalStrings() throws ConfigurationException {
        List<String> paths = new ArrayList<>();
        paths.add("file:/abc");
        paths.add("file:/abc");
        String common = utils.getBasePath(paths);
        assertEquals("file:/", common);
    }

    @Test
    public void getBasePath_stringsDifferentLength() throws ConfigurationException {
        List<String> paths = new ArrayList<>();
        paths.add("file:/abc");
        paths.add("file:/abcdef");
        String common = utils.getBasePath(paths);
        assertEquals("file:/", common);
    }

    @Test
    public void getBasePath_differentStrings() throws ConfigurationException {
        List<String> paths = new ArrayList<>();
        paths.add("file:/abc/def");
        paths.add("file:/abc/ghi");
        String common = utils.getBasePath(paths);
        assertEquals("file:/abc", common);
    }

    @Test
    public void getBasePath_includesEmptyString() throws ConfigurationException {
        List<String> paths = new ArrayList<>();
        paths.add("");
        paths.add("file:/def");
        String common = utils.getBasePath(paths);
        assertEquals("", common);
    }

    @Test
    public void getBasePath_resolveNestedDirs() throws ConfigurationException {
        List<String> paths = new ArrayList<>();
        paths.add("file:/def/xz/file.txt");
        paths.add("file:/def/file.txt");
        String common = utils.getBasePath(paths);
        assertEquals("file:/def", common);
    }

    @Test
    public void getBasePath_resolveNestedDirsWithLongFileName() throws ConfigurationException {
        List<String> paths = new ArrayList<>();
        paths.add("file:/def/xz/file.txt");
        paths.add("file:/def/filenamelongerthanthepreviouspath.txt");
        String common = utils.getBasePath(paths);
        assertEquals("file:/def", common);
    }

    @Test
    public void getBasePath_multipleSlashes() throws ConfigurationException {
        List<String> paths = new ArrayList<>();
        paths.add("file:/def/xz/file.txt");
        paths.add("file:/def//file.txt");
        paths.add("file:///def/file.txt");
        String common = utils.getBasePath(paths);
        assertEquals("file:/def", common);
    }

    @Test
    public void getBasePath_includesNullArgument() throws ConfigurationException {
        List<String> paths = new ArrayList<>();
        paths.add(null);
        paths.add("file:/def");
        String common = utils.getBasePath(paths);
        assertEquals("file:/", common);
    }

    @Test
    public void getBasePath_emptyString() throws ConfigurationException {
        List<String> paths = new ArrayList<>();
        paths.add("");
        String common = utils.getBasePath(paths);
        assertEquals("", common);
    }

    @Test
    public void getBasePath_resourceType() throws ConfigurationException {
        List<String> paths = new ArrayList<>();
        paths.add("s3:///def/xz/file.txt");
        paths.add("s3:///def/file.txt");
        String common = utils.getBasePath(paths);
        assertEquals("s3:///def", common);
    }

    @Test
    public void testRemotePath_success() throws ResourceUtilsException, URISyntaxException {
        URI uri = new URI("ftp:/dummy_host/file.txt");
        assertEquals("/file.txt", utils.remotePath("dummy_host", uri));
    }

    @Test
    public void createRemoteResourcePath_successSftp() throws ResourceUtilsException, MalformedURLException {
        SftpFileInfo fileInfo = mock(SftpFileInfo.class);
        when(fileInfo.getRemoteDirectory()).thenReturn("/root_dir/");
        when(fileInfo.getFilename()).thenReturn("file.txt");
        assertEquals("ftp:/dummy_host/root_dir/file.txt", utils.createRemoteURL("ftp", "dummy_host", fileInfo).toString());
    }

    private List<Resource> createResources(String prefix, String extension) {
        List<Resource> resources = new ArrayList<>();
        IntStream.range(0,4).forEach(i -> {
            resources.add(TestUtils.createMockResource(prefix, extension, i));
        });
        return resources;
    }

    @Test
    public void evalUrlDomain_successHttp() throws ResourceUtilsException {
        assertEquals("my-domain", utils.evalUrlDomain("https://my-domain/index.html"));
    }

    @Test
    public void evalUrlDomain_successFile() throws ResourceUtilsException {
        assertEquals("my-domain", utils.evalUrlDomain("file:///my-domain/test.csv"));
    }

    @Test
    public void evalUrlDomain_successS3SingleSlash() throws ResourceUtilsException {
        assertEquals("my-domain", utils.evalUrlDomain("s3:/my-domain/test.csv"));
    }

    @Test
    public void evalUrlDomain_successS3TwoSlash() throws ResourceUtilsException {
        assertEquals("my-domain", utils.evalUrlDomain("s3://my-domain/test.csv"));
    }

    @Test
    public void evalUrlDomain_successS3ThreeSlash() throws ResourceUtilsException {
        assertEquals("my-domain", utils.evalUrlDomain("s3:///my-domain/test.csv"));
    }

    @Test(expected = ResourceUtilsException.class)
    public void evalUrlDomain_throwsExceptionNoDomain() throws ResourceUtilsException {
        utils.evalUrlDomain("test.csv");
    }

    @Test(expected = ResourceUtilsException.class)
    public void evalUrlDomain_throwsExceptionNullArgument() throws ResourceUtilsException {
        utils.evalUrlDomain(null);
    }

    @Test
    public void correctS3ResourcePathForScanLocation_success() throws URISyntaxException {
        SimpleStorageResource resource = mock(SimpleStorageResource.class);
        when(resource.getS3Uri())
            .thenReturn(new URI("s3:///my-scanlocation/path/study/meta_study.txt"));
        assertEquals("study/meta_study.txt", utils.correctS3ResourcePathForScanLocation(resource));
    }

    @Test
    public void correctS3ResourcePathForScanLocation_failNullArgument() throws URISyntaxException {
        assertNull(utils.correctS3ResourcePathForScanLocation(null));
    }

    @Test
    public void extractDirs_successS3() throws URISyntaxException, ResourceUtilsException {
        SimpleStorageResource resource = mock(SimpleStorageResource.class);
        when(resource.getAmazonS3())
            .thenReturn(mock(AmazonS3.class));
        when(resource.getS3Uri())
            .thenReturn(new URI("s3:///my-scanlocation/path/study/meta_study.txt"));
        Resource[] resources = new Resource[] {resource};
        Resource[] dirs = utils.extractDirs(resources);
        assertEquals(1, dirs.length);
        assertEquals("path/study", dirs[0].getFilename());
        assertNotNull(((SimpleStorageResource) dirs[0]).getAmazonS3());
        assertEquals("s3://my-scanlocation/path/study", ((SimpleStorageResource) dirs[0]).getS3Uri().toString());
    }
}