package org.cbioportal.staging.exceptions;

public class LoaderException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5072243529310767969L;
	
	public LoaderException()
	{
	}
	public LoaderException(String message, Exception e)
	{
		super(message);
	}
	public LoaderException(String message)
	{
		super(message);
	}

}
