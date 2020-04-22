package org.cbioportal.staging.services.resource;

import org.cbioportal.staging.exceptions.ResourceCollectionException;
import org.springframework.core.io.Resource;

public interface IStudyResourceStrategy {

    Study[] resolveResources(Resource[] resources) throws ResourceCollectionException;

}
