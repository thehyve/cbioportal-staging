package org.cbioportal.staging.services.resource;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Stream;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * ResourceFilterImpl
 *
 * Filters Resources to include only contents of folders specified in
 * 'scan.extract.folders' property, and to exclude all files specified
 * in the file references by the 'scan.ignore.file' property.
 */
@Component
public class DefaultResourceFilter implements IResourceFilter {

    @Value("${scan.extract.folders:}")
    private String scanExtractFolders;

    @Autowired
    private ResourceIgnoreSet resourceIgnoreSet;

    @Override
    public Map<String,Resource[]> filterResources(Map<String,Resource[]> resources) throws ResourceCollectionException {
        Map<String,Resource[]> out = new HashMap<String,Resource[]>();
        for (Entry<String,Resource[]> entry: resources.entrySet()) {
            out.put(entry.getKey(), filter(entry.getValue()));
        }
        return out;
    }

    private Resource[] filter(Resource[] resources) {
        Resource[] r = Stream.of(resources).filter(
            resource -> {
                String[] includeDirs = scanExtractFolders.split(",");
                try {

                    String path = resource.getURL().toString();
                    String pathTypeRemoved = path.replaceFirst("^.*\\:", "");

                    boolean inIgnoreFile = ! resourceIgnoreSet.isEmpty()
                                            && (resourceIgnoreSet.contains(path)
                                                || resourceIgnoreSet.contains(pathTypeRemoved));
                    boolean inIncludeDir = Stream.of(includeDirs).anyMatch(dir -> path.startsWith(dir) || pathTypeRemoved.startsWith(dir) );

                    return inIncludeDir && ! inIgnoreFile;
                } catch (IOException e) {
                    throw new RuntimeException("Cannot get URL from resource.", e);
                }

            }).toArray(Resource[]::new);

        return r;
    }

}