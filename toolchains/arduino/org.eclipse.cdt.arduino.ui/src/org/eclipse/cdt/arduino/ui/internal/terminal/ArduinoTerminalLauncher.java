/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.cdt.arduino.ui.internal.terminal;

import org.eclipse.tm.terminal.connector.cdtserial.launcher.SerialLauncherDelegate;
import org.eclipse.tm.terminal.view.ui.interfaces.IConfigurationPanel;
import org.eclipse.tm.terminal.view.ui.interfaces.IConfigurationPanelContainer;
import org.eclipse.tm.terminal.view.ui.interfaces.ILauncherDelegate;

public class ArduinoTerminalLauncher extends SerialLauncherDelegate implements ILauncherDelegate {

	@Override
	public IConfigurationPanel getPanel(IConfigurationPanelContainer container) {
		return new ArduinoTerminalConfigPanel(container);
	}

}
