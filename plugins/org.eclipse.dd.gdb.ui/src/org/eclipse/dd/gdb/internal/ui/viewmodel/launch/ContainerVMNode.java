/*******************************************************************************
 * Copyright (c) 2006 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Ericsson			  - Initial API and implementation
 *******************************************************************************/

package org.eclipse.dd.gdb.internal.ui.viewmodel.launch;


import java.util.concurrent.RejectedExecutionException;

import org.eclipse.dd.dsf.concurrent.DsfRunnable;
import org.eclipse.dd.dsf.concurrent.RequestMonitor;
import org.eclipse.dd.dsf.datamodel.DMContexts;
import org.eclipse.dd.dsf.datamodel.IDMEvent;
import org.eclipse.dd.dsf.debug.service.IRunControl;
import org.eclipse.dd.dsf.debug.service.IRunControl.IContainerDMContext;
import org.eclipse.dd.dsf.debug.service.IRunControl.IExitedDMEvent;
import org.eclipse.dd.dsf.debug.service.IRunControl.IStartedDMEvent;
import org.eclipse.dd.dsf.service.DsfSession;
import org.eclipse.dd.dsf.ui.concurrent.ViewerDataRequestMonitor;
import org.eclipse.dd.dsf.ui.viewmodel.VMDelta;
import org.eclipse.dd.dsf.ui.viewmodel.datamodel.AbstractDMVMNode;
import org.eclipse.dd.dsf.ui.viewmodel.datamodel.AbstractDMVMProvider;
import org.eclipse.dd.gdb.internal.provisional.service.GDBRunControl;
import org.eclipse.dd.gdb.internal.provisional.service.GDBRunControl.GDBProcessData;
import org.eclipse.dd.gdb.internal.provisional.service.command.GDBControl;
import org.eclipse.dd.gdb.internal.provisional.service.command.GDBControlDMContext;
import org.eclipse.dd.gdb.internal.provisional.service.command.GDBControl.GDBStartedEvent;
import org.eclipse.dd.mi.service.command.MIInferiorProcess;
import org.eclipse.dd.mi.service.command.MIInferiorProcess.InferiorExitedDMEvent;
import org.eclipse.dd.mi.service.command.MIInferiorProcess.InferiorStartedDMEvent;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IChildrenUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IElementLabelProvider;
import org.eclipse.debug.internal.ui.viewers.model.provisional.ILabelUpdate;
import org.eclipse.debug.internal.ui.viewers.model.provisional.IModelDelta;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.IDebugUIConstants;


@SuppressWarnings("restriction")
public class ContainerVMNode extends AbstractDMVMNode 
    implements IElementLabelProvider
{

    
	public ContainerVMNode(AbstractDMVMProvider provider, DsfSession session) {
        super(provider, session, IRunControl.IExecutionDMContext.class);
	}

	@Override
	protected void updateElementsInSessionThread(IChildrenUpdate update) {
      GDBControl controlService = getServicesTracker().getService(GDBControl.class);
      if ( controlService == null ) {
              handleFailedUpdate(update);
              return;
      }
      
      MIInferiorProcess inferiorProcess = controlService.getInferiorProcess();
      if (inferiorProcess != null && inferiorProcess.getState() != MIInferiorProcess.State.TERMINATED) {
          update.setChild(createVMContext(inferiorProcess.getExecutionContext()), 0);
      }
      update.done();
	}

	
    public void update(final ILabelUpdate[] updates) {
        try {
            getSession().getExecutor().execute(new DsfRunnable() {
                public void run() {
                    updateLabelInSessionThread(updates);
                }});
        } catch (RejectedExecutionException e) {
            for (ILabelUpdate update : updates) {
                handleFailedUpdate(update);
            }
        }
    }
	
	protected void updateLabelInSessionThread(ILabelUpdate[] updates) {
        for (final ILabelUpdate update : updates) {
        	final GDBRunControl runControl = getServicesTracker().getService(GDBRunControl.class);
            if ( runControl == null ) {
                handleFailedUpdate(update);
                continue;
            }
            
            final GDBControlDMContext dmc = findDmcInPath(update.getViewerInput(), update.getElementPath(), GDBControlDMContext.class);

            String imageKey = null;
            if (runControl.isSuspended(dmc)) {
                imageKey = IDebugUIConstants.IMG_OBJS_THREAD_SUSPENDED;
            } else {
                imageKey = IDebugUIConstants.IMG_OBJS_THREAD_RUNNING;
            }
            update.setImageDescriptor(DebugUITools.getImageDescriptor(imageKey), 0);
            
            runControl.getProcessData(
                dmc, 
                new ViewerDataRequestMonitor<GDBProcessData>(getExecutor(), update) { 
					@Override
                    public void handleCompleted() {
                        if (!isSuccess()) {
                            update.done();
                            return;
                        }
                        update.setLabel(getData().getName(), 0);
                        update.done();
                    }
                });
        }
    }

    public int getDeltaFlags(Object e) {
        if(e instanceof IRunControl.IContainerResumedDMEvent || 
           e instanceof IRunControl.IContainerSuspendedDMEvent) 
        {
            return IModelDelta.CONTENT;
        } else if (e instanceof GDBControl.GDBExitedEvent || e instanceof InferiorExitedDMEvent) {
            return IModelDelta.CONTENT;
        } else if (e instanceof GDBStartedEvent) {
            return IModelDelta.EXPAND;
        } else if (e instanceof InferiorStartedDMEvent) {
            return IModelDelta.EXPAND | IModelDelta.SELECT;            
        } if(e instanceof IStartedDMEvent || e instanceof IExitedDMEvent) {
            return IModelDelta.CONTENT;
        }
        return IModelDelta.NO_CHANGE;
    }

    public void buildDelta(Object e, final VMDelta parentDelta, final int nodeOffset, final RequestMonitor requestMonitor) {
    	if(e instanceof IRunControl.IContainerResumedDMEvent || 
    	   e instanceof IRunControl.IContainerSuspendedDMEvent) 
    	{
            parentDelta.addNode(createVMContext(((IDMEvent<?>)e).getDMContext()), IModelDelta.CONTENT);
        } else if (e instanceof GDBControl.GDBExitedEvent || e instanceof InferiorExitedDMEvent) {
            // Note: we must process the inferior started/exited events before the thread's 
            // started/exited events otherwise the inferior's handlers would never be called.
            parentDelta.setFlags(parentDelta.getFlags() |  IModelDelta.CONTENT);
        } else if (e instanceof GDBStartedEvent) {
            parentDelta.addNode(createVMContext(((IDMEvent<?>)e).getDMContext()), IModelDelta.EXPAND);
        } else if (e instanceof InferiorStartedDMEvent) {
            parentDelta.addNode(createVMContext(((IDMEvent<?>)e).getDMContext()), IModelDelta.EXPAND | IModelDelta.SELECT);
        } else if (e instanceof IStartedDMEvent || e instanceof IExitedDMEvent) {
            IContainerDMContext containerCtx = DMContexts.getAncestorOfType(
                ((IDMEvent<?>)e).getDMContext(), IContainerDMContext.class);
            if (containerCtx != null) {
                parentDelta.addNode(createVMContext(containerCtx), IModelDelta.CONTENT);
            }
        }

    	requestMonitor.done();
  	 }
}
