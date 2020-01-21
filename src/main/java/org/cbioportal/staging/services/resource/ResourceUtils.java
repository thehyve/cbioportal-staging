package org.cbioportal.staging.services.resource;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.stream.Stream;

import org.springframework.core.io.Resource;

/**
 * ResourceUtils
 */
public class ResourceUtils {

    public static String trimDir(String dir) {
        return dir.replaceFirst("\\/*\\**$", "");
    }

    public static String getTimeStamp(String pattern) {
        return new SimpleDateFormat("yyyyMMdd-HHmmss").format(new Date());
    }

    public static Resource[] filterFiles(Resource[] resources, String prefixPattern, String extensionPattern) {
        String filePattern = ".*" + prefixPattern + ".*" + extensionPattern + "$";
        return Stream.of(resources).filter(n -> n.getFilename().matches(filePattern)).toArray(Resource[]::new);
    }

    public static Resource getMostRecent(Resource[] resources) {
        // Stream.of(resources).max(ThrowingBiFunction.unchecked((a,b) -> a.lastModified() - b.lastModified()));
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

}