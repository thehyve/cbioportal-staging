package org.cbioportal.staging.services.resource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
public class TestUtils {

    public static FileSystemResource createMockResource(String fileName, int number) {
        FileSystemResource r = mock(FileSystemResource.class);
        try {
            long modifiedDate = (long) number;
            URL url = new URL(fileName);
            when(r.getFilename()).thenReturn(fileName);
            when(r.getURL()).thenReturn(url);
            when(r.lastModified()).thenReturn(modifiedDate);
            when(r.exists()).thenReturn(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return r;
    }

    public static Resource createResource(String prefix, String extension, int number) {
        return createMockResource("file:/"+prefix+"_"+String.valueOf(number)+"."+extension, number);
    }

}