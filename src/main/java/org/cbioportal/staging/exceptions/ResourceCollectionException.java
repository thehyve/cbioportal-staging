package org.cbioportal.staging.exceptions;

public class ResourceCollectionException extends Exception {

    private static final long serialVersionUID = 1L;

    public ResourceCollectionException(String message) {
        super(message);
    }

    public ResourceCollectionException(String message, Throwable e) {
        super(message, e);
    }

}
