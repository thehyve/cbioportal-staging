package org.cbioportal.staging.exceptions;

public class ResourceTransmissionException extends Exception {

    private static final long serialVersionUID = 1L;

    public ResourceTransmissionException(String message) {
        super(message);
    }

    public ResourceTransmissionException(String message, Throwable e) {
        super(message, e);
    }

    public ResourceTransmissionException(Throwable e) {
        super(e);
    }

}
