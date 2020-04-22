package org.cbioportal.staging.services.report;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.io.IOException;

import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.WritableResource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = { TextLogFileAppender.class },
    properties = {
        "log.format=text",
        "log.html.prepend=true"
    }
)
public class TextLogFileAppenderTest {

    @Autowired
    private TextLogFileAppender appender;

    @MockBean
    private ResourceUtils resourceUtils;

    @Before
    public void init() throws ResourceUtilsException, ReporterException, IOException {
        doNothing().when(resourceUtils).writeToFile(any(WritableResource.class), anyString(), anyBoolean());
    }

    @Test
    public void testAppend_isDefault() throws ReporterException, IOException, ResourceUtilsException {
        appender.addToLog(mock(WritableResource.class), "message");
        verify(resourceUtils, times(1)).writeToFile(any(WritableResource.class), eq("message"), eq(true));
    }

}