package org.cbioportal.staging;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.stream.Stream;

import org.cbioportal.staging.services.resource.Study;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Component;

@Component
public class TestUtils {

    public static WritableResource createMockResource(String fileName, int number) {
        WritableResource r = mock(WritableResource.class);
        InputStream i = mock(InputStream.class);
        try {
            long modifiedDate = (long) number;
            URL url = new URL(fileName);
            when(r.getFilename()).thenReturn(fileName.substring(fileName.lastIndexOf("/")+1));
            when(r.getURL()).thenReturn(url);
            when(r.lastModified()).thenReturn(modifiedDate);
            when(r.exists()).thenReturn(true);
            when(r.getInputStream()).thenReturn(i);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return r;
    }

    public static WritableResource createMockResource(String prefix, String extension, int number) {
        return createMockResource("file:/"+prefix+"_"+String.valueOf(number)+"."+extension, number);
    }

    public static Study[] studyList(Study ... s) {
        return s;
    }

    public static boolean has(Study[] studies, String id) {
        return Stream.of(studies).filter(s -> s.getStudyId().equals(id)).findAny().isPresent();
    }

}