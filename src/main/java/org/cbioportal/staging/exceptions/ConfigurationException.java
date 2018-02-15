/*
* Copyright (c) 2018 The Hyve B.V.
* This code is licensed under the GNU Affero General Public License,
* version 3, or (at your option) any later version.
*/
package org.cbioportal.staging.exceptions;

public class ConfigurationException extends Exception {
	
	/**
	 * 
	 */
	private static final long serialVersionUID = -20480534356703321L;
	
	public ConfigurationException()
	{
	}
	public ConfigurationException(String message, Exception e)
	{
		super(message);
	}
	public ConfigurationException(String message)
	{
		super(message);
	}

}
