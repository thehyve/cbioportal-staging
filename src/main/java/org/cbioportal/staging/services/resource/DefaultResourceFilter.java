package org.cbioportal.staging.services.resource;

import java.util.ArrayList;
import java.util.List;
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

    @Value("${scan.location}")
    private Resource scanLocation;

    @Autowired
    private ResourceIgnoreSet resourceIgnoreSet;

    @Autowired
    private ResourceUtils utils;

    private List<String> includedDirs;

    @PostConstruct
    public void init() {
        scanExtractFolders = scanExtractFolders.replaceAll("\\s+", "");
        includedDirs = Stream.of(scanExtractFolders.split(","))
            .map(e -> utils.trimPathRight(e))
            .map(e -> utils.trimPathLeft(e))
            .collect(Collectors.toList());
    }

    @Override
    public Study[] filterResources(Study[] studies) throws ResourceCollectionException {

        List<Study> out = new ArrayList<>();

        if (studies == null)
            return out.toArray(new Study[0]);

        for (Study study: studies) {
            Resource[] res = filter(study.getResources());
            if (res.length > 0)
                out.add(new Study(study.getStudyId(), study.getVersion(), study.getTimestamp(), study.getStudyDir(), res));
        }

        return out.toArray(new Study[0]);
    }

    private Resource[] filter(Resource[] resources) {
        Resource[] r = Stream.of(resources).filter(
            resource -> {
                try {

                    String path = utils.getURL(resource).toString();
                    String pathMinusScanLocation = utils.trimPathLeft(path.replace(utils.getURL(scanLocation).toString(), ""));

                    boolean inIgnoreFile = ! resourceIgnoreSet.isEmpty()
                                            && (resourceIgnoreSet.contains(path)
                                                || resourceIgnoreSet.contains(pathMinusScanLocation));
                    boolean inIncludeDir = includedDirs.stream().anyMatch(dir -> path.startsWith(dir) || pathMinusScanLocation.startsWith(dir) );

                    return inIncludeDir && ! inIgnoreFile;
                } catch (ResourceUtilsException e) {
                    throw new RuntimeException("Cannot get URL from resource.", e);
                }

            }).toArray(Resource[]::new);

        return r;
    }

}