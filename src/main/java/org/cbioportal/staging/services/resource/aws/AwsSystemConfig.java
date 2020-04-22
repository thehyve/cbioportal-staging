package org.cbioportal.staging.services.resource.aws;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.IntegrationComponentScan;

@Configuration
@ConditionalOnProperty(value="scan.location.type", havingValue ="aws")
@IntegrationComponentScan("org.cbioportal.staging.services.resource.aws")
public class AwsSystemConfig {

}