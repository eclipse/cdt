/*******************************************************************************
 * Copyright (c) 2008, 2020 QNX Software Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     Andy Jin - Hardware debugging UI improvements, bug 229946
 *     John Dallaway - ST-LINK extension, bug 558266
 *     John Dallaway - Eliminate deprecated API, bug 566462
 *******************************************************************************/
package org.eclipse.cdt.debug.gdbjtag.core.jtagdevice;

import java.util.Collection;

/**
 * @since 9.5
 */
public class STLink extends DefaultGDBJtagConnectionImpl {

	@Override
	public void doDelay(int delay, Collection<String> commands) {
		/* not supported */
	}

	@Override
	public void doHalt(Collection<String> commands) {
		/* not supported */
	}

	@Override
	public void doReset(Collection<String> commands) {
		doResetAndHalt(commands);
		doContinue(commands);
	}

	@Override
	public void doResetAndHalt(Collection<String> commands) {
		addCmd(commands, "monitor reset"); //$NON-NLS-1$
	}

}
