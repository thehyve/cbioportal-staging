package org.cbioportal.staging.services.resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.cbioportal.staging.exceptions.ConfigurationException;
import org.springframework.beans.factory.annotation.Autowired;
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

    public Resource copyResource(String destination, Resource resource, String remoteFilePath) throws IOException {
		InputStream inputStream = resource.getInputStream();
		String fullDestinationPath = destination + remoteFilePath;
		ensureDirs(fullDestinationPath.substring(0, fullDestinationPath.lastIndexOf("/")));
		Files.copy(inputStream, Paths.get(fullDestinationPath));
		inputStream.close();
		return resourcePatternResolver.getResource(fullDestinationPath);
	}

    public void ensureDirs(File path) {
		if (!path.exists()) {
			path.mkdirs();
		}
    }

    public void ensureDirs(String path) throws IOException {
		ensureDirs(new File(path));
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
	public String getBasePath(List<String> paths) {

        List<String> pathsNoNull = paths.stream().filter(s -> s != null).collect(Collectors.toList());

		Optional<String> shortestOpt = pathsNoNull.stream().min(Comparator.comparingInt(String::length));
		if (!shortestOpt.isPresent())
			return null;
        String shortest = shortestOpt.get();

		OptionalInt firstPositionMismatch = IntStream.range(0, shortest.length())
			.filter(i -> ! pathsNoNull.stream().allMatch(p -> p.charAt(i) == shortest.charAt(i)))
			.findFirst();

		if (firstPositionMismatch.isPresent()) {
			return shortest.substring(0, firstPositionMismatch.getAsInt());
		}

		return shortest;
    }
    
    public File createLogFile(String studyId, File studyPath, String logPrefix) {
        String logName = studyId + "_" + logPrefix;
        File logFile = new File(studyPath + "/" + logName);
        return logFile;
    }

}