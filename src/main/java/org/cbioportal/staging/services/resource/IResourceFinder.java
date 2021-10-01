package org.cbioportal.staging.services.resource;

import org.springframework.core.io.Resource;

public interface IResourceFinder {

    Resource getInputDirectory();

}
