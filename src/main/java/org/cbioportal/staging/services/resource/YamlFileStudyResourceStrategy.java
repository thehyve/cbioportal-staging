package org.cbioportal.staging.services.resource;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.Yaml;

/**
 * YamlResourceStrategy
 *
 * Selects the newest Yaml file and extracts a
 * list of resources per study.
 */
@Component
public class YamlFileStudyResourceStrategy implements IStudyResourceStrategy {

    private static final Logger logger = LoggerFactory.getLogger(YamlFileStudyResourceStrategy.class);

    @Configuration
    static class MyConfiguration {

        @Bean
        public Yaml yamlParser() {
            return new Yaml();
        }

    }

    @Value("${scan.yaml.fileprefix:list_of_studies}")
    private String yamlPrefix;

    @Value("${scan.location}")
    private String scanLocation;

    @Autowired
    private ResourcePatternResolver resourcePatternResolver;

    @Autowired
    private Yaml yamlParser;

    @Autowired
    private ResourceUtils utils;

    @Override
    public Study[] resolveResources(Resource[] resources) throws ResourceCollectionException {

        List<Study> out = new ArrayList<>();
        String timestamp = utils.getTimeStamp("yyyyMMdd-HHmmss");
        try {

            logger.info("Looking for newest yaml file...");

            Resource[] yamlFiles = utils.filterFiles(resources, yamlPrefix, "[yaml|yml]");
            Resource yamlFile = utils.getMostRecent(yamlFiles);

            if (yamlFile != null) {

                logger.info("Most recent yaml file is: " + yamlFile.getFilename());

                Map<String, List<String>> parsedYaml = parseYaml(yamlFile);

                for (Entry<String, List<String>> entry : parsedYaml.entrySet()) {
                    List<Resource> collectedResources = new ArrayList<>();
                    for (String filePath : entry.getValue() ) {
                        String fullFilePath = filePath(filePath);
                        collectedResources.add(resourcePatternResolver.getResource(fullFilePath));
                    }
                    out.add(new Study(entry.getKey(), yamlFile.getFilename(), timestamp, null, collectedResources.toArray(new Resource[0])));
                }
            }

        } catch (IOException e) {
            throw new ResourceCollectionException("Cannot read from yaml file.");
        }

        return out.toArray(new Study[0]);
    }

    private Map<String, List<String>> parseYaml(Resource resource) throws IOException {
		InputStream is = resource.getInputStream();
		@SuppressWarnings("unchecked")
		Map<String, List<String>> result = (Map<String, List<String>>) yamlParser.load(is);
		return result;
    }

    private String filePath(String filePath) {
        String trimmedScanLocation = utils.trimDir(scanLocation);
        String trimmedFilePath = filePath.replaceFirst("^\\/+", "");
        return trimmedScanLocation + "/" + trimmedFilePath;
    }

}