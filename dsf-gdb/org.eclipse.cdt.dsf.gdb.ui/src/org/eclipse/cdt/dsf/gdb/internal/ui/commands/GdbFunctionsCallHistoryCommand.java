/*******************************************************************************
 * Copyright (c) 2021 Trande UG.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Trande UG - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.dsf.gdb.internal.ui.commands;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.RejectedExecutionException;

import org.eclipse.cdt.debug.core.model.IFunctionsCallHistoryHandler;
import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.DsfExecutor;
import org.eclipse.cdt.dsf.concurrent.Query;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IExecutionDMContext;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService.ICommandControlDMContext;
import org.eclipse.cdt.dsf.gdb.internal.ui.GdbUIPlugin;
import org.eclipse.cdt.dsf.gdb.service.IReverseRunControl;
import org.eclipse.cdt.dsf.gdb.service.IReverseRunControl3;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.cdt.dsf.ui.viewmodel.datamodel.IDMVMContext;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.IRequest;
import org.eclipse.debug.core.commands.AbstractDebugCommand;
import org.eclipse.debug.core.commands.IDebugCommandRequest;
import org.eclipse.debug.core.commands.IEnabledStateRequest;

/**
 * Command performing a reverse resume.
 *
 * @since 2.1
 */
public class GdbFunctionsCallHistoryCommand extends AbstractDebugCommand implements IFunctionsCallHistoryHandler {

	private final DsfExecutor fExecutor;
	private final DsfServicesTracker fTracker;

	public GdbFunctionsCallHistoryCommand(DsfSession session) {
		fExecutor = session.getExecutor();
		fTracker = new DsfServicesTracker(GdbUIPlugin.getBundleContext(), session.getId());
	}

	public void dispose() {
		fTracker.dispose();
	}

	@Override
	protected void doExecute(Object[] targets, IProgressMonitor monitor, IRequest request) throws CoreException {
		if (targets.length != 1) {
			return;
		}

		final ICommandControlDMContext dmc = DMContexts.getAncestorOfType(((IDMVMContext) targets[0]).getDMContext(),
				ICommandControlDMContext.class);
		if (dmc == null) {
			return;
		}

		Query<Object> functionsCallHistory = new Query<>() {
			@Override
			public void execute(DataRequestMonitor<Object> rm) {
				IReverseRunControl3 runControl = fTracker.getService(IReverseRunControl3.class);

				if (runControl != null) {
					runControl.functionsCallHistory(dmc, rm);
				} else {
					rm.done();
				}
			}
		};
		try {
			fExecutor.execute(functionsCallHistory);
			Object t = functionsCallHistory.get();
		} catch (InterruptedException e) {
			e.printStackTrace();
		} catch (ExecutionException e) {
			e.printStackTrace();
		} catch (RejectedExecutionException e) {
			e.printStackTrace();
			// Can be thrown if the session is shutdown
		}
	}

	@Override
	protected boolean isExecutable(Object[] targets, IProgressMonitor monitor, IEnabledStateRequest request)
			throws CoreException {
		if (targets.length != 1) {
			return false;
		}

		final IExecutionDMContext dmc = DMContexts.getAncestorOfType(((IDMVMContext) targets[0]).getDMContext(),
				IExecutionDMContext.class);
		if (dmc == null) {
			return false;
		}

		Query<Boolean> canReverseResume = new Query<>() {
			@Override
			public void execute(DataRequestMonitor<Boolean> rm) {
				IReverseRunControl runControl = fTracker.getService(IReverseRunControl.class);
				if (runControl != null) {
					runControl.canReverseResume(dmc, rm);
				} else {
					rm.setData(false);
					rm.done();
				}
			}
		};
		try {
			fExecutor.execute(canReverseResume);
			return canReverseResume.get();
		} catch (InterruptedException e) {
		} catch (ExecutionException e) {
		} catch (RejectedExecutionException e) {
			// Can be thrown if the session is shutdown
		}
		return false;
	}

	@Override
	protected Object getTarget(Object element) {
		if (element instanceof IDMVMContext) {
			return element;
		}
		return null;
	}

	@Override
	protected boolean isRemainEnabled(IDebugCommandRequest request) {
		return false;
	}
}