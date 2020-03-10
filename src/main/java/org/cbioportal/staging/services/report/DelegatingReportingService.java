/*
* Copyright (c) 2018 The Hyve B.V.
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as
* published by the Free Software Foundation, either version 3 of the
* License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program.  If not, see <https://www.gnu.org/licenses/>.
*/
package org.cbioportal.staging.services.report;

import static com.pivovarit.function.ThrowingConsumer.sneaky;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;

import org.cbioportal.staging.exceptions.ReporterException;
import org.cbioportal.staging.services.ExitStatus;
import org.cbioportal.staging.services.resource.Study;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
@Primary
public class DelegatingReportingService implements IReportingService {

    private static final Logger logger = LoggerFactory.getLogger(DelegatingReportingService.class);

	@Autowired(required = false)
    private List<IReportingService> delegates;

	@PostConstruct
	public void init() {
		if (delegates == null) {
            delegates = new ArrayList<>();
        }
        logger.debug("Reporting Service has found "+delegates.size()+" delegates.");
	}

	@Override
	public void reportStudyFileNotFound(Map<String, List<String>> failedStudies, Integer timeRetry) throws ReporterException {
		delegates.stream().forEach(sneaky(e -> e.reportStudyFileNotFound(failedStudies, timeRetry)));
	}

	@Override
	public void reportSummary(Study study, Resource transformerLogs, Resource validatorLogs, Resource validatorReports, 
    Resource loaderLogs, ExitStatus transformerStatus, ExitStatus validatorStatus, ExitStatus loaderStatus) throws ReporterException {
        logger.debug("Reporting Service calling reportSummary.");
        delegates.stream().forEach(sneaky(e -> e.reportSummary(study, transformerLogs, validatorLogs, validatorReports, loaderLogs,
            transformerStatus, validatorStatus, loaderStatus)));
    }

	@Override
	public void reportGenericError(String errorMessage, Exception e) throws ReporterException {
		delegates.stream().forEach(sneaky(i -> i.reportGenericError(errorMessage, e)));
	}

	public void register(IReportingService service) {
		delegates.add(service);
	}

}
