package org.cbioportal.staging.services.resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.pivovarit.function.ThrowingPredicate;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.RandomStringUtils;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.resource.ftp.FtpResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.integration.sftp.session.SftpFileInfo;
import org.springframework.stereotype.Component;

@Component
public class ResourceUtils {

    @Autowired
    private ResourcePatternResolver resourceResolver;

    @Autowired
    private IResourceProvider resourceProvider;

    @Value("${java.io.tmpdir}")
    private FileSystemResource tempDir;

    /**
     * Remove trailing slashes and asterixes from the right end of a path.
     *
     * Examples:
     * - '/test/' becomes '/test'
     * - '/test//**' becomes '/test'
     *
     * @param dir path
     * @return String trimmed path
     */
    public String trimPathRight(String dir) {
        return dir.replaceFirst("\\/*\\**$", "");
    }

    /**
     * Remove trailing slashes from the left end of a path.
     *
     * Examples:
     * - '/test/' becomes 'test/'
     * - '//test/' becomes 'test/'
     *
     * @param dir  path
     * @return String  trimmed path
     */
    public String trimPathLeft(String dir) {
        return dir.replaceFirst("^\\/*", "");
    }

    /**
     * Remove protocol prefix from URL string.
     *
     * Examples:
     * - 'file:/test/' becomes '/test/'
     * - 'file:///test/' becomes '/test/'
     *
     * @param dir
     * @return String
     */
    public String stripResourceTypePrefix(String dir) {
        String s = dir.replaceFirst("^.*:", "");
        return s.replaceFirst("^\\/+", "/");
    }

    /**
     * Create a timestamp string.
     *
     * Example: pattern "yyyyMMdd-HHmmss" returns '20200303-15:36:01'
     *
     * @param pattern  SimpleDateFormat pattern
     * @return String  formatted timestamp
     */
    public String getTimeStamp(String pattern) {
        return new SimpleDateFormat(pattern).format(new Date());
    }

    /**
     * Filter Resources based on match with pre- and suffix string.
     *
     * @param resources  Resources be filtered
     * @param prefixPattern  String that must be present at the start of a resource filename
     * @param extensionPattern  String that must be present at the end of a resource filename
     * @return Resource[]  list of fitered Resources that match pre- and extensionPattern
     */
    public Resource[] filterFiles(Resource[] resources, String prefixPattern, String extensionPattern) {
        String filePattern = ".*" + prefixPattern + ".*" + extensionPattern + "$";
        return Stream.of(resources).filter(n -> n.getFilename().matches(filePattern)).toArray(Resource[]::new);
    }

