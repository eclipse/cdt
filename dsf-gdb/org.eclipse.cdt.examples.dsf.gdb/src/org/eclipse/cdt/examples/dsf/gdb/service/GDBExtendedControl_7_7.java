/*******************************************************************************
 * Copyright (c) 2014 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Marc Khouzam (Ericsson) - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.examples.dsf.gdb.service;

import java.util.Map;

import org.eclipse.cdt.dsf.concurrent.RequestMonitorWithProgress;
import org.eclipse.cdt.dsf.concurrent.Sequence;
import org.eclipse.cdt.dsf.gdb.service.command.GDBControl_7_7;
import org.eclipse.cdt.dsf.mi.service.command.CommandFactory;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.cdt.examples.dsf.gdb.launch.GdbExtendedFinalLaunchSequence_7_7;
import org.eclipse.debug.core.ILaunchConfiguration;

public class GDBExtendedControl_7_7 extends GDBControl_7_7 {
    public GDBExtendedControl_7_7(DsfSession session, ILaunchConfiguration config, CommandFactory factory) {
    	super(session, config, factory);
    }

    @Override
	protected Sequence getCompleteInitializationSequence(Map<String, Object> attributes, RequestMonitorWithProgress rm) {
		return new GdbExtendedFinalLaunchSequence_7_7(getSession(), attributes, rm);
	}
}
