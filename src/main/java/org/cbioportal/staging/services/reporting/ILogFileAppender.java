package org.cbioportal.staging.services.reporting;

import org.cbioportal.staging.exceptions.ReporterException;
import org.springframework.core.io.WritableResource;

public interface ILogFileAppender {

    public void addToLog(WritableResource file, String text) throws ReporterException;

}