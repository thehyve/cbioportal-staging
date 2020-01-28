package org.cbioportal.staging.services.resource;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.stereotype.Component;

/**
 * ResourceIgnoreSet
 *
 * Represents a list of resources that are ignored (not collected
 * by the resource collector). Ignored resources are read from a
 * file which name is set by the 'scna.ignore.file' property.
 *
 */
@Component
public class ResourceIgnoreSet extends HashSet<String> {

    private static final Logger logger = LoggerFactory.getLogger(ResourceIgnoreSet.class);

    private static final long serialVersionUID = -8289845398838148990L;

    // defined bufferedreader here so that
    // it can be mocked in the test.
    @Configuration
    public static class MyConfiguration {

        @Bean
        public BufferedReader bufferedReader(@Value("${scan.ignore.file:}") File ignoreFile) throws FileNotFoundException {
            if (ignoreFile != null) {
                return new BufferedReader(new FileReader(ignoreFile));
            }
            return null;
        }

    }

    @Autowired
    private ResourcePatternResolver resourcePatternResolver;

    @Autowired
    private BufferedReader bufferedReader;

    @PostConstruct
    private void postConstruct() {
        if (bufferedReader != null) {
            try {
                String line = bufferedReader.readLine();
                while (line != null) {
                    this.add(resourcePatternResolver.getResource(line).getURL().toString());
                    line = bufferedReader.readLine();
                }
            } catch (IOException e) {
                // will never happen ...
                throw new RuntimeException("Cannot read URL from Resource.");
            } finally {
                try {
                    bufferedReader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                logger.debug("Read " + String.valueOf(this.size()) + " files from the ignore file: " + String.join(",", this));
            }
        }
    }

}
