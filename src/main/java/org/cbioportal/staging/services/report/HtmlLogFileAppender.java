package org.cbioportal.staging.services.report;

import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.services.resource.ResourceUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.WritableResource;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(value = "log.format", havingValue = "html")
public class HtmlLogFileAppender implements ILogFileAppender {

	@Value("${log.html.prepend:false}")
	private boolean prepend;

	@Autowired
    private ResourceUtils resourceUtils;

    @Override
    public void addToLog(WritableResource file, String text) throws ReporterException {
        try {
            Document log = Jsoup.parse(resourceUtils.getFile(file), "UTF-8");
            if (prepend) {
                log.body().prepend(text);
            } else {
                log.body().append(text);
            }
            resourceUtils.writeToFile(file, log.html(), false);
        } catch (Exception e) {
            throw new ReporterException("Cannot write to html log file.", e);
        }
    }

}