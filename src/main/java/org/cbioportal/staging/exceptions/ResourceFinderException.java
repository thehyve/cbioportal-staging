package org.cbioportal.staging.exceptions;

import java.io.IOException;

public class ResourceFinderException extends RuntimeException {
    public ResourceFinderException(String s) {
    }

    public ResourceFinderException(String error_while_evaluating_study_dir, Exception e) {
    }
}
