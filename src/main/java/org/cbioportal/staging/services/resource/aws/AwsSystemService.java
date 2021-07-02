package org.cbioportal.staging.services.resource.aws;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "scan.location.type" , havingValue = "aws")
public class AwsSystemService {

    private static final Logger logger = LoggerFactory.getLogger(AwsSystemService.class);

    @Autowired
    private ResourcePatternResolver resourcePatternResolver;

    @Value("${scan.location}")
    private String scanLocation;

    @Autowired
    private ResourceUtils utils;

    @ServiceActivator(inputChannel = "resource.ls")
    public List<Resource> awsGatewayLs(String directory) throws ResourceCollectionException {
        List<Resource> resources = awsGatewayLsDirRecur(directory);
        // Remove all resources that are not directly in the directory.
        // For correct downstream analysis of study directories this means that all
        // resources that are one dir away from the root are included
        // For instance, eval of:
        // [
        //   s3://scan.location/file1,
        //   s3://scan.location/dir1/file1,
        //   s3://scan.location/dir1/dir2/file1
        // ]
        // for dir 's3://scan.location/' will result in:
        // [
        //   s3://scan.location/file1,
        //   s3://scan.location/dir1/file1
        // ]
        // This is evaluated based on the number of fwd-slashes in the filename (S3 objectKey):
        //   s3://scan.location/[file1]           --> no slash     --> accepted
        //   s3://scan.location/[dir1/file1]      --> one slash    --> accepted
        //   s3://scan.location/[dir1/dir2/file1] --> two slashes  --> rejected
        // The code takes into account that scan.location van be a dir nested deep somewhere in the s3 bucket
        logger.debug("awsGatewayLsDir: before filtering :" + resources.stream().map(r -> r.getFilename()).collect(Collectors.joining(", ")));
        List<Resource> resourceList =  resources.stream()
            .filter(r -> {
                String scanLocationCorrectedFileName = utils.correctS3ResourcePathForScanLocation((SimpleStorageResource) r);
                return StringUtils.countMatches(scanLocationCorrectedFileName, "/") < 1;
            })
            .collect(Collectors.toList());
        logger.debug("awsGatewayLsDir: after filtering :" + resourceList.stream().map(r -> r.getFilename()).collect(Collectors.joining(", ")));
        return resourceList;
    }

    @ServiceActivator(inputChannel = "resource.ls.dir")
    public List<Resource> awsGatewayLsDir(String directory) throws ResourceCollectionException {
        List<Resource> resources = awsGatewayLsDirRecur(directory);
        // For correct downstream analysis of study directories, all resources that are not
        // directly in the directory param are excluded.
        // This is evaluated based on the number of fwd-slashes in the filename (S3 objectKey):
        //   s3://scan.location/[file1]           --> no slash     --> rejected
        //   s3://scan.location/[dir1/file1]      --> one slash    --> accepted
        //   s3://scan.location/[dir1/dir2/file1] --> two slashes  --> rejected
        // The code takes into account that scan.location van be a dir nested deep somewhere in the s3 bucket
        logger.debug("awsGatewayLsDir: before filtering :" + resources.stream().map(r -> r.getFilename()).collect(Collectors.joining(", ")));
        List<Resource> resourceList =  resources.stream()
            .filter(r -> {
                String scanLocationCorrectedFileName = utils.correctS3ResourcePathForScanLocation((SimpleStorageResource) r);
                return StringUtils.countMatches(scanLocationCorrectedFileName, "/") == 1;
            })
            .collect(Collectors.toList());
        logger.debug("awsGatewayLsDir: after filtering :" + resourceList.stream().map(r -> r.getFilename()).collect(Collectors.joining(", ")));
        return resourceList;
    }

    @ServiceActivator(inputChannel = "resource.ls.dir.recur")
    public List<Resource> awsGatewayLsDirRecur(String directory) throws ResourceCollectionException {
        return lsHelper(directory, "**/*", false);
    }

    private List<Resource> lsHelper(String dir, String wildcard, boolean excludeDirs) throws ResourceCollectionException {
        try {
            String path = utils.trimPathRight(dir);
            String wildCardPath = path + "/" + wildcard;
            logger.debug("Scanning at path: " + wildCardPath);
            Resource[] res = resourcePatternResolver.getResources(wildCardPath);
            return Arrays.asList(res);
        } catch (Exception e) {
            throw new ResourceCollectionException("Could not read from remote directory: " + dir, e);
        }
    }

    @ServiceActivator(inputChannel = "resource.get.stream")
    public InputStream awsGatewayGetStream(String file) {
        try {
            return resourcePatternResolver.getResource(file).getInputStream();
        } catch (Exception e) {
            throw new RuntimeException("Error reading file: " + file, e);
        }
    }

    @ServiceActivator(inputChannel = "resource.put")
    public String awsGatewayPut(Message<byte[]> message) {
        byte[] contents = message.getPayload();
        String destinationDir = (String) message.getHeaders().get("dest.dir");
        String fileName = (String) message.getHeaders().get("filename");
        try {
            URL path = utils.createRemoteURL("s3", destinationDir, fileName);
            WritableResource resource = (WritableResource) resourcePatternResolver
                .getResource(path.toString());
            try (OutputStream outputStream = resource.getOutputStream()) {
                outputStream.write(contents);
            }
            return path.toString();
        } catch (Exception e) {
            throw new RuntimeException("Error writing file: " + "s3:/" + destinationDir + "/" +fileName, e);
        }
    }

    @ServiceActivator(inputChannel = "resource.rm")
    public Boolean awsGatewayRm(String file) {
        throw new UnsupportedOperationException("File removal from AWS is not yet implmented for cbioportal staging app.");
    }

}