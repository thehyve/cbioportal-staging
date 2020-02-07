package org.cbioportal.staging.services;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.services.reporting.DelegatingReportingService;
import org.cbioportal.staging.services.reporting.EmailReportingService;
import org.cbioportal.staging.services.reporting.LogReportingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = { DelegatingReportingService.class })
public class DelegatingReportingServiceTest {

    @MockBean
    private LogReportingService service1;

    @MockBean
    private EmailReportingService service2;

    @SpyBean
    private DelegatingReportingService delegatingReportingService;

    @Test
    public void delegateRegistration_excludeSelfInDelegation() throws ReporterException {
        delegatingReportingService.reportGenericError("test", mock(Exception.class));
        verify(delegatingReportingService, times(1)).reportGenericError(eq("test"), any(Exception.class) );
        verify(service1, times(1)).reportGenericError(eq("test"), any(Exception.class) );
        verify(service2, times(1)).reportGenericError(eq("test"), any(Exception.class) );
    }

}