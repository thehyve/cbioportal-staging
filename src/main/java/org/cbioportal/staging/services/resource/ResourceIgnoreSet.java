package org.cbioportal.staging.services.resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.annotation.PostConstruct;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

import com.pivovarit.function.ThrowingFunction;

/**
 *
 * Represents a list of resources that are ignored (not collected by the
 * resource collector). Ignored resources are read from a file which name is set
 * by the 'scna.ignore.file' property.
 *
 */
@Component
public class ResourceIgnoreSet extends HashSet<String> {

    private static final Logger logger = LoggerFactory.getLogger(ResourceIgnoreSet.class);

    private static final long serialVersionUID = -8289845398838148990L;

    @Value("${scan.ignore.file:}")
    private Resource ignoreFile;

    @Autowired
    private ResourceUtils utils;

    @Configuration
    public static class MyConfiguration {

        @Bean
        public BufferedReader bufferedReader(@Value("${scan.ignore.file:}") Resource ignoreFile)
                throws IOException {
            if (ignoreFile != null) {
                String filePath = ignoreFile.getFile().getAbsolutePath();
                File path = new File(filePath.substring(0, filePath.lastIndexOf("/")));
                path.mkdirs();
                ignoreFile.getFile().createNewFile();
                return new BufferedReader(new FileReader(ignoreFile.getFile()));
            }
            return null;
        }

    }

    @Autowired
    private ResourcePatternResolver resourcePatternResolver;

    @Autowired(required = false)
    private BufferedReader bufferedReader;

    @PostConstruct
    private void postConstruct() throws ResourceCollectionException {
        if (bufferedReader != null) {
            try {
                String line = bufferedReader.readLine();
                while (line != null) {
                    this.add(utils.getURI(resourcePatternResolver.getResource(line)).toString());
                    line = bufferedReader.readLine();
                }
            } catch (IOException e) {
                throw new ResourceCollectionException("Cannot read URL from Resource.", e);
            } finally {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    throw new ResourceCollectionException("Cannot read URL from Resource.", e);
                }
                logger.debug("Read " + String.valueOf(this.size()) + " files from the ignore file: "
                    + String.join(",", this));
            }
        }
    }

    public void appendResources(Resource[] resources) throws ResourceCollectionException {
        try {
            List<String> uris = Stream.of(resources).map(ThrowingFunction.sneaky(r -> utils.getURI(r).toString()))
            .collect(Collectors.toList());

            uris.stream().forEach(uri -> add(uri));

            if (ignoreFile != null && !ignoreFile.exists()) {
                ignoreFile = utils.createFileResource(ignoreFile, "");
            }

            if (ignoreFile != null && ignoreFile.exists()) {
                utils.writeToFile(utils.getWritableResource(ignoreFile), uris, true);
            }
        } catch (ResourceUtilsException e) {
            throw new ResourceCollectionException("Error adding resources to ignore file.", e);
        }
    }

    public void resetAndDeleteFile() throws ResourceUtilsException {
        super.clear();
        utils.deleteResource(ignoreFile);
    }

}
