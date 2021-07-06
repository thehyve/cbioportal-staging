package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isA;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.etl.ETLProcessRunner;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.report.IReportingService;
import org.cbioportal.staging.services.scanner.IScheduledScannerService;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@RunWith(SpringRunner.class)
@TestPropertySource(
    properties = {
        "scan.cron.iterations=5",
        "scan.ignore.appendonsuccess=false",
        "scan.ignore.appendonfailure=false"
    }
    )
@SpringBootTest(classes = ScheduledScanner.class)
public class ScheduledScannerTest {

    @MockBean
    private IResourceCollector resourceCollector;

    @MockBean
    private ETLProcessRunner etlProcessRunner;

    @MockBean
    private IReportingService reportingService;

    @MockBean
    private IScheduledScannerService scheduledScannerService;

    @Autowired
    private ScheduledScanner scheduledScanner;

    @MockBean
    private ResourceIgnoreSet ignoreSet;

    @Captor
    ArgumentCaptor<Resource[]> valueCaptor;

    @Before
    public void init() {
        ReflectionTestUtils.setField(scheduledScanner, "ignoreAppendSuccess", false);
        ReflectionTestUtils.setField(scheduledScanner, "ignoreAppendFailure", false);
    }


    @Test
    public void testScan_success() throws Exception {

        Study[] res = TestUtils.studyList(new Study("dummy", null, null, null, new Resource[0]));
        when(resourceCollector.getResources(isA(Resource.class))).thenReturn(res);

        doNothing().when(etlProcessRunner).run(any(Study[].class));

        scheduledScanner.scan();

        verify(reportingService, never()).reportGenericError(anyString(), any());
        verify(scheduledScannerService, times(1)).stopAppWithSuccess();
    }

    @Test
    public void testScan_resourceCollectionFails() throws Exception {

        doThrow(ResourceCollectionException.class).when(resourceCollector).getResources(isA(Resource.class));

        scheduledScanner.scan();

        verify(reportingService, times(1)).reportGenericError(anyString(), any());
        verify(scheduledScannerService, times(1)).stopApp();
    }

    @Test
    public void testScan_etlFails() throws Exception {

        Study[] res = TestUtils.studyList(new Study("dummy", null, null, null, new Resource[0]));
        when(resourceCollector.getResources(isA(Resource.class))).thenReturn(res);

        doThrow(ResourceCollectionException.class).when(etlProcessRunner).run(any(Study[].class));

        scheduledScanner.scan();

        verify(reportingService, times(1)).reportGenericError(anyString(), any());
        verify(scheduledScannerService, times(2)).stopApp();
    }

    @Test
    public void testScan_gracefulExitAfterIneffectiveScans() throws Exception {

        Study[] emptyRes = new Study[0];
        when(resourceCollector.getResources(isA(Resource.class))).thenReturn(emptyRes);

        boolean exitStatus = scheduledScanner.scan();

        verify(reportingService, never()).reportGenericError(anyString(), any());
        assertTrue(exitStatus);
    }

    @Test
    public void testScan_addsToIgnoreSetOnSuccess() throws Exception {

        ReflectionTestUtils.setField(scheduledScanner, "ignoreAppendSuccess", true);


        Resource[] resToBeIgnored = new Resource[] {TestUtils.createMockResource("file:/success_resource.txt", 0)};
        Resource[] resNotToBeIgnored = new Resource[] {TestUtils.createMockResource("file:/failure_resource.txt", 0)};
        Study dummyStudySuccess = new Study("dummy_study_success", null, null, null, resToBeIgnored);
        Study dummyStudyFailure = new Study("dummy_study_failure", null, null, null, resNotToBeIgnored);
        Study[] res = TestUtils.studyList(dummyStudySuccess, dummyStudyFailure);

        when(resourceCollector.getResources(isA(Resource.class))).thenReturn(res);

        Map<Study, ExitStatus> exit = new HashMap<>();
        exit.put(dummyStudySuccess, ExitStatus.SUCCESS);
        exit.put(dummyStudyFailure, ExitStatus.ERROR);
        when(etlProcessRunner.getLoaderExitStatus()).thenReturn(exit);

        scheduledScanner.scan();

        verify(ignoreSet, times(1)).appendResources(eq(resToBeIgnored));
        verify(ignoreSet, never()).appendResources(eq(resNotToBeIgnored));
    }


    @Test
    public void testScan_addsToIgnoreSetOnFailure() throws Exception {

        ReflectionTestUtils.setField(scheduledScanner, "ignoreAppendSuccess", true);
        ReflectionTestUtils.setField(scheduledScanner, "ignoreAppendFailure", true);

        Resource[] successResource = new Resource[] {TestUtils.createMockResource("file:/success_resource.txt", 0)};
        Resource[] failureResource = new Resource[] {TestUtils.createMockResource("file:/failure_resource.txt", 0)};
        Study dummyStudySuccess = new Study("dummy_study_success", null, null, null, successResource);
        Study dummyStudyFailure = new Study("dummy_study_failure", null, null, null, failureResource);
        Study[] res = TestUtils.studyList(dummyStudySuccess, dummyStudyFailure);

        when(resourceCollector.getResources(isA(Resource.class))).thenReturn(res);

        Map<Study, ExitStatus> exit = new HashMap<>();
        exit.put(dummyStudySuccess, ExitStatus.SUCCESS);
        exit.put(dummyStudyFailure, ExitStatus.ERROR);
        when(etlProcessRunner.getLoaderExitStatus()).thenReturn(exit);

        scheduledScanner.scan();

        verify(ignoreSet, times(2)).appendResources(valueCaptor.capture());
        List<String> ignoredFilenames = valueCaptor.getAllValues().stream()
            .flatMap(s -> Stream.of(s))
            .map(Resource::getFilename)
            .collect(Collectors.toList());

        assertTrue(ignoredFilenames.contains("success_resource.txt"));
        assertTrue(ignoredFilenames.contains("failure_resource.txt"));

    }

}