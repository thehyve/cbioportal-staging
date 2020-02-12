package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertFalse;
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
import java.util.Map;

import org.cbioportal.staging.TestUtils;
import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.etl.ETLProcessRunner;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.IScheduledScannerService;
import org.cbioportal.staging.services.reporting.IReportingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("unchecked")
@RunWith(SpringRunner.class)
@TestPropertySource(properties = { "scan.cron.iterations=5", "scan.ignore.append=false" })
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

    @Test
    public void testScan_sucess() throws Exception {

        Map<String, Resource[]> res = new HashMap<>();
        res.put("dummy", new Resource[0]);
        when(resourceCollector.getResources(isA(Resource.class))).thenReturn(res);

        doNothing().when(etlProcessRunner).run(any(Map.class));

        scheduledScanner.scan();

        verify(reportingService, never()).reportGenericError(anyString(), any());
        verify(scheduledScannerService, never()).stopAppWithSuccess();
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

        Map<String, Resource[]> res = new HashMap<>();
        res.put("dummy", new Resource[0]);
        when(resourceCollector.getResources(isA(Resource.class))).thenReturn(res);

        doThrow(ResourceCollectionException.class).when(etlProcessRunner).run(any(Map.class));

        scheduledScanner.scan();

        verify(reportingService, times(1)).reportGenericError(anyString(), any());
        verify(scheduledScannerService, times(1)).stopApp();
    }

    @Test
    public void testScan_gracefulExitAfterIneffectiveScans() throws Exception {

        Map<String, Resource[]> emptyRes = new HashMap<>();
        when(resourceCollector.getResources(isA(Resource.class))).thenReturn(emptyRes);

        boolean exitStatus = scheduledScanner.scan();

        verify(reportingService, never()).reportGenericError(anyString(), any());
        assertFalse(exitStatus);
    }

    @Test
    public void testScan_addsToIgnoreSet() throws Exception {

        ReflectionTestUtils.setField(scheduledScanner, "ignoreAppend", true);

        Map<String, Resource[]> res = new HashMap<>();
        Resource[] resToBeIgnored = new Resource[] {TestUtils.createMockResource("file:/success_resource.txt", 0)};
        Resource[] resNotToBeIgnored = new Resource[] {TestUtils.createMockResource("file:/failure_resource.txt", 0)};
        res.put("dummy_study_success", resToBeIgnored);
        res.put("dummy_study_failure", resNotToBeIgnored);
        when(resourceCollector.getResources(isA(Resource.class))).thenReturn(res);

        Map<String, ExitStatus> exit = new HashMap<>();
        exit.put("dummy_study_success", ExitStatus.SUCCESS);
        exit.put("dummy_study_failure", ExitStatus.ERROR);
        when(etlProcessRunner.getLoaderExitStatus()).thenReturn(exit);

        scheduledScanner.scan();

        verify(ignoreSet, times(1)).appendResources(eq(resToBeIgnored));
        verify(ignoreSet, never()).appendResources(eq(resNotToBeIgnored));
    }

}