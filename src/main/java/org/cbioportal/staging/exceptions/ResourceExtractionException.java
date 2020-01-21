package org.cbioportal.staging.exceptions;

public class ResourceExtractionException extends Exception {

    private static final long serialVersionUID = 1L;

    public ResourceExtractionException(String message) {
        super(message);
	}

    public ResourceExtractionException(String message, Throwable e) {
        super(message, e);
	}

}
