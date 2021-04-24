package org.cbioportal.staging.services.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 *
 * Recieves a directory and recursively extracts a list of
 * resources for a single study. The files/resources are returned
 * keyed by the study id. The study identifier is set to the name
 * of the study folder.
 *
 */
@ConditionalOnProperty(value="scan.studyfiles.strategy", havingValue = "versiondefined")
@Component
@Primary
public class FolderStudyVersionDefinedResourceStrategy implements IStudyResourceStrategy {

    @Autowired
    private ResourceUtils utils;

    @Autowired
    private IResourceProvider resourceProvider;

    @Value("${scan.location}")
    private Resource scanLocation;

    @Override
    public Study[] resolveResources(Resource[] resources) throws ResourceCollectionException {

        // perform a recursive scan of the study folder
        // the resources argument does not files inside dirs
        Resource[] studyResources = resourceProvider.list(scanLocation, true, true);

        List<Study> out = new ArrayList<>();
        String timestamp = utils.getTimeStamp("yyyyMMdd-HHmmss");
        try {
            String studyVersion = getStudyVersion(scanLocation);
            String studyId = getStudyId(scanLocation);

            out.add(new Study(studyId, studyVersion, timestamp, scanLocation, studyResources));

        } catch (IOException e) {
            throw new ResourceCollectionException("Cannot read from study directory:" + scanLocation, e);
        }

        return out.toArray(new Study[0]);
    }

    private String getStudyVersion(Resource dir) throws IOException {
        String uri = utils.trimPathRight(utils.getURI(dir).toString());
        return uri.substring(uri.lastIndexOf("/") + 1);
    }

    private String getStudyId(Resource dir) throws IOException {
        String url = utils.trimPathRight(utils.getURI(dir).toString());
        String[] parts = url.split("/");
        return parts[parts.length -2];
    }

}