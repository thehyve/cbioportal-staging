package org.cbioportal.staging.services.resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

@Component
public class ResourceUtils {

    @Autowired
    private ResourcePatternResolver resourceResolver;

    public String trimDir(String dir) {
        return dir.replaceFirst("\\/*\\**$", "");
    }

    public String trimFile(String dir) {
        return dir.replaceFirst("^\\/*", "");
    }

    public String stripResourceTypePrefix(String dir) {
        return dir.replaceFirst("^.*:", "");
    }

    public String getTimeStamp(String pattern) {
        return new SimpleDateFormat(pattern).format(new Date());
    }

    public Resource[] filterFiles(Resource[] resources, String prefixPattern, String extensionPattern) {
        String filePattern = ".*" + prefixPattern + ".*" + extensionPattern + "$";
        return Stream.of(resources).filter(n -> n.getFilename().matches(filePattern)).toArray(Resource[]::new);
    }

    public Resource getMostRecent(Resource[] resources) {
        // Stream.of(resources).max(ThrowingBiFunction.unchecked((a,b) ->
        // a.lastModified() - b.lastModified()));
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

    public Resource[] extractDirs(Resource[] resources) {
        // Stream.of(resources).filter(ThrowingFunction.unchecked(n ->
        // n.getFile().isDirectory())).toArray(Resource[]::new);
        return Stream.of(resources).filter(n -> {
            try {
                return n.getFile().isDirectory();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return false;
        }).toArray(Resource[]::new);
    }

    public Map<String, String> readMetaFile(Resource studyMetaFile) throws ResourceUtilsException {
        BufferedReader bufferedReader = null;
        Map<String, String> entries = new HashMap<>();
        try {
            bufferedReader = new BufferedReader(new FileReader(studyMetaFile.getFile()));
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
     * This method gets the "base path" of all entries. I.e. it assumes all entries
     * share a common parent path on the S3 or other resource folder where they are
     * originally shared. So for the following list of files configured in the list
     * of studies yaml as below: study1: - folder/study1path/fileA.txt -
     * folder/study1path/fileB.txt - folder/study1path/mafs/maf1.maf -
     * folder/study1path/mafs/mafn.maf
     *
     * this method will return "folder/study1path".
     *
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

    public void copyDirectory(Resource sourceDir, Resource targetDir) throws ResourceUtilsException {
        try {
            FileUtils.copyDirectory(sourceDir.getFile(), targetDir.getFile());
        } catch (IOException e) {
            throw new ResourceUtilsException("Cannot copy directory: " + targetDir.getDescription());
        }
    }

    public Resource copyResource(Resource destination, InputStreamSource resource, String remoteFilePath)
            throws ResourceUtilsException {
        try {
            String fullDestinationPath = trimDir(getFile(destination).getAbsolutePath()) + "/" + trimFile(remoteFilePath);
            ensureDirs(fullDestinationPath.substring(0, fullDestinationPath.lastIndexOf("/")));

            WritableResource localFile = getWritableResource(fullDestinationPath);
            IOUtils.copy(resource.getInputStream(), localFile.getOutputStream());

            return localFile;
        } catch (IOException e) {
            throw new ResourceUtilsException("Cannot copy resource", e);
        }
    }

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

    public void ensureDirs(Resource path) throws ResourceUtilsException {
        ensureDirs(getFile(path));
    }

    public void ensureDirs(String path) throws ResourceUtilsException {
		ensureDirs(new File(path));
    }

	public Resource getResource(String path) {
		return resourceResolver.getResource(path);
    }

    public Resource[] getResources(String path) throws ResourceUtilsException {
		try {
            return resourceResolver.getResources(path);
        } catch (IOException e) {
            throw new ResourceUtilsException("Error retrieving resources.", e);
        }
    }

    // TODO writable resources are hard coded to be on the local system
    // if needed (writable remote files) update implementation.
    public WritableResource getWritableResource(String path) {
        return new FileSystemResource(path);
	}

    public WritableResource getWritableResource(Resource resource) throws ResourceUtilsException {
        return new FileSystemResource(getFile(resource));
    }

	public Resource createFileResource(Resource basePath, String ... fileElements)
            throws ResourceUtilsException {
        try {
            String base = trimDir(basePath.getURL().toString());
            Resource res = getResource(base + "/" + Stream.of(fileElements).collect(Collectors.joining("/")));
            res.getFile().createNewFile();
            return res;
        } catch (IOException e) {
            throw new ResourceUtilsException("Cannot create new file Resource: " + basePath.getDescription());
        }
    }

	public Resource createDirResource(Resource basePath, String ... fileElements)
            throws ResourceUtilsException {
        try {
            String base = trimDir(basePath.getURL().toString());
            Resource res = getResource(base + "/" + Stream.of(fileElements).collect(Collectors.joining("/")) + "/");
            ensureDirs(res);
            return res;
        } catch (IOException e) {
            throw new ResourceUtilsException("Cannot create new directory Resource: " + basePath.getDescription());
        }
    }

	public Resource createDirResource(Resource dir) throws ResourceUtilsException {
        return createDirResource(dir, "");
    }

    public void deleteResource(Resource res) throws ResourceUtilsException {
        if (res.exists()) {
            try {
                res.getFile().delete();
            } catch (IOException e) {
                throw new ResourceUtilsException("Error deleting resource.");
            }
        }
    }

    public boolean isFile(Resource resource) throws ResourceUtilsException {
        try {
            return resource.getFile().isFile();
        } catch (IOException e) {
            throw new ResourceUtilsException("Cannot het File from Resource object: " + resource.getDescription());
        }
    }

    public URL getURL(Resource resource) throws ResourceUtilsException {
        try {
            return resource.getURL();
        } catch (IOException e) {
            throw new ResourceUtilsException("Cannot read URL from Resource: " + resource.getDescription());
        }
    }

    public File getFile(Resource resource) throws ResourceUtilsException {
        try {
            return resource.getFile();
        } catch (IOException e) {
            throw new ResourceUtilsException("Cannot read File from Resource: " + resource.getDescription());
        }
    }

    public void writeToFile(WritableResource file, Collection<String> lines, boolean append) throws ResourceUtilsException {
        String line = lines.stream().collect(Collectors.joining("\n")) + "\n";
        writeToFile(file, line, append);
    }

    public void writeToFile(WritableResource file, String content, boolean append) throws ResourceUtilsException {
        try {
            FileWriter writer = new FileWriter(file.getFile(), append);
            writer.write(content);
            writer.close();
        } catch (IOException e) {
            throw new ResourceUtilsException("Error while writing to file.", e);
        }
    }

}