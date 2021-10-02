package org.cbioportal.staging.services.resource;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.cbioportal.staging.exceptions.DirectoryCreatorException;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.directory.IDirectoryCreator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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
@Primary
@Component
@ConditionalOnProperty(value="scan.studyfiles.strategy", havingValue = "versiondir")
public class FolderStudyVersionResourceStrategy implements IStudyResourceStrategy {

    private static final Logger logger = LoggerFactory.getLogger(FolderStudyVersionResourceStrategy.class);

    @Autowired
    private IResourceProvider resourceProvider;

    @Autowired
    private ResourceUtils utils;

    @Autowired
    private IDirectoryCreator directoryCreator;

    @PostConstruct
    public void init() {
        logger.debug("Activated FolderStudyVersionResourceResolver from spring profile.");
    }

    @Override
    public Study[] resolveResources(Resource[] resources) throws ResourceCollectionException {

        List<Study> out = new ArrayList<>();
        String studyPath = "";
        String studyVersionPath = "";
        String timestamp = utils.getTimeStamp("yyyyMMdd-HHmmss");
        try {

            logger.info("Looking for study directories...");

            Resource[] studyDirs = utils.extractDirs(resources);

            logger.info("Found study directories: " + Stream.of(studyDirs).map(e -> e.getFilename()).collect(Collectors.joining(", ")) );

            for (Resource studyDir : studyDirs) {

                studyPath = studyDir.getFilename();

                Resource studyVersionDir = getMostRecentStudyVersion(studyDir);

                if (studyVersionDir != null) {

                    studyVersionPath = studyVersionDir.getFilename();
                    String studyVersion = getStudyVersion(studyVersionDir);

                    logger.info("Most recent version for " + studyPath + ": " + studyVersionPath);
                    logger.info("For study " + studyPath + "use version: " + studyVersion);

                    Resource[] studyResources = resourceProvider.list(studyVersionDir, true, true);

                    String studyId = getStudyId(studyResources, studyPath);

                    Study study = new Study(studyId, studyVersion, timestamp, studyVersionDir,
                        studyResources);
                    study.setStudyDir(directoryCreator.getStudyExtractDir(study));
                    out.add(study);
                }

            }

        } catch (IOException e) {
            throw new ResourceCollectionException("Cannot read from study dir.", e);
        } catch (DirectoryCreatorException e) {
            throw new ResourceCollectionException("Cannot evaluate extraction directory.", e);
        } catch (ResourceUtilsException e) {
            throw new ResourceCollectionException("Cannot read study version.", e);
        }

        return out.toArray(new Study[0]);
    }

    private String getStudyId(Resource[] resources, String studyFolder) throws ResourceUtilsException {
        // find study meta file and if found get the studyId from the meta file (transformed studies)
        Optional<Resource> studyMetaFile = Stream.of(resources).filter(e -> e.getFilename().matches(".*meta_study.txt$")).findAny();
        if (studyMetaFile.isPresent()) {
            return utils.readMetaFile(studyMetaFile.get()).get("cancer_study_identifier");
        }
        // if not meta file found use the study folder name as studyId (folder previous to the "version" folder)
        studyFolder = utils.trimPathRight(studyFolder);
        return studyFolder.substring(studyFolder.lastIndexOf("/") + 1);
    }

    private Resource getMostRecentStudyVersion(Resource studyDir) throws ResourceCollectionException {
        try {
            Resource[] studyResources = resourceProvider.list(studyDir);
            Resource[] versions = new Resource[0];
            versions = utils.extractDirs(studyResources);
            if (versions.length == 0) {
                return null;
            }
            return utils.getMostRecent(versions);
        } catch (ResourceUtilsException e) {
            throw new ResourceCollectionException("Could not process resources.", e);
        }
    }

    private String getStudyVersion(Resource dir) throws IOException {
        String url = utils.trimPathRight(utils.getURI(dir).toString());
        return url.substring(url.lastIndexOf("/") + 1);
    }

}