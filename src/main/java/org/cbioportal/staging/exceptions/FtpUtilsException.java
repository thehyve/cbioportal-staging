package org.cbioportal.staging.exceptions;

public class FtpUtilsException extends Exception {

    private static final long serialVersionUID = -585493174762188709L;

    public FtpUtilsException() {
        super();
    }
	public FtpUtilsException(String message, Exception e) {super(message, e);}
	public FtpUtilsException(String message) {super(message);}
	public FtpUtilsException(Exception e) {super(e);}

}
