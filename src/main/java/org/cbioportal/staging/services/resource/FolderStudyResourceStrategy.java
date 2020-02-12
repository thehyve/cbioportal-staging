package org.cbioportal.staging.services.resource;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
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
@Profile("scan.studydir")
@Component
@Primary
public class FolderStudyResourceStrategy implements IStudyResourceStrategy {

    private static final Logger logger = LoggerFactory.getLogger(FolderStudyResourceStrategy.class);

    @Autowired
    private IResourceProvider resourceProvider;

    @Autowired
    private ResourceUtils utils;

    @PostConstruct
    public void init() {
        logger.debug("Activated FolderStudyResourceResolver from spring profile.");
    }

    @Override
    public Map<String,Resource[]> resolveResources(Resource[] resources) throws ResourceCollectionException {

        Map<String,Resource[]> out = new HashMap<>();
        String studyPath = "";
        try {

            logger.info("Looking for study directories...");

            Resource[] studyDirs = utils.extractDirs(resources);

            logger.info("Found study directories: " + Stream.of(studyDirs).map(e -> e.getFilename()).collect(Collectors.joining(", ")) );

            for (Resource studyDir : studyDirs) {

                Resource[] studyResources = resourceProvider.list(studyDir, true);

                String studyId = getStudyId(studyResources, studyDir.getFilename());

                out.put(studyId, studyResources);
            }

        } catch (ResourceUtilsException e) {
            throw new ResourceCollectionException("Cannot read from study directory:" + studyPath);
        }

        return out;
    }

    private String getStudyId(Resource[] resources, String studyPath) throws ResourceUtilsException {
        // find study meta file and if found get the studyId from the meta file
        Optional<Resource> studyMetaFile = Stream.of(resources).filter(e -> e.getFilename().matches(".*meta_study.txt$")).findAny();
        if (studyMetaFile.isPresent()) {
            return utils.readMetaFile(studyMetaFile.get()).get("cancer_study_identifier");
        }
        // if not meta file found use the study folder name as studyId
        studyPath = utils.trimDir(studyPath);
        return studyPath.substring(studyPath.lastIndexOf("/") + 1);
    }

}