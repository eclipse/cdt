/*******************************************************************************
 * Copyright (c) 2015 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cdt.dsf.gdb.service.extensions;

import org.eclipse.cdt.dsf.gdb.service.GDBBreakpointsManager_7_2;
import org.eclipse.cdt.dsf.mi.service.MIBreakpointsManager;
import org.eclipse.cdt.dsf.service.DsfSession;

/**
 * Top-level class in the version hierarchy of implementations of {@link MIBreakpointsManager}.
 * <br> 
 * Extenders should subclass this class for their special needs, which will allow
 * them to always extend the most recent version of the service.
 * For example, if GDB<Service>_7_9 is added, this GDB<Service>_HEAD class
 * will be changed to extend it instead of the previous version, therefore
 * automatically allowing extenders to be extending the new class.
 * 
 * @since 4.8
 */
public class GDBBreakpointsManager_HEAD extends GDBBreakpointsManager_7_2 {
	public GDBBreakpointsManager_HEAD(DsfSession session, String debugModelId) {
		super(session, debugModelId);
	}
}
