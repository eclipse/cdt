/*******************************************************************************
 * Copyright (c) 2017 Red Hat Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Inc. - initial API
 *******************************************************************************/
package org.eclipse.cdt.internal.errorparsers;

public class Fixit {
	
	private String range;
	private String change;
	private int lineNumber;
	private int columnNumber;
	private int length;
	
	public Fixit(String range, String change) {
		this.range = range;
		this.change = change;
		parseRange();
	}
	
	private void parseRange() {
		String[] region = range.split("-"); //$NON-NLS-1$
		String start = region[0];
		String[] token = start.split(":"); //$NON-NLS-1$
		this.lineNumber = Integer.valueOf(token[0]).intValue();
		this.columnNumber = Integer.valueOf(token[1]).intValue();
		String end = region[1];
		token = end.split(":"); //$NON-NLS-1$
		int endColumnNumber = Integer.valueOf(token[1]).intValue();
		this.length = endColumnNumber - columnNumber;
	}
	
	/**
	 * Get line number.
	 * 
	 * @return 1-based line number of fix-it
	 */
	public int getLineNumber() {
		return lineNumber;
	}
	
	/**
	 * Get column number.
	 * 
	 * @return 1-based column number of fix-it
	 */
	public int getColumnNumber() {
		return columnNumber;
	}
	
	/**
	 * Get length.
	 * 
	 * @return length of change for fix-it
	 */
	public int getLength() {
		return length;
	}
	
	/**
	 * Get the change string.
	 * @return the string to change the region to (can be empty).
	 */
	public String getChange() {
		return change;
	}
}
