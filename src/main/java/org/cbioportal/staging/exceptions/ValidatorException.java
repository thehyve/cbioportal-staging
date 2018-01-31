package org.cbioportal.staging.exceptions;

public class ValidatorException extends Exception {
	/**
	 * 
	 */
	private static final long serialVersionUID = -5072243529310767969L;
	
	public ValidatorException()
	{
	}
	public ValidatorException(String message, Exception e)
	{
		super(message);
	}
	public ValidatorException(String message)
	{
		super(message);
	}

}
