/*******************************************************************************
 * Copyright (c) 2015 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cdt.dsf.gdb.service.extensions;

import org.eclipse.cdt.dsf.debug.service.IRunControl;
import org.eclipse.cdt.dsf.gdb.service.GDBRunControl_7_2_NS;
import org.eclipse.cdt.dsf.service.DsfSession;

/**
 * Top-level class in the version hierarchy of implementations of {@link IRunControl}
 * in Non-Stop.
 * <br> 
 * Extenders should subclass this class for their special needs, which will allow
 * them to always extend the most recent version of the service.
 * For example, if GDB<Service>_7_9 is added, this GDB<Service>_HEAD class
 * will be changed to extend it instead of the previous version, therefore
 * automatically allowing extenders to be extending the new class.
 * 
 * @since 4.8
 */
public class GDBRunControl_NS_HEAD extends GDBRunControl_7_2_NS {
	public GDBRunControl_NS_HEAD(DsfSession session) {
		super(session);
	}
}
