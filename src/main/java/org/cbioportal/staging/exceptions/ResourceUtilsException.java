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

public class ResourceUtilsException extends Exception
{
	private static final long serialVersionUID = 1L;

	public ResourceUtilsException() {
		super();
	}
	public ResourceUtilsException(String message, Exception e) {super(message, e);}
	public ResourceUtilsException(String message) {super(message);}
	public ResourceUtilsException(Exception e) {super(e);}

}