package org.cbioportal.staging.services.resource;

import static org.junit.Assert.assertFalse;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.cbioportal.staging.app.ScheduledScanner;
import org.cbioportal.staging.etl.ETLProcessRunner;
import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.cbioportal.staging.services.EmailService;
import org.cbioportal.staging.services.ScheduledScannerService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.core.io.Resource;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@TestPropertySource(properties = { "scan.cron.iterations=5" })
@SpringBootTest(classes = ScheduledScanner.class)
public class ScheduledScannerTest {

    @MockBean
    private IResourceCollector resourceCollector;

    @MockBean
    private ETLProcessRunner etlProcessRunner;

    @MockBean
    private EmailService emailService;

    @MockBean
    private ScheduledScannerService scheduledScannerService;

    @Autowired
    private ScheduledScanner scheduledScanner;

    @Test
    public void testScan_sucess() throws Exception {

        Map<String, Resource[]> res = new HashMap<>();
        res.put("dummy", new Resource[0]);
        when(resourceCollector.getResources(any(Resource.class))).thenReturn(res);

        doNothing().when(etlProcessRunner).run(any(Map.class));

        scheduledScanner.scan();

        verify(emailService, never()).emailGenericError(anyString(), any());
        verify(scheduledScannerService, never()).stopAppWithSuccess();
    }

    @Test
    public void testScan_resourceCollectionFails() throws Exception {

        doThrow(ResourceCollectionException.class).when(resourceCollector).getResources(any(Resource.class));

        scheduledScanner.scan();

        verify(emailService, times(1)).emailGenericError(anyString(), any());
        verify(scheduledScannerService, times(1)).stopApp();
    }

    @Test
    public void testScan_etlFails() throws Exception {

        Map<String, Resource[]> res = new HashMap<>();
        res.put("dummy", new Resource[0]);
        when(resourceCollector.getResources(any(Resource.class))).thenReturn(res);

        doThrow(ResourceCollectionException.class).when(etlProcessRunner).run(any(Map.class));

        scheduledScanner.scan();

        verify(emailService, times(1)).emailGenericError(anyString(), any());
        verify(scheduledScannerService, times(1)).stopApp();
    }

    @Test
    public void testScan_gracefulExitAfterIneffectiveScans() throws Exception {

        Map<String, Resource[]> emptyRes = new HashMap<>();
        when(resourceCollector.getResources(any(Resource.class))).thenReturn(emptyRes);

        boolean exitStatus = scheduledScanner.scan();

        verify(emailService, never()).emailGenericError(anyString(), any());
        assertFalse(exitStatus);
    }

}