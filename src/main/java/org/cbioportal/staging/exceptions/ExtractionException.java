package org.cbioportal.staging.exceptions;

public class ExtractionException extends Exception {

    private static final long serialVersionUID = 1L;

    public ExtractionException(String message) {
        super(message);
	}

    public ExtractionException(String message, Throwable e) {
        super(message, e);
	}

}
