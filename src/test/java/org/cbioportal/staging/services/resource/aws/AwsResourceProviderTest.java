package org.cbioportal.staging.services.resource.aws;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;


import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResource;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = AwsResourceProvider.class,
    properties = "scan.location.type=aws"
)
public class AwsResourceProviderTest {

  @Autowired
  private AwsResourceProvider awsResourceProvider;

  @MockBean
  private ResourceUtils utils;

  @MockBean
  private IAwsGateway gateway;

  @Test
  public void copyFromRemote_success()
      throws ResourceCollectionException, ResourceUtilsException {
    SimpleStorageResource resource = mock(SimpleStorageResource.class);
    when(resource.getFilename())
        .thenReturn("scan.location/study/meta_study.txt");
    Resource destinationDir = new FileSystemResource("/tmp/etl-workdir/study");
    awsResourceProvider.copyFromRemote(destinationDir, resource);
    verify(utils, times(1)).copyResource(destinationDir, resource, "meta_study.txt");
  }

  @Test
  public void copyFromRemote_successFileInRoot()
      throws ResourceCollectionException, ResourceUtilsException {
    SimpleStorageResource resource = mock(SimpleStorageResource.class);
    when(resource.getFilename())
        .thenReturn("meta_study.txt");
    Resource destinationDir = new FileSystemResource("/tmp/etl-workdir/study");
    awsResourceProvider.copyFromRemote(destinationDir, resource);
    verify(utils, times(1)).copyResource(destinationDir, resource, "meta_study.txt");
  }

}