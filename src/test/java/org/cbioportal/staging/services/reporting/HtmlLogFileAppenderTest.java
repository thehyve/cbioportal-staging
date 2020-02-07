package org.cbioportal.staging.services.reporting;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;
import org.powermock.modules.junit4.PowerMockRunnerDelegate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.core.io.WritableResource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(PowerMockRunner.class)
@PowerMockRunnerDelegate(SpringRunner.class)
@PrepareForTest(Jsoup.class)
@SpringBootTest(
    classes = { HtmlLogFileAppender.class },
    properties = {
        "log.format=html"
    }
)
public class HtmlLogFileAppenderTest {

    @Autowired
    private HtmlLogFileAppender appender;

    @Mock(answer = Answers.RETURNS_DEEP_STUBS)
    private Document jsoupNodes;

    @MockBean
    private ResourceUtils resourceUtils;

    @Before
    public void init() throws ResourceUtilsException, ReporterException, IOException {
        when(resourceUtils.getFile(any(Resource.class))).thenReturn(null);
        PowerMockito.mockStatic(Jsoup.class);
        PowerMockito.when(Jsoup.parse(any(File.class), anyString())).thenReturn(jsoupNodes);
    }

    @Test
    public void testAppend_isDefault() throws ReporterException, IOException {
        appender.addToLog(mock(WritableResource.class), "message");
        verify(jsoupNodes.body()).append(eq("message"));
    }

    @Test
    public void testPrepend_onProperty() throws ReporterException, IOException {
        ReflectionTestUtils.setField(appender, "prepend", true);
        appender.addToLog(mock(WritableResource.class), "message");
        verify(jsoupNodes.body()).prepend(eq("message"));
    }

}