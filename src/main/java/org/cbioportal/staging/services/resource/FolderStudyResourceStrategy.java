package org.cbioportal.staging.services.resource;

import com.pivovarit.function.ThrowingFunction;
import java.io.IOException;
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

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Recieves a list of directories and recursively extracts a list of
 * resources for each study. The files/resources are returned keyed
 * by study id. When available the study indentifier is extracted from
 * the meta_study.txt file. If not, study identifier is set to the name
 * of the study folder.
 */
@Primary
@Component
@ConditionalOnProperty(value="scan.studyfiles.strategy", havingValue = "studydir")
public class FolderStudyResourceStrategy implements IStudyResourceStrategy {

    private static final Logger logger = LoggerFactory.getLogger(FolderStudyResourceStrategy.class);

    @Autowired
    private IResourceProvider resourceProvider;

    @Autowired
    private ResourceUtils utils;

    @Autowired
    private IDirectoryCreator directoryCreator;

    @PostConstruct
    public void init() {
        logger.debug("Activated FolderStudyResourceResolver from spring profile.");
    }

    @Override
    public Study[] resolveResources(Resource[] resources) throws ResourceCollectionException {

        List<Study> out = new ArrayList<>();

        String studyPath = "";
        String timestamp = utils.getTimeStamp("yyyyMMdd-HHmmss");
        try {
            List<String> paths = Stream.of(resources)
                .map(ThrowingFunction.unchecked(e -> utils.getURI(e).toString()))
                .collect(Collectors.toList());

            Resource[] studyDirs = utils.extractDirs(resources);

            logger.info("Found study directories: " +
                Stream.of(studyDirs).map(e -> e.getFilename()).collect(Collectors.joining(", ")));

            for (Resource studyDir : studyDirs) {

                Resource[] studyResources = resourceProvider.list(studyDir, true, true);

                String studyId = getStudyId(studyResources, studyDir.getFilename());

                Study study = new Study(studyId, null, timestamp, studyDir, studyResources);
                out.add(study);
                study.setStudyDir(directoryCreator.getStudyExtractDir(study));
                out.add(study);
            }

        } catch (ResourceUtilsException e) {
            throw new ResourceCollectionException("Cannot read from study dir:" + studyPath,
                e);
        } catch (DirectoryCreatorException e) {
            throw new ResourceCollectionException("Cannot evaluate extraction directory.", e);
        } catch (IOException e) {
            throw new ResourceCollectionException("Cannot read from study dir.", e);
        }

        return out.toArray(new Study[0]);
    }

    private String getStudyId(Resource[] resources, String studyPath)
        throws ResourceUtilsException {
        // find study meta file and if found get the studyId from the meta file
        Optional<Resource> studyMetaFile =
            Stream.of(resources).filter(e -> e.getFilename().matches(".*meta_study.txt$"))
                .findAny();
        if (studyMetaFile.isPresent()) {
            return utils.readMetaFile(studyMetaFile.get()).get("cancer_study_identifier");
        }
        // if not meta file found use the study folder name as studyId
        studyPath = utils.trimPathRight(studyPath);
        return studyPath.substring(studyPath.lastIndexOf("/") + 1);
    }

}