package org.cbioportal.staging.services.report;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Map;

import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("unchecked")
@RunWith(SpringRunner.class)
@SpringBootTest(
    classes = { LogReportingService.class },
    properties = {
        "log.enable=true",
        "log.file.createnotexist=false",
        "log.file=file:/dummy.log"
    }
)
public class LogReportingServiceTest {

    @Autowired
    private LogReportingService reportingService;

    @MockBean
    private LogMessageUtils messageUtils;

    @MockBean
    private ResourceUtils resourceUtils;

    @MockBean
    private ILogFileAppender appender;

    @Before
    public void init() throws ResourceUtilsException, ReporterException, IOException {
        when(messageUtils.messageStudyFileNotFound(eq("template"), any(Map.class), eq(2))).thenReturn("");
        when(resourceUtils.getWritableResource(isA(Resource.class))).thenReturn(null);
    }

    @Test
    public void testUseCorrectTemplate_text() throws ReporterException {
        ReflectionTestUtils.setField(reportingService, "logFormat", "text");
        reportingService.reportStudyFileNotFound(mock(Map.class), 1);
        verify(messageUtils, times(1)).messageStudyFileNotFound(eq("studyFileNotFound_log_txt.ftl"), any(Map.class), eq(1));
    }

    @Test
    public void testUseCorrectTemplate_html() throws ReporterException {
        ReflectionTestUtils.setField(reportingService, "logFormat", "html");
        reportingService.reportStudyFileNotFound(mock(Map.class), 1);
        verify(messageUtils, times(1)).messageStudyFileNotFound(eq("studyFileNotFound_log_html.ftl"), any(Map.class), eq(1));
    }

}