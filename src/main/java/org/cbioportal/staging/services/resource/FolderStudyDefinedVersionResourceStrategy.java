package org.cbioportal.staging.services.resource;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.PostConstruct;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

/**
 *
 * Recieves a list of directories and recursively extracts a list of
 * resources for each study. The files/resources are returned keyed
 * by study id. When available the study indentifier is extracted from
 * the meta_study.txt file. If not, study identifier is set to the name
 * of the study folder.
 *
 */
@ConditionalOnProperty(value="scan.studyfiles.strategy", havingValue = "definedversion")
@Component
@Primary
public class FolderStudyDefinedVersionResourceStrategy implements IStudyResourceStrategy {

    private static final Logger logger = LoggerFactory.getLogger(FolderStudyVersionResourceStrategy.class);

    @Autowired
    private ResourceUtils utils;

    @Value("${scan.location}")
    private Resource scanLocation;
    
    @Value("${scan.extract.folders}")
    private String studyId;

    @PostConstruct
    public void init() {
        logger.debug("Activated FolderStudyDefinedVersionResourceResolver from spring profile.");
    }

    @Override
    public Study[] resolveResources(Resource[] resources) throws ResourceCollectionException {

        List<Study> out = new ArrayList<>();
        String timestamp = utils.getTimeStamp("yyyyMMdd-HHmmss");
        if (studyId == null) {
            throw new ResourceCollectionException("The scan.extract.folders is not defined.");
        } else if (studyId.indexOf(",") != -1) {
            throw new ResourceCollectionException("The scan.extract.folders contains more than one value: "+studyId);
        }
        try {
            String studyVersion = getStudyVersion(scanLocation);

            out.add(new Study(studyId, studyVersion, timestamp, scanLocation, resources));

        } catch (ResourceUtilsException e) {
            throw new ResourceCollectionException("Cannot read from study directory:" + scanLocation, e);
        }

        return out.toArray(new Study[0]);
    }

    private String getStudyVersion(Resource dir) throws ResourceUtilsException {
        String url = utils.trimPathRight(utils.getURL(dir).toString());
        return url.substring(url.lastIndexOf("/") + 1);
    }

}