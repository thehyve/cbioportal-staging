/*
* Copyright (c) 2018 The Hyve B.V.
* This program is free software: you can redistribute it and/or modify
* it under the terms of the GNU Affero General Public License as
* published by the Free Software Foundation, either version 3 of the
* License, or (at your option) any later version.
*
* This program is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU Affero General Public License for more details.
*
* You should have received a copy of the GNU Affero General Public License
* along with this program.  If not, see <https://www.gnu.org/licenses/>.
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
	public TransformerException(String message) {
		super (message);
	}
}