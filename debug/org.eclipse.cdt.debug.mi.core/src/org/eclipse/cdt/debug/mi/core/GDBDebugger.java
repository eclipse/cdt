/*******************************************************************************
 * Copyright (c) 2000, 2004 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v10.html
 * 
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.debug.mi.core;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.eclipse.cdt.debug.core.ICDebugger;
import org.eclipse.cdt.debug.core.cdi.CDIException;
import org.eclipse.cdt.debug.core.cdi.ICDISession;
import org.eclipse.cdt.debug.core.cdi.ICDISharedLibraryManager;
import org.eclipse.cdt.debug.mi.core.cdi.Session;
import org.eclipse.cdt.debug.mi.core.cdi.SharedLibraryManager;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.debug.core.ILaunchConfiguration;

public class GDBDebugger implements ICDebugger {

	protected void initializeLibraries(ILaunchConfiguration config, Session session) throws CDIException {
		try {
			ICDISharedLibraryManager manager = session.getSharedLibraryManager();
			if (manager instanceof SharedLibraryManager) {
				SharedLibraryManager mgr = (SharedLibraryManager)manager;
				boolean autolib = config.getAttribute(IMILaunchConfigurationConstants.ATTR_DEBUGGER_AUTO_SOLIB, IMILaunchConfigurationConstants.DEBUGGER_AUTO_SOLIB_DEFAULT);
				boolean stopOnSolibEvents = config.getAttribute(IMILaunchConfigurationConstants.ATTR_DEBUGGER_STOP_ON_SOLIB_EVENTS, IMILaunchConfigurationConstants.DEBUGGER_STOP_ON_SOLIB_EVENTS_DEFAULT);
				try {
					mgr.setAutoLoadSymbols(autolib);
					mgr.setStopOnSolibEvents(stopOnSolibEvents);
					// The idea is that if the user set autolib, by default
					// we provide with the capability of deferred breakpoints
					// And we set setStopOnSolib events for them(but they should not see those things.
					//
					// If the user explicitly set stopOnSolibEvents well it probably
					// means that they wanted to see those events so do no do deferred breakpoints.
					if (autolib && !stopOnSolibEvents) {
						mgr.setDeferredBreakpoint(true);
						mgr.setStopOnSolibEvents(true);
					}
				} catch (CDIException e) {
					// Ignore this error
					// it seems to be a real problem on many gdb platform
				}
			}
			List p = config.getAttribute(IMILaunchConfigurationConstants.ATTR_DEBUGGER_SOLIB_PATH, Collections.EMPTY_LIST);
			if (p.size() > 0) {
				String[] oldPaths = manager.getSharedLibraryPaths();
				String[] paths = new String[oldPaths.length + p.size()];
				System.arraycopy(p.toArray(new String[p.size()]), 0, paths, 0, p.size());
				System.arraycopy(oldPaths, 0, paths, p.size(), oldPaths.length);
				manager.setSharedLibraryPaths(paths);
			}
		} catch (CoreException e) {
			throw new CDIException(MIPlugin.getResourceString("src.GDBDebugger.Error_initializing_shared_lib_options") + e.getMessage()); //$NON-NLS-1$
		}
	}

	public ICDISession createLaunchSession(ILaunchConfiguration config, IFile exe) throws CDIException {
		Session session = null;
		boolean failed = false;
		try {
			String gdb = config.getAttribute(IMILaunchConfigurationConstants.ATTR_DEBUG_NAME, "gdb"); //$NON-NLS-1$
			File cwd = exe.getProject().getLocation().toFile();
			String gdbinit = config.getAttribute(IMILaunchConfigurationConstants.ATTR_GDB_INIT, ".gdbinit"); //$NON-NLS-1$
			session = (Session)MIPlugin.getDefault().createCSession(gdb, exe.getLocation().toFile(), cwd, gdbinit, null);
			initializeLibraries(config, session);
			return session;
		} catch (IOException e) {
			failed = true;
			throw new CDIException(MIPlugin.getResourceString("src.GDBDebugger.Error_creating_session") + e.getMessage()); //$NON-NLS-1$
		} catch (MIException e) {
			failed = true;
			throw new CDIException(MIPlugin.getResourceString("src.GDBDebugger.Error_creating_session") + e.getMessage()); //$NON-NLS-1$
		} catch (CoreException e) {
			failed = true;
			throw new CDIException(MIPlugin.getResourceString("src.GDBDebugger.Error_creating_session") + e.getMessage()); //$NON-NLS-1$
		} finally {
			if (failed) {
				if (session != null) {
					try {
						session.terminate();
					} catch (Exception ex) {
						// ignore the exception here.
					}
				}
			}
		}
	}

	public ICDISession createAttachSession(ILaunchConfiguration config, IFile exe, int pid) throws CDIException {
		Session session = null;
		boolean failed = false;
		try {
			String gdb = config.getAttribute(IMILaunchConfigurationConstants.ATTR_DEBUG_NAME, "gdb"); //$NON-NLS-1$
			File cwd = exe.getProject().getLocation().toFile();
			String gdbinit = config.getAttribute(IMILaunchConfigurationConstants.ATTR_GDB_INIT, ".gdbinit"); //$NON-NLS-1$
			session = (Session)MIPlugin.getDefault().createCSession(gdb, exe.getLocation().toFile(), pid, null, cwd, gdbinit, null);
			initializeLibraries(config, session);
			return session;
		} catch (IOException e) {
			failed = true;
			throw new CDIException(MIPlugin.getResourceString("src.GDBDebugger.Error_creating_session") + e.getMessage()); //$NON-NLS-1$
		} catch (MIException e) {
			failed = true;
			throw new CDIException(MIPlugin.getResourceString("src.GDBDebugger.Error_creating_session") + e.getMessage()); //$NON-NLS-1$
		} catch (CoreException e) {
			failed = true;
			throw new CDIException(MIPlugin.getResourceString("src.GDBDebugger.Error_creating_session") + e.getMessage()); //$NON-NLS-1$
		} finally {
			if (failed) {
				if (session != null) {
					try {
						session.terminate();
					} catch (Exception ex) {
						// ignore the exception here.
					}
				}
			}
		}
	}

	public ICDISession createCoreSession(ILaunchConfiguration config, IFile exe, IPath corefile) throws CDIException {
		Session session = null;
		boolean failed = false;
		try {
			String gdb = config.getAttribute(IMILaunchConfigurationConstants.ATTR_DEBUG_NAME, "gdb"); //$NON-NLS-1$
			File cwd = exe.getProject().getLocation().toFile();
			String gdbinit = config.getAttribute(IMILaunchConfigurationConstants.ATTR_GDB_INIT, ".gdbinit"); //$NON-NLS-1$
			session = (Session)MIPlugin.getDefault().createCSession(gdb, exe.getLocation().toFile(), corefile.toFile(), cwd, gdbinit, null);
			initializeLibraries(config, session);
			return session;
		} catch (IOException e) {
			failed = true;
			throw new CDIException(MIPlugin.getResourceString("src.GDBDebugger.Error_creating_session") + e.getMessage()); //$NON-NLS-1$
		} catch (MIException e) {
			failed = true;
			throw new CDIException(MIPlugin.getResourceString("src.GDBDebugger.Error_creating_session") + e.getMessage()); //$NON-NLS-1$
		} catch (CoreException e) {
			failed = true;
			throw new CDIException(MIPlugin.getResourceString("src.GDBDebugger.Error_creating_session") + e.getMessage()); //$NON-NLS-1$
		} finally {
			if (failed) {
				if (session != null) {
					try {
						session.terminate();
					} catch (Exception ex) {
						// ignore the exception here.
					}
				}
			}
		}
	}

}
