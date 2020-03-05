package org.cbioportal.staging.services.resource.filesystem;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

@Component
public class LocalFileSystemService {

    @ServiceActivator(inputChannel = "resource.ls")
    public List<File> ftpGatewayLs(String directory) {
        try {
            return Files.list(Paths.get(directory)).filter(Files::isRegularFile).map(Path::toFile)
                    .collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error listing files in directory: " + directory, e);
        }
    }

    @ServiceActivator(inputChannel = "resource.ls.dir")
    public List<File> ftpGatewayLsDir(String directory) {
        try {
            return Files.list(Paths.get(directory)).map(Path::toFile).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error listing files/directories in directory: " + directory, e);
        }
    }

    @ServiceActivator(inputChannel = "resource.ls.dir.recur")
    public List<File> ftpGatewayLsDirRecur(String directory) {
        try {
            return Files.walk(Paths.get(directory)).map(Path::toFile).collect(Collectors.toList());
        } catch (IOException e) {
            throw new RuntimeException("Error recursively listing files/directories in directory: " + directory, e);
        }

    }

    @ServiceActivator(inputChannel = "resource.get.stream")
    public InputStream ftpGatewayGetStream(String file) {
        try {
            return new FileInputStream(file);
        } catch (FileNotFoundException e) {
            throw new RuntimeException("Error reading file: " + file, e);
        }
    }

    @ServiceActivator(inputChannel = "resource.put")
    public String ftpGatewayPut(Message<byte[]> message) {
        byte[] contents = message.getPayload();
        String destinationDir = (String) message.getHeaders().get("dest.dir");
        String fileName = (String) message.getHeaders().get("filename");
        Path dest = Paths.get(destinationDir + "/" + fileName);
        try {
            Path file = Files.write(dest, contents);
            return file.toString();
        } catch (IOException e) {
            throw new RuntimeException("Error writing file: " + dest.toString(), e);
        }
    }

    @ServiceActivator(inputChannel = "resource.rm")
    public Boolean ftpGatewayRm(String file) {
        Path filePath = Paths.get(file);
        try {
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            throw new RuntimeException("Error deleting file: " + filePath.toString(), e);
        }
    }

}