    /**
     * Return the Resource with the most recent creation date.
     *
     * @param resources  Resources be filtered
     * @return Resource  Resource with the most recent creation date
     */
    public Resource getMostRecent(Resource[] resources) {
        Resource file = null;
        try {
            for (Resource resource : resources) {
                if (file == null || resource.lastModified() > file.lastModified()) {
                    file = resource;
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return file;
    }

    /**
     * Filter Resources to include directories exclusively.
     *
     * @param resources  Resources be filtered
     * @return Resource[]  List of directory Resources
     */
    public Resource[] extractDirs(Resource[] resources) {
        return Stream.of(resources)
            .filter(ThrowingPredicate.unchecked(
                    n -> {
                        if (n.isFile())
                            return n.getFile().isDirectory();
                        if (n instanceof FtpResource)
                            return ((FtpResource) n).isDirectory();
                        return false;
                    }))
            .toArray(Resource[]::new);
    }

    /**
     * Read the contents of a cBioPortal meta_study.txt file into a Map.
     *
     * @param studyMetaFile  Resource pointing to a meta_study.txt file
     * @return Map<String, String>  Key-value pairs of entries in meta_study.txt file
     * @throws ResourceUtilsException
     */
    public Map<String, String> readMetaFile(Resource studyMetaFile) throws ResourceUtilsException {
        BufferedReader bufferedReader = null;
        Map<String, String> entries = new HashMap<>();
        try {
            // The meta file may be located on a remote location.
            // Copy meta file to the /tmp dir and read contents.
            String uniqueKey = RandomStringUtils.randomAlphabetic(20);
            Resource tmpLocation = resourceResolver.getResource(tempDir.getURL().toString() + uniqueKey);
            Resource localStudyMetaFile = resourceProvider.copyFromRemote(tmpLocation, studyMetaFile);
            bufferedReader = new BufferedReader(new FileReader(localStudyMetaFile.getFile()));
            String line = bufferedReader.readLine();
            while (line != null) {
                String[] elements = line.split("\\s*:\\s*");
                entries.put(elements[0].trim(), elements[1].trim());
                line = bufferedReader.readLine();
            }
        } catch (Exception e) {
            throw new ResourceUtilsException("Error reading the study_meta.txt file.", e);
        } finally {
            if (bufferedReader != null)
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    throw new ResourceUtilsException("Error reading the study_meta.txt file.", e);
                }
        }
        return entries;
    }

    /**
     * Identify shared path to collection of Resources.
     *
     * This method gets the "base path" of all entries. I.e. it assumes all entries
     * share a common parent path on the S3 or other resource folder where they are
     * originally shared. So for the following list of files configured in the list
     * of studies yaml as below:
     *
     * study1:
     * - folder/study1path/fileA.txt
     * - folder/study1path/fileB.txt
     * - folder/study1path/mafs/maf1.maf
     * - folder/study1path/mafs/mafn.maf
     *
     * this method will return "folder/study1path".
     *
     * @param paths  List of Resources with common base path
     * @throws ConfigurationException
     */
    public String getBasePath(List<String> paths) throws ConfigurationException {

        List<String> pathsNoNull = paths.stream().filter(s -> s != null).collect(Collectors.toList());

        int shortest = Integer.MAX_VALUE;
        String shortestPath = "";
        for (String filePath : pathsNoNull) {
            if (filePath.length() < shortest) {
                shortest = filePath.length();
                shortestPath = filePath;
            }
        }
        String result = "";
        if (shortestPath.indexOf("/") != -1) {
            result = shortestPath.substring(0, shortestPath.lastIndexOf("/"));
        }
        // validate if main assumption is correct (i.e. all paths contain the shortest
        // path):
        for (String filePath : pathsNoNull) {
            if (!filePath.contains(result)) {
                throw new ConfigurationException("Study configuration contains mixed locations. Not allowed. E.g. "
                        + "locations: " + filePath + " and " + result + "/...");
            }
        }
        return result;
    }

    /**
     * Copy a directory to a new location preserving the file dates.
     *
     * This method copies the specified directory and all its child directories
     * and files to the specified destination.The destination is the new location
     * and name of the directory.
     *
     * @param sourceDir  Directory to be copied
     * @param targetDir  Name and path to new directory
     * @throws ResourceUtilsException
     */
    public void copyDirectory(Resource sourceDir, Resource targetDir) throws ResourceUtilsException {
        try {
            FileUtils.copyDirectory(sourceDir.getFile(), targetDir.getFile());
        } catch (IOException e) {
            throw new ResourceUtilsException("Cannot copy directory: " + targetDir.getDescription());
        }
    }

    /**
     * Copy a file to a new location.
     *
     * @param destinationDir  Resource pointing to directory where new file should be created
     * @param inputResource  Object
     * @param fileName
     * @return Resource
     * @throws ResourceUtilsException
     */
    public Resource copyResource(Resource destinationDir, InputStreamSource inputResource, String fileName)
        throws ResourceUtilsException {
        try {
            String fullDestinationPath = trimPathRight(getFile(destinationDir).getAbsolutePath()) + "/" + trimPathLeft(fileName);
            ensureDirs(fullDestinationPath.substring(0, fullDestinationPath.lastIndexOf("/")));

            FileSystemResource localFile = getWritableResource(fullDestinationPath);
            IOUtils.copy(inputResource.getInputStream(), localFile.getOutputStream());

            return localFile;
        } catch (IOException e) {
            throw new ResourceUtilsException("Cannot copy resource", e);
        }
    }

    /**
     * Create all subdirectories under a file or directory.
     *
     * Only works on a local file system.
     *
     * @param path  File representing a path.
     * @throws ResourceUtilsException
     */
    public void ensureDirs(File path) throws ResourceUtilsException {
        if (path.isFile()) {
            String pathStr = path.getAbsolutePath();
            ensureDirs(pathStr.substring(0, pathStr.lastIndexOf("/") + 1));
        } else {
            if (!path.exists()) {
                path.mkdirs();
            }
        }
    }

    /**
     * Create all subdirectories under a file or directory.
     *
     * @param path  Resource representing a path.
     * @throws ResourceUtilsException
     */
    public void ensureDirs(Resource path) throws ResourceUtilsException {
        ensureDirs(getFile(path));
    }

    /**
     * Create all subdirectories under a file or directory.
     *
     * @param path  Resource representing a path.
     * @throws ResourceUtilsException
     */
    public void ensureDirs(String path) throws ResourceUtilsException {
		ensureDirs(new File(path));
    }

    /**
     * Resolve the given location pattern into Resource objects.
     *
     * @param path   location pattern to resolve (e.g, file:///basedir/*)
     * @return Resource[]  Resource objects matching the location pattern
     * @throws ResourceUtilsException
     */
    public Resource[] getResources(String path) throws ResourceUtilsException {
		try {
            return resourceResolver.getResources(path);
        } catch (IOException e) {
            throw new ResourceUtilsException("Error retrieving resources.", e);
        }
    }

    /**
     * Create WritableResource object from URL string.
     *
     * @param path  URL string (e.g., file:///file.txt)
     * @return WritableResource
     */
    // XXX writable resources are hard coded to be on the local system
    // if needed (writable remote files) update implementation.
    public FileSystemResource getWritableResource(String path) {
        return new FileSystemResource(path);
	}

    /**
     * Create WritableResource object from Resource object.
     *
     * @param resource  Resource object to be converted in writable form
     * @return WritableResource
     * @throws ResourceUtilsException
     */
    public WritableResource getWritableResource(Resource resource) throws ResourceUtilsException {
        return new FileSystemResource(getFile(resource));
    }

    /**
     * Create a file Resource based on a base directory and strings concatenated with "/".
     *
     * Example: ["file:/basedir", "subdir", "file.txt"] will become Resource["file:/basedir/subdir/file.txt"]
     *
     * @param basePath  Resource representing the base directory
     * @param fileElements  Strings representing subdir and files
     * @return Resource
     * @throws ResourceUtilsException
     */
    public Resource createFileResource(Resource basePath, String ... fileElements)
            throws ResourceUtilsException {
        try {
            String base = trimPathRight(basePath.getURL().toString());
            Resource res = resourceResolver.getResource(base + "/" + Stream.of(fileElements).collect(Collectors.joining("/")));
            File file = res.getFile();
            if (file.exists()) {
                file.delete();
            }
            file.createNewFile();
            return res;
        } catch (IOException e) {
            throw new ResourceUtilsException("Cannot create new file Resource: " + basePath.getDescription());
        }
    }

    /**
     * Create a directory Resource based on a base directory and strings concatenated with "/".
     *
     * Makes sure the path to the new Resource exists on the file system.
     *
     * Example: ["file:/basedir", "subdir1", "dir"] will become Resource["file:/basedir/subdir/dir/"]
     *
     * @param basePath  Resource representing the base directory
     * @param fileElements  Strings representing directories
     * @return Resource
     * @throws ResourceUtilsException
     */
    public Resource createDirResource(Resource basePath, String ... fileElements)
            throws ResourceUtilsException {
        try {
            String base = trimPathRight(basePath.getURL().toString());
            Resource res = resourceResolver.getResource(base + "/" + Stream.of(fileElements).collect(Collectors.joining("/")) + "/");
            ensureDirs(res);
            return res;
        } catch (IOException e) {
            throw new ResourceUtilsException("Cannot create new directory Resource: " + basePath.getDescription());
        }
    }

    /**
     * Create a directory structure on the file system to Resource object.
     *
     * @param dir  Resource representing the directory
     * @return Resource
     * @throws ResourceUtilsException
     */
    public Resource createDirResource(Resource dir) throws ResourceUtilsException {
        return createDirResource(dir, "");
    }

    /**
     * Delete a file or directory on the file system.
     *
     * @param res  Resource to be deleted
     * @throws ResourceUtilsException
     */
    public void deleteResource(Resource res) throws ResourceUtilsException {
        if (res.exists()) {
            try {
                res.getFile().delete();
            } catch (IOException e) {
                throw new ResourceUtilsException("Error deleting resource.");
            }
        }
    }

    /**
     * Check whether Resource represents a file (not a directory).
     *
     * @param resource
     * @return boolean
     * @throws ResourceUtilsException
     */
    public boolean isFile(Resource resource) throws ResourceUtilsException {
        try {
            return resource.isFile() && resource.getFile().isFile();
        } catch (IOException e) {
            throw new ResourceUtilsException("Cannot get File from Resource object: " + resource.getDescription());
        }
    }

    /**
     * Get URL from Resource object.
     *
     * @param resource
     * @return URL
     * @throws ResourceUtilsException
     */
    public URL getURL(Resource resource) throws ResourceUtilsException {
        try {
            return resource.getURL();
        } catch (IOException e) {
            throw new ResourceUtilsException("Cannot read URL from Resource: " + resource.getDescription());
        }
    }

    /**
     * Get File from Resource object.
     * @param resource
     * @return File
     * @throws ResourceUtilsException
     */
    public File getFile(Resource resource) throws ResourceUtilsException {
        try {
                return resource.getFile();
        } catch (IOException e) {
            throw new ResourceUtilsException("Cannot read File from Resource: " + resource.getDescription());
        }
    }

    /**
     * Write lines to a Resource on the local file system.
     *
     * @param file  File on the local filesystem where lines will be written to
     * @param lines  Strings that will be written as lines to the file
     * @param append  When 'true' lines will be appended to the file
     * @throws ResourceUtilsException
     */
    public void writeToFile(WritableResource file, Collection<String> lines, boolean append) throws ResourceUtilsException {
        String line = lines.stream().collect(Collectors.joining("\n")) + "\n";
        writeToFile(file, line, append);
    }

    /**
     * Write a String to a Resource on the local file system.
     *
     * @param file  File on the local filesystem where lines will be written to
     * @param content  String that will be written to the file
     * @param append  When 'true' the conent will be appended to the file
     * @throws ResourceUtilsException
     */
    public void writeToFile(WritableResource file, String content, boolean append) throws ResourceUtilsException {
        try {
            FileWriter writer = new FileWriter(file.getFile(), append);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            throw new ResourceUtilsException("Error while writing to file.", e);
        }
    }

    /**
     * Convert protocol, hot and file path URL.
     *
     * Example: ftp, host, '/file.txt' becomes URL['ftp:///host/file.txt']
     * Host name is resolved from the ftp.host application property.
     *
     * @param protocol
     * @param host
     * @param remoteFilePath  path to item from perspective of FTP server
     * @return URL
     * @throws ResourceUtilsException
     */
    public URL createRemoteURL(String protocol, String host, String remoteFilePath) throws ResourceUtilsException {
        StringBuilder bffr = new StringBuilder();
        bffr.append(protocol)
            .append(":///")
            .append(host)
            .append(remoteFilePath);
        try {
            return new URL(bffr.toString());
        } catch (MalformedURLException e) {
            throw new ResourceUtilsException(e);
        }
    }

    /**
     * Build URL for remote item from SftpFileInfo object.
     *
     * Example: URL['ftp:///host/root_dir/file.txt'].
     * Host name is resolved from the ftp.host application property.
     *
     * @param protocol
     * @param host
     * @param fileInfo file info object
     * @return URL
     * @throws ResourceUtilsException
     */
    public URL createRemoteURL(String protocol, String host, SftpFileInfo fileInfo) throws ResourceUtilsException {
        StringBuilder bffr = new StringBuilder();
        bffr.append(fileInfo.getRemoteDirectory()).append(fileInfo.getFilename());
        return createRemoteURL(protocol, host, bffr.toString());
    }

    /**
     * Strip protocol and host name from URL.
     *
     * The resulting path can be used by the server to indicate
     * resources for upload/download.
     *
     * Example: host, URL['ftp:/host/file.txt'] becomes '/file.txt'.
     *
     * @param host
     * @param path URL of item on server
     * @return String  path to item from perspective of server
     * @throws ResourceUtilsException
     */
    public String remotePath(String host, URL path) throws ResourceUtilsException {
        if (host == null)
            host = "";
        String pathStr = path.toString();

        pathStr = trimPathLeft(stripResourceTypePrefix(pathStr)).replaceFirst(host, "");

        return "/" + trimPathLeft(pathStr);
    }

    public Properties parsePropertiesFile(String propertiesPath) throws ResourceUtilsException {

        try (InputStream input = new FileInputStream(propertiesPath)) {

            Properties prop = new Properties();

            // load a properties file
            prop.load(input);

            return prop;

        } catch (IOException ex) {
            throw new ResourceUtilsException("Properties file could not be parsed.", ex);
        }
    }

}