package org.cbioportal.staging.services.resource;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 * ResourceFilterImpl
 *
 * Filters Resources to include only contents of folders specified in
 * 'scan.extract.folders' property, and to exclude all files specified in the
 * file references by the 'scan.ignore.file' property.
 */
@Component
public class DefaultResourceFilter implements IResourceFilter {

    @Value("${scan.extract.folders:}")
    private String scanExtractFolders;

    @Autowired
    private ResourceIgnoreSet resourceIgnoreSet;

    @Autowired
    private ResourceUtils utils;

    private List<String> includedDirs;

    @PostConstruct
    public void init() {
        scanExtractFolders = scanExtractFolders.replaceAll("\\s+", "");
        includedDirs = Stream.of(scanExtractFolders.split(","))
            .map(e -> utils.trimDir(e))
            .map(e -> utils.trimFile(e))
            .collect(Collectors.toList());
    }

    @Override
    public Map<String,Resource[]> filterResources(Map<String,Resource[]> resources) throws ResourceCollectionException {
        Map<String,Resource[]> out = new HashMap<String,Resource[]>();

        if (resources == null)
            return out;

        for (Entry<String,Resource[]> entry: resources.entrySet()) {
            Resource[] res = filter(entry.getValue());
            if (res.length > 0)
                out.put(entry.getKey(), filter(entry.getValue()));
        }

        return out;
    }

    private Resource[] filter(Resource[] resources) {
        Resource[] r = Stream.of(resources).filter(
            resource -> {
                try {

                    String path = utils.trimFile(utils.getURL(resource).toString());
                    String pathTypeRemoved = utils.trimFile(utils.stripResourceTypePrefix(path));

                    boolean inIgnoreFile = ! resourceIgnoreSet.isEmpty()
                                            && (resourceIgnoreSet.contains(path)
                                                || resourceIgnoreSet.contains(pathTypeRemoved));
                    boolean inIncludeDir = includedDirs.stream().anyMatch(dir -> path.startsWith(dir) || pathTypeRemoved.startsWith(dir) );

                    return inIncludeDir && ! inIgnoreFile;
                } catch (ResourceUtilsException e) {
                    throw new RuntimeException("Cannot get URL from resource.", e);
                }

            }).toArray(Resource[]::new);

        return r;
    }

}