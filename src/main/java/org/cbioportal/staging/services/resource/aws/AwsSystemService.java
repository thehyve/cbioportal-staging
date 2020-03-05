package org.cbioportal.staging.services.resource.aws;

import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.pivovarit.function.ThrowingFunction;
import com.pivovarit.function.ThrowingPredicate;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.core.io.support.ResourcePatternResolver;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;


// NOTE: after refactoring the functionality of this class was not tested in S3 environment

@Component
public class AwsSystemService {

    @Autowired
    private ResourcePatternResolver resourceResolver;

    @Autowired
    private ResourceUtils utils;

    @ServiceActivator(inputChannel = "resource.ls")
    public List<File> ftpGatewayLs(String directory) throws ResourceCollectionException {
        return lsHelper(directory, "*", true);
    }

    @ServiceActivator(inputChannel = "resource.ls.dir")
    public List<File> ftpGatewayLsDir(String directory) throws ResourceCollectionException {
        return lsHelper(directory, "*", false);
    }

    @ServiceActivator(inputChannel = "resource.ls.dir.recur")
    public List<File> ftpGatewayLsDirRecur(String directory) throws ResourceCollectionException {
        return lsHelper(directory, "**", false);
    }

    private List<File> lsHelper(String dir, String wildcard, boolean exludeDirs) throws ResourceCollectionException {
        try {
            String path = utils.trimPathRight(dir);
            String wildCardPath = path + "/" + wildcard;
            Resource[] res = resourceResolver.getResources(wildCardPath);
            return Stream.of(res)
                .filter(ThrowingPredicate.sneaky(e -> ! exludeDirs || e.getFile().isFile()))
                .map(ThrowingFunction.sneaky(r -> r.getFile()))
                .collect(Collectors.toList());
        } catch (Exception e) {
            throw new ResourceCollectionException("Could not read from remote directory: " + dir, e);
        }
    }

    @ServiceActivator(inputChannel = "resource.get.stream")
    public InputStream ftpGatewayGetStream(String file) {
        try {
            return resourceResolver.getResource(file).getInputStream();
        } catch (Exception e) {
            throw new RuntimeException("Error reading file: " + file, e);
        }
    }

    @ServiceActivator(inputChannel = "resource.put")
    public String ftpGatewayPut(Message<byte[]> message) {
        byte[] contents = message.getPayload();
        String destinationDir = (String) message.getHeaders().get("dest.dir");
        String fileName = (String) message.getHeaders().get("filename");
        try {
            URL path = utils.createRemoteURL("s3", destinationDir, fileName);
            WritableResource resource = (WritableResource) resourceResolver
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
    public Boolean ftpGatewayRm(String file) {
        throw new UnsupportedOperationException("File removal from AWS is not yet implmented for cbioportal staging app.");
    }

}