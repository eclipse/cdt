/*******************************************************************************
 * Copyright (c) 2016 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cdt.dsf.gdb.launching;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.eclipse.cdt.debug.core.CDebugUtils;
import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.IDsfStatusConstants;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitorWithProgress;
import org.eclipse.cdt.dsf.debug.service.IProcesses.IProcessDMContext;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IContainerDMContext;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.internal.GdbPlugin;
import org.eclipse.cdt.dsf.gdb.service.SessionType;
import org.eclipse.cdt.dsf.gdb.service.command.IGDBControl;
import org.eclipse.cdt.dsf.mi.service.IMIContainerDMContext;
import org.eclipse.cdt.dsf.mi.service.IMIProcesses;
import org.eclipse.cdt.dsf.mi.service.command.CommandFactory;
import org.eclipse.cdt.dsf.mi.service.command.events.MIEvent;
import org.eclipse.cdt.dsf.mi.service.command.events.MIThreadCreatedEvent;
import org.eclipse.cdt.dsf.mi.service.command.events.MIThreadGroupCreatedEvent;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;
import org.eclipse.cdt.dsf.mi.service.command.output.MIListThreadGroupsInfo;
import org.eclipse.cdt.dsf.mi.service.command.output.MIListThreadGroupsInfo.IThreadGroupInfo;
import org.eclipse.cdt.dsf.mi.service.command.output.MIListThreadGroupsInfo.IThreadGroupInfo2;
import org.eclipse.cdt.dsf.mi.service.command.output.MIThread;
import org.eclipse.cdt.dsf.mi.service.command.output.MIThreadInfoInfo;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;

/**
 * Subclass for GDB >= 7.12.
 * 
 * @since 5.2
 */
public class FinalLaunchSequence_7_12 extends FinalLaunchSequence_7_7 {
	private IGDBControl fCommandControl;
	private CommandFactory fCommandFactory;
	private Map<String, Object> fAttributes;

	public FinalLaunchSequence_7_12(DsfSession session, Map<String, Object> attributes,
			RequestMonitorWithProgress rm) {
		super(session, attributes, rm);
		fAttributes = attributes;
	}

	@Override
	protected String[] getExecutionOrder(String group) {
		if (GROUP_TOP_LEVEL.equals(group)) {
			// Initialize the list with the base class' steps
			// We need to create a list that we can modify, which is why we create our own ArrayList.
			List<String> orderList = new ArrayList<String>(
					Arrays.asList(super.getExecutionOrder(GROUP_TOP_LEVEL)));

			// Now insert our steps right after the initialization of the base class.
			orderList.add(orderList.indexOf("stepInitializeFinalLaunchSequence_7_7") + 1, //$NON-NLS-1$
					"stepInitializeFinalLaunchSequence_7_12"); //$NON-NLS-1$

			orderList.add(orderList.indexOf("stepSourceGDBInitFile") + 1, //$NON-NLS-1$
					"stepSetTargetAsync"); //$NON-NLS-1$
			
			orderList.add(orderList.indexOf("stepSetTargetAsync") + 1, //$NON-NLS-1$
					"stepSetRecordFullStopAtLimit"); //$NON-NLS-1$
			
			
			
			
//			orderList.add(orderList.indexOf("stepSetTargetAsync") + 2, //$NON-NLS-1$
//					"stepSyncToExistingGDBSession"); //$NON-NLS-1$
			
			orderList.add(orderList.indexOf("stepSetTargetAsync") + 2, //$NON-NLS-1$
					"stepSyncToExistingGDBSessionUsingListThreadGroupRecursive"); //$NON-NLS-1$
			// stepSyncToExistingGDBSessionUsingListThreadGroupRecursive
			

			return orderList.toArray(new String[orderList.size()]);
		}

		return null;
	}

	/**
	 * Initialize the members of the FinalLaunchSequence_7_12 class. This step is mandatory for the rest of
	 * the sequence to complete.
	 */
	@Execute
	public void stepInitializeFinalLaunchSequence_7_12(RequestMonitor rm) {
		DsfServicesTracker tracker = new DsfServicesTracker(GdbPlugin.getBundleContext(),
				getSession().getId());
		fCommandControl = tracker.getService(IGDBControl.class);
		tracker.dispose();

		if (fCommandControl == null) {
			rm.done(new Status(IStatus.ERROR, GdbPlugin.PLUGIN_ID, IDsfStatusConstants.INTERNAL_ERROR,
					"Cannot obtain service", null)); //$NON-NLS-1$
			return;
		}

		fCommandFactory = fCommandControl.getCommandFactory();

		rm.done();

	}

