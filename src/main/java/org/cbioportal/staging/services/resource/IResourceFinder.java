package org.cbioportal.staging.services.resource;

import org.cbioportal.staging.exceptions.ResourceFinderException;
import org.springframework.core.io.Resource;

public interface IResourceFinder {

    Resource getInputDirectory();

//    Resource getStudyDir(Study s) throws Exception, ResourceFinderException;
}
