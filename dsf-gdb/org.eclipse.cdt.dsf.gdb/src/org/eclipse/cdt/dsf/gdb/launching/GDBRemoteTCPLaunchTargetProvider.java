/*******************************************************************************
 * Copyright (c) 2019 QNX Software Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.cdt.dsf.gdb.launching;

import org.eclipse.launchbar.core.target.ILaunchTarget;
import org.eclipse.launchbar.core.target.ILaunchTargetManager;
import org.eclipse.launchbar.core.target.ILaunchTargetProvider;
import org.eclipse.launchbar.core.target.TargetStatus;

/**
 * @since 5.7
 */
public class GDBRemoteTCPLaunchTargetProvider implements ILaunchTargetProvider {

	public static final String TYPE_ID = "org.eclipse.cdt.dsf.gdb.remoteTCPLaunchTargetType"; //$NON-NLS-1$

	@Override
	public void init(ILaunchTargetManager targetManager) {
		// There are no autodiscovered launches for this type.
	}

	@Override
	public TargetStatus getStatus(ILaunchTarget target) {
		return TargetStatus.OK_STATUS;
	}

}
