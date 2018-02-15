/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
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
