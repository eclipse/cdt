/*******************************************************************************
 * Copyright (c) 2015 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marc Dumais (Ericsson) - Initial API and implementation (Bug 460476)
 *******************************************************************************/

package org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.actions;

import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.ui.view.MulticoreVisualizer;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.utils.PersistentSettingsManager;
import org.eclipse.cdt.dsf.gdb.multicorevisualizer.internal.utils.PersistentSettingsManager.PersistentParameter;
import org.eclipse.cdt.visualizer.ui.VisualizerAction;
import org.eclipse.jface.action.Action;

/** Actions that shows/hides the debug actions on the Multicore Visualizer toolbar */
public class ShowDebugToolbarAction extends VisualizerAction {
	// --- members ---

	/** Visualizer instance we're associated with. */
	MulticoreVisualizer m_visualizer = null;
	
	/** persistent settings manager */
	protected PersistentSettingsManager m_persistentSettingsManager;
	
	/** Persistent parameter that remembers if the debug actions should be shown or not */
	protected PersistentParameter<Boolean> m_showDebugActions;

	// --- constructors/destructors ---

	/** Constructor.
	 * @param  showDebugActions : show the debug actions by default
	 * @param MVInstanceId : id that uniquely identifies a Multicore Visualizer instance
	 */
	public ShowDebugToolbarAction(boolean showDebugActions, String MVInstanceId)
	{
		super("Show Debug Toolbar", Action.AS_CHECK_BOX); //$NON-NLS-1$
		
		m_persistentSettingsManager = new PersistentSettingsManager(MVInstanceId);
		
		m_showDebugActions =  m_persistentSettingsManager.getNewParameter(Boolean.class, 
				"showDebugActionsInMVToolbar", true, showDebugActions); //$NON-NLS-1$
		
		this.setChecked(m_showDebugActions.value());
		setText("Show Debug Toolbar"); //$NON-NLS-1$
		setDescription("Shows / hides the debug action from the visualizer toolbar"); //$NON-NLS-1$
	}

	/** Dispose method. */
	@Override
	public void dispose()
	{
		m_visualizer = null;
		super.dispose();
	}


	// --- init methods ---

	/** Initializes this action for the specified view. */
	public void init(MulticoreVisualizer visualizer)
	{
		m_visualizer = visualizer;
	}


	// --- methods ---

	/** Invoked when action is triggered. */
	@Override
	public void run() {
		if (m_visualizer != null) {
			m_showDebugActions.set(this.isChecked());
			// trigger refresh of canvas
			m_visualizer.raiseVisualizerChangedEvent();
		}
	}
}
