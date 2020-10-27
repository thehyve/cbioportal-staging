package org.cbioportal.staging.services.resource.filesystem;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;

@Configuration
@IntegrationComponentScan("org.cbioportal.staging.services.resource.filesystem")
public class LocalFileSystemConfig {

}