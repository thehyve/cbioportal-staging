package org.cbioportal.staging.services.resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.io.FileUtils;
import org.cbioportal.staging.exceptions.ConfigurationException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.InputStreamSource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * ResourceUtils
 */
@Component
public class ResourceUtils {

	@Autowired
	private ResourcePatternResolver resourcePatternResolver;


    public String trimDir(String dir) {
        return dir.replaceFirst("\\/*\\**$", "");
    }

    public String stripResourceTypePrefix(String dir) {
        return dir.replaceFirst("^.*:", "");
    }

    public String getTimeStamp(String pattern) {
        return new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
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

    public Map<String, String> readMetaFile(Resource studyMetaFile) throws FileNotFoundException, IOException {
        Map<String,String> entries = new HashMap<>();
        BufferedReader bufferedReader = new BufferedReader(new FileReader(studyMetaFile.getFile()));
        try {
            String line = bufferedReader.readLine();
            while (line != null) {
                String[] elements = line.split("\\s*:\\s*");
                entries.put(elements[0].trim(), elements[1].trim());
                line = bufferedReader.readLine();
            }
        } finally {
            bufferedReader.close();
        }
        return entries;
    }

    /**
	 * This method gets the "base path" of all entries. I.e. it assumes
	 * all entries share a common parent path on the S3 or other resource folder
	 * where they are originally shared. So for the following list of files configured in the
	 * list of studies yaml as below:
	 *   study1:
     *    - folder/study1path/fileA.txt
     *    - folder/study1path/fileB.txt
     *    - folder/study1path/mafs/maf1.maf
     *    - folder/study1path/mafs/mafn.maf
     *
     * this method will return "folder/study1path".
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
		//validate if main assumption is correct (i.e. all paths contain the shortest path):
		for (String filePath : pathsNoNull) {
			if (!filePath.contains(result)) {
				throw new ConfigurationException("Study configuration contains mixed locations. Not allowed. E.g. "
						+ "locations: "+ filePath + " and " + result + "/...");
			}
		}
		return result;
    }

    public void copyDirectory(Resource sourceDir, Resource targetDir) throws ResourceCollectionException {
        try {
            FileUtils.copyDirectory(sourceDir.getFile(), targetDir.getFile());
        } catch (IOException e) {
            throw new ResourceCollectionException("Cannot copy directory: " + targetDir.getDescription());
        }
    }


	public Resource copyResource(String destination, InputStreamSource resource, String remoteFilePath)
            throws ResourceCollectionException {
		try {
        InputStream inputStream = resource.getInputStream();
		String fullDestinationPath = destination + remoteFilePath;
            ensureDirs(fullDestinationPath.substring(0, fullDestinationPath.lastIndexOf("/")));
            Files.copy(inputStream, Paths.get(fullDestinationPath));
            inputStream.close();
            return resourcePatternResolver.getResource(fullDestinationPath);
        } catch (IOException e) {
            throw new ResourceCollectionException("Cannot copy resource", e);
        }
    }

	public Resource copyResource(Resource destination, InputStreamSource resource, String remoteFilePath)
            throws ResourceCollectionException {
        return copyResource(getURL(destination).toString(), resource, remoteFilePath);
    }


    public void ensureDirs(File path) {
		if (!path.exists()) {
			path.mkdirs();
		}
    }

    public void ensureDirs(Resource path) throws ResourceCollectionException {
        if (getFile(path).isFile()) {
            throw new ResourceCollectionException(
                    "Resource is a file (should be a directory): " + getFile(path).getAbsolutePath());
        }
        if (!path.exists()) {
            getFile(path).mkdirs();
        }
    }

    public void ensureDirs(String path) throws IOException {
		ensureDirs(new File(path));
    }

    public Resource createLogFile(String studyId, Resource studyPath, String logPrefix)
            throws ResourceCollectionException {
        String logName = studyId + "_" + logPrefix;
        try {
            Resource logFile = studyPath.createRelative(studyPath + "/" + logName);
            // File logFile = new File(studyPath + "/" + logName);
            return logFile;
        } catch (IOException e) {
            throw new ResourceCollectionException("Cannot create relative path: " + studyPath.getDescription());
        }
    }

	public Resource getResource(String path) {
		return resourcePatternResolver.getResource(path);
	}

	public Resource getResource(Resource basePath, String ... fileElements)
            throws ResourceCollectionException {
        try {
            return basePath.createRelative(Stream.of(fileElements).collect(Collectors.joining("/")));
        } catch (IOException e) {
            throw new ResourceCollectionException("Cannot create relative path: " + basePath.getDescription());
        }
    }

    public boolean isFile(Resource resource) throws ResourceCollectionException {
        try {
            return resource.getFile().isFile();
        } catch (IOException e) {
            throw new ResourceCollectionException("Cannot het File from Resource object: " + resource.getDescription());
        }
    }

    public URL getURL(Resource resource) throws ResourceCollectionException {
        try {
            return resource.getURL();
        } catch (IOException e) {
            throw new ResourceCollectionException("Cannot read URL from Resource: " + resource.getDescription());
        }
    }

    public File getFile(Resource resource) throws ResourceCollectionException {
        try {
            return resource.getFile();
        } catch (IOException e) {
            throw new ResourceCollectionException("Cannot read File from Resource: " + resource.getDescription());
        }
    }

}