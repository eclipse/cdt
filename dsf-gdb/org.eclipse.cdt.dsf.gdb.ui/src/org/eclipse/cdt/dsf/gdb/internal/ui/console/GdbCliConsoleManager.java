/*******************************************************************************
 * Copyright (c) 2016, 2017 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cdt.dsf.gdb.internal.ui.console;

import java.util.concurrent.RejectedExecutionException;

import org.eclipse.cdt.debug.ui.CDebugUIPlugin;
import org.eclipse.cdt.debug.ui.debuggerconsole.IDebuggerConsole;
import org.eclipse.cdt.debug.ui.debuggerconsole.IDebuggerConsoleManager;
import org.eclipse.cdt.dsf.concurrent.ConfinedToDsfExecutor;
import org.eclipse.cdt.dsf.concurrent.DsfRunnable;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService.ICommandControlInitializedDMEvent;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService.ICommandControlShutdownDMEvent;
import org.eclipse.cdt.dsf.gdb.internal.ui.GdbUIPlugin;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.cdt.dsf.gdb.service.IGDBBackend;
import org.eclipse.cdt.dsf.gdb.service.command.IGDBControl;
import org.eclipse.cdt.dsf.mi.service.IMIBackend;
import org.eclipse.cdt.dsf.service.DsfServiceEventHandler;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchesListener2;

/**
 * A console manager for GDB sessions which adds and removes
 * gdb cli consoles.
 * 
 * There is a single such console per debug session.
 * This console interacts directly with the GDB process using
 * the standard GDB CLI interface.
 * These consoles cannot be enabled/disabled by the user.
 * However, they are only supported by GDB >= 7.12;
 * to handle this limitation, the console manager will use the DSF Backend
 * service to establish if it should start a gdb cli console or not.
 */
public class GdbCliConsoleManager implements ILaunchesListener2 {

	/**
	 * Start the tracing console.  We don't do this in a constructor, because
	 * we need to use <code>this</code>.
	 */
	public void startup() {
		// Listen to launch events
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener(this);
	}

	public void shutdown() {
		DebugPlugin.getDefault().getLaunchManager().removeLaunchListener(this);
	}

    @Override
	public void launchesAdded(ILaunch[] launches) {
		for (ILaunch launch : launches) {
			handleConsoleForLaunch(launch);
		}
	}

    @Override
	public void launchesChanged(ILaunch[] launches) {
	}

    @Override
	public void launchesRemoved(ILaunch[] launches) {
		for (ILaunch launch : launches) {
			removeConsole(launch);
		}
	}
	
    @Override
	public void launchesTerminated(ILaunch[] launches) {
		for (ILaunch launch : launches) {
			renameConsole(launch);
		}
	}
	
	protected void handleConsoleForLaunch(ILaunch launch) {
		// Full CLI GDB consoles are only added for GdbLaunches
		if (launch instanceof GdbLaunch) {
			new GdbConsoleCreator((GdbLaunch)launch).init();
		}
	}

	protected void removeConsole(ILaunch launch) {
		IDebuggerConsole console = getConsole(launch);
		if (console != null) {
			removeConsole(console);
		}
	}

	private void renameConsole(ILaunch launch) {
		IDebuggerConsole console = getConsole(launch);
		if (console != null) {
			console.resetName();
		}		
	}

	private IDebuggerConsole getConsole(ILaunch launch) {
		IDebuggerConsoleManager manager = CDebugUIPlugin.getDebuggerConsoleManager(); 
		for (IDebuggerConsole console : manager.getConsoles()) {
			if (console.getLaunch().equals(launch)) {
				return console;
			}
		}
		return null;
	}

	private void addConsole(IDebuggerConsole console) {
		getDebuggerConsoleManager().addConsole(console);
	}
	
	private void removeConsole(IDebuggerConsole console) {
		getDebuggerConsoleManager().removeConsole(console);
	}
	
	private IDebuggerConsoleManager getDebuggerConsoleManager() {
		return CDebugUIPlugin.getDebuggerConsoleManager();
	}
	