	@Execute
	public void stepSetTargetAsync(RequestMonitor requestMonitor) {
		if (fGDBBackend.getSessionType() == SessionType.EXISTING) {
			requestMonitor.done();
			return;
		}
		
		// Use target async when interfacing with GDB 7.12 or higher
		// this will allow us to use the new enhanced GDB Full CLI console
		fCommandControl.queueCommand(
			fCommandFactory.createMIGDBSetTargetAsync(fCommandControl.getContext(), true),
			new DataRequestMonitor<MIInfo>(getExecutor(), requestMonitor) {
				@Override
				protected void handleError() {
					// We should only be calling this for GDB >= 7.12,
					// but just in case, accept errors for older GDBs
					requestMonitor.done();
				}
			});
	}
	
	/**
	 * Set reverse debugging record full stop-at-limit to off, so GDB does not halt waiting for user input
	 * when the recording buffer gets full
	 * @param requestMonitor
	 */
	@Execute
	public void stepSetRecordFullStopAtLimit(RequestMonitor requestMonitor) {
		if (fGDBBackend.getSessionType() == SessionType.EXISTING) {
			requestMonitor.done();
			return;
		}
		
		fCommandControl.queueCommand(
			fCommandFactory.createMIGDBSetRecordFullStopAtLimit(fCommandControl.getContext(), false),
			new DataRequestMonitor<MIInfo>(getExecutor(), requestMonitor) {
				@Override
				protected void handleError() {
					// Accept errors since this is not essential
					requestMonitor.done();
				}
			});
	}

	@Override
	@Execute
	public void stepSetNonStop(final RequestMonitor requestMonitor) {
		boolean isNonStop = CDebugUtils.getAttribute(fAttributes,
				IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_NON_STOP,
				LaunchUtils.getIsNonStopModeDefault());
		
		if (fGDBBackend.getSessionType() == SessionType.EXISTING) {
			requestMonitor.done();
			return;
		}
		
		if (isNonStop) {
			// GDBs that don't support non-stop don't allow you to set it to false.
			// We really should set it to false when GDB supports it though.
			// Something to fix later.
			// Note that disabling pagination is taken care of elsewhere
			fCommandControl.queueCommand(
					fCommandFactory.createMIGDBSetNonStop(fCommandControl.getContext(), true),
					new DataRequestMonitor<MIInfo>(getExecutor(), requestMonitor));
		} else {
			requestMonitor.done();
		}
	}	
	
	
	//createMIListThreadGroups(ICommandControlDMContext ctx, boolean listAll, boolean recurse) {
	/**
	 * @since 5.3
	 */
	@Execute
	public void stepSyncToExistingGDBSessionUsingListThreadGroupRecursive(RequestMonitor requestMonitor) {
		if (fGDBBackend.getSessionType() != SessionType.EXISTING) {
			requestMonitor.done();
			return;
		}
		IMIProcesses proc = fTracker.getService(IMIProcesses.class);
		IGDBControl control = fTracker.getService(IGDBControl.class);
		
		DataRequestMonitor<MIListThreadGroupsInfo> rm = 
				new DataRequestMonitor<MIListThreadGroupsInfo>(getExecutor(), requestMonitor) {
			@Override
			protected void handleSuccess() {
				MIListThreadGroupsInfo info = getData();
				IThreadGroupInfo[] groups = info.getGroupList();
				
				for (IThreadGroupInfo group : groups) {
					final String procPid = group.getPid();
					final String groupId = group.getGroupId();
					Path execPath = new Path(group.getExecutable());
					// "create" process
					proc.createProcess(groupId, procPid, execPath.lastSegment());
					
					// send MIThreadGroupCreatedEvent for each process
					if (procPid != null) {
						final IProcessDMContext procDmc = proc.createProcessContext(control.getContext(), procPid);
						System.out.println("MIThreadGroupCreatedEvent (procDMC=:" + procDmc + ")"); //$NON-NLS-1$ //$NON-NLS-2$
						MIEvent<?> event =  new MIThreadGroupCreatedEvent(procDmc, 0, groupId);
						getSession().dispatchEvent(event, null);
						
						// threads under this process
						if (group instanceof IThreadGroupInfo2) {
							IThreadGroupInfo2 group2 = (IThreadGroupInfo2) group;
							MIThread[] threads = group2.getThreads();
							for (MIThread t : threads) {
								// "create" thread
								proc.createThread(t.getThreadId(), groupId);
								
								// send MIThreadCreatedEvent
								IMIContainerDMContext containerCtx =  proc.createContainerContext(procDmc, groupId);
								
								IContainerDMContext cont = null;
								MIEvent<?> event2 =  new MIThreadCreatedEvent(containerCtx, t.getThreadId());
								getSession().dispatchEvent(event2, null);
								
							}
							
						}
					}
					
					requestMonitor.done();
				}
			}
		};
		
		// use recursive version -list-thread-group, since it helpfully groups threads under their inferior 
		fCommandControl.queueCommand(fCommandFactory.createMIListThreadGroups(fCommandControl.getContext(), false, true),rm);
		
	}
	
