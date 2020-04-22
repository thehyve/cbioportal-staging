package org.cbioportal.staging.services.report;

import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.exceptions.ResourceUtilsException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "log.format", havingValue = "text", matchIfMissing = true)
public class TextLogFileAppender implements ILogFileAppender {

    @Autowired
    private ResourceUtils resourceUtils;

    @Override
    public void addToLog(WritableResource file, String text) throws ReporterException {
        try {
            resourceUtils.writeToFile(file, text, true);
        } catch (ResourceUtilsException e) {
            throw new ReporterException("Cannot write to text log file.", e);
        }
    }

}