	/**
	 * Class that determines what type of console should be created
	 * for this particular Gdblaunch.  It figures this out by asking the
	 * Backend service.  It then either creates a GdbFullCliConsole or
	 * a GdbBasicCliConsole.
	 */
	private class GdbConsoleCreator {
		private GdbLaunch fLaunch;
		private DsfSession fSession;
		
		public GdbConsoleCreator(GdbLaunch launch) {
			fLaunch = launch;
			fSession = launch.getSession();
		}
		
		public void init() {
			try {
				fSession.getExecutor().submit(new DsfRunnable() {
		        	@Override
		        	public void run() {
		        		// Look for backend service right away.  It probably 
		        		// won't be available yet but we must make sure.
		            	DsfServicesTracker tracker = new DsfServicesTracker(GdbUIPlugin.getBundleContext(), fSession.getId());
		            	// Use the lowest level service name in case those are created but don't implement
		            	// the most specialized classes IGDBControl or IGDBBackend.
		            	ICommandControlService control = tracker.getService(ICommandControlService.class);
		            	IMIBackend backend = tracker.getService(IMIBackend.class);
		            	tracker.dispose();
		            	
		            	// If we use the old console we not only need the backend service but
		            	// also the control service.  For simplicity, always wait for both.
		            	if (backend != null && control != null) {
		            		// Backend and Control services already available, we can start!
		            		verifyAndCreateCliConsole();
		            	}

		            	// Listen for the start and termination of the control service, either to create or delete the
		            	// corresponding console
		            	new GdbServiceLifeCycleListener(GdbConsoleCreator.this, fSession);
		        	}
		        });
			} catch (RejectedExecutionException e) {
			}
		}
		
		@ConfinedToDsfExecutor("fSession.getExecutor()")
		private void verifyAndCreateCliConsole() {
			String gdbVersion;
			try {
				gdbVersion = fLaunch.getGDBVersion();
			} catch (CoreException e) {
				gdbVersion = "???"; //$NON-NLS-1$
				assert false : "Should not happen since the gdb version is cached"; //$NON-NLS-1$
			}
			String consoleTitle = fLaunch.getGDBPath().toOSString().trim() + " (" + gdbVersion +")"; //$NON-NLS-1$ //$NON-NLS-2$

        	DsfServicesTracker tracker = new DsfServicesTracker(GdbUIPlugin.getBundleContext(), fSession.getId());
        	IGDBControl control = tracker.getService(IGDBControl.class);
        	IGDBBackend backend = tracker.getService(IGDBBackend.class);
        	tracker.dispose();

        	IDebuggerConsole console;
			if (backend != null && backend.isFullGdbConsoleSupported()) {
				// Create a full GDB cli console
				console = new GdbFullCliConsole(fLaunch, consoleTitle, backend.getProcess(), backend.getProcessPty());
			} else if (control != null) {
				// Create a simple text console for the cli.
				console = new GdbBasicCliConsole(fLaunch, consoleTitle, control.getGDBBackendProcess());
			} else {
				// Something is wrong.  Don't create a console
				assert false;
				return;
			}
			
			addConsole(console);
			// Make sure the Debugger Console view is visible but do not force it to the top
			getDebuggerConsoleManager().openConsoleView();
		}

		private void deleteConsole() {
			removeConsole(fLaunch);
		}
	}
	
	/**
	 * Class used to listen for started and terminated events for the services we need.
	 * This class must be public to receive the event.
	 */
	public class GdbServiceLifeCycleListener {
		private DsfSession fSession;
		private GdbConsoleCreator fCreator;
		
		public GdbServiceLifeCycleListener(GdbConsoleCreator creator, DsfSession session) {
	 		fCreator = creator;
			fSession = session;
			fSession.addServiceEventListener(this, null);
		}
		
		@DsfServiceEventHandler
		public final void eventDispatched(ICommandControlInitializedDMEvent event) {
			// With the commandControl service started, we know the backend service
			// is also started.  So we are good to go.
			fCreator.verifyAndCreateCliConsole();
		}

		@DsfServiceEventHandler
		public final void eventDispatched(ICommandControlShutdownDMEvent event) {
			// Time to terminate the CLI console
			fCreator.deleteConsole();
			// No longer need to receive events.
			fSession.removeServiceEventListener(this);
		}
	}
}