	/**
	 * Since we're connecting to an existing GDB session, we missed important event, 
	 * and are in an unknown state (which procs being debugged, what are their threads, etc)
	 * So attempt to have strategic CDT services sync'ed to GDB's state
	 * @since 5.3
	 */
	@Execute
	public void stepSyncToExistingGDBSession(RequestMonitor requestMonitor) {
		if (fGDBBackend.getSessionType() != SessionType.EXISTING) {
			requestMonitor.done();
			return;
		}
		
		IMIProcesses proc = fTracker.getService(IMIProcesses.class);
		IGDBControl control = fTracker.getService(IGDBControl.class);
		
		String groupId = "i1"; //$NON-NLS-1$

		DataRequestMonitor<MIListThreadGroupsInfo> rm = 
				new DataRequestMonitor<MIListThreadGroupsInfo>(getExecutor(), requestMonitor) {
			@Override
			protected void handleSuccess() {
				MIListThreadGroupsInfo info = getData();
				IThreadGroupInfo[] groups = info.getGroupList();
				for (IThreadGroupInfo group : groups) {
					// create processes
					final String procPid = group.getPid();
					Path execPath = new Path(group.getExecutable());
					
					proc.createProcess(group.getGroupId(), procPid, execPath.lastSegment());

					// send MIThreadGroupCreatedEvent
					if (procPid != null) {
						IProcessDMContext procDmc = proc.createProcessContext(control.getContext(), procPid);
						System.out.println("MIThreadGroupCreatedEvent (procDMC=:" + procDmc + ")"); //$NON-NLS-1$ //$NON-NLS-2$
						MIEvent<?> event =  new MIThreadGroupCreatedEvent(procDmc, 0, groupId);
						getSession().dispatchEvent(event, null);
					}

				}
				doThreads(requestMonitor);
			}
		};

		fCommandControl.queueCommand(fCommandFactory.createMIListThreadGroups(fCommandControl.getContext()),rm);

	}
	
	private void doThreads(RequestMonitor rm) {
		IMIProcesses proc = fTracker.getService(IMIProcesses.class);
		
		fCommandControl.queueCommand(
				fCommandFactory.createMIThreadInfo(fCommandControl.getContext()), 
				new DataRequestMonitor<MIThreadInfoInfo>(getExecutor(), rm) {
					@Override
					protected void handleSuccess() {
						MIThreadInfoInfo info = getData();

						for (MIThread thread : info.getThreadList()) {
							// TODO: replace hardcoded groupId with group from improved -thread-info (Simon's patch)
							proc.createThread(thread.getThreadId(), "i1"); //$NON-NLS-1$
							
							// send MIThreadCreatedEvent
//							MIEvent<?> event =  new MIThreadCreatedEvent(IContainerDMContext ctx, int id);
//							getSession().dispatchEvent(event, null);
							
						}
						System.out.println("****  " + info.toString()); //$NON-NLS-1$
						rm.done();
					}
				});
	}
	
	/*
	proc.getProcessesBeingDebugged(control.getContext(), new DataRequestMonitor<IDMContext[]> (getExecutor(), rm) {
		@Override
		public void handleSuccess() {
			IDMContext[] ctxs = getData();
			for (IDMContext c : ctxs) {
				System.out.println("*** container context: " + c.toString()); //$NON-NLS-1$
				
				proc.getProcessesBeingDebugged(c, new DataRequestMonitor<IDMContext[]> (getExecutor(), rm) {
					@Override
					public void handleSuccess() {
						IDMContext[] ctxs = getData();
						for (IDMContext c : ctxs) {
							System.out.println("*** thread context: " + c.toString()); //$NON-NLS-1$
							
						}
					}
				});
				
				
			}
		}
	}); */
	
}
