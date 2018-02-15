/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.exceptions;

import java.io.FileNotFoundException;
import java.io.IOException;

public class TransformerException extends Exception
{
	/**
	 * 
	 */
	private static final long serialVersionUID = 8901822768870101717L;

	public TransformerException()
	{
	}
	public TransformerException(String message, IOException e)
	{
		super(message);
	}
	
	public TransformerException(String message, FileNotFoundException e)
	{
		super(message);
	}
}