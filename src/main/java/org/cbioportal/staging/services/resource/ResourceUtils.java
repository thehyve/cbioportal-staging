package org.cbioportal.staging.services.resource;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * ResourceUtils
 */
@Component
public class ResourceUtils {

    public String trimDir(String dir) {
        return dir.replaceFirst("\\/*\\**$", "");
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

}