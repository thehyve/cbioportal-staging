package org.cbioportal.staging.services.resource;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;

import org.springframework.core.io.Resource;

public class TestUtils {

    public static Resource createResource(String fileName, int number) {
        Resource r = mock(Resource.class);
        try {
            long modifiedDate = (long) number;
            when(r.getFilename()).thenReturn(fileName);
            when(r.getURL()).thenReturn(new URL(fileName));
            when(r.lastModified()).thenReturn(modifiedDate);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return r;
    }

    public static Resource createResource(String prefix, String extension, int number) {
        return createResource("file:/"+prefix+"_"+String.valueOf(number)+"."+extension, number);
    }

}