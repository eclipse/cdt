/*******************************************************************************
 * Copyright (c) 2004, 2006 QNX Software Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     QNX Software Systems - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.utils.coff.parser;

import org.eclipse.cdt.core.IBinaryParser;
import org.eclipse.cdt.core.IBinaryParser.IBinaryExecutable;
import org.eclipse.cdt.core.IBinaryParser.IBinaryFile;
import org.eclipse.core.runtime.IPath;


public class CygwinPEBinaryExecutable extends CygwinPEBinaryObject implements IBinaryExecutable {

	/**
	 * @param parser
	 * @param path
	 * @param executable
	 */
	public CygwinPEBinaryExecutable(IBinaryParser parser, IPath path, int executable) {
		super(parser, path, IBinaryFile.EXECUTABLE);
	}

}
