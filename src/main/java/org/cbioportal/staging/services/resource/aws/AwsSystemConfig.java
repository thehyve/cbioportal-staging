package org.cbioportal.staging.services.resource.aws;

import com.amazonaws.services.s3.AmazonS3;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.aws.core.io.s3.PathMatchingSimpleStorageResourcePatternResolver;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.integration.annotation.IntegrationComponentScan;

@Configuration
@ConditionalOnProperty(value="scan.location.type", havingValue ="aws")
@IntegrationComponentScan("org.cbioportal.staging.services.resource.aws")
public class AwsSystemConfig {

  @Autowired
  private ApplicationContext applicationContext;

  @Autowired
  private AmazonS3 amazonS3;

  @Bean
  @Primary
  public ResourcePatternResolver resourcePatternResolver() {
    return new PathMatchingSimpleStorageResourcePatternResolver(amazonS3, applicationContext);
  }

}