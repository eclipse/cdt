/*******************************************************************************
 * Copyright (c) 2008, 2015 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     Marc Khouzam (Ericsson) - Support for exited processes in the debug view (bug 407340)
 *******************************************************************************/
package org.eclipse.cdt.dsf.gdb.internal.ui.viewmodel.launch;

import java.util.Map;

import org.eclipse.cdt.dsf.debug.ui.viewmodel.launch.ExecutionContextLabelText;
import org.eclipse.cdt.dsf.debug.ui.viewmodel.launch.ILaunchVMConstants;
import org.eclipse.core.runtime.IStatus;

/**
 * @since 2.0
 */
public class GdbExecutionContextLabelText extends ExecutionContextLabelText {

    public GdbExecutionContextLabelText(String formatPattern, String[] propertyNames) {
        super(formatPattern, propertyNames);
    }

    @Override
    protected Object getPropertyValue(String propertyName, IStatus status, Map<String, Object> properties) {
        if (IGdbLaunchVMConstants.PROP_OS_ID_KNOWN.equals(propertyName)) {
            return properties.get(IGdbLaunchVMConstants.PROP_OS_ID) != null ? 1 : 0;
        }
        if (IGdbLaunchVMConstants.PROP_CORES_ID_KNOWN.equals(propertyName)) {
            return properties.get(IGdbLaunchVMConstants.PROP_CORES_ID) != null ? 1 : 0;
        } 
        if (IGdbLaunchVMConstants.PROP_THREAD_SUMMARY_KNOWN.equals(propertyName)) {
            return properties.get(IGdbLaunchVMConstants.PROP_THREAD_SUMMARY) != null ? 1 : 0;
        }
        if (IGdbLaunchVMConstants.PROP_EXIT_CODE_KNOWN.equals(propertyName)) {
        	return properties.get(IGdbLaunchVMConstants.PROP_EXIT_CODE) != null ? 1 : 0;
        }
        if (ILaunchVMConstants.PROP_ELEMENT_SELECTED_KNOWN.equals(propertyName)) {
        	return properties.get(ILaunchVMConstants.PROP_ELEMENT_SELECTED) != null ? 1 : 0;
        }
        return super.getPropertyValue(propertyName, status, properties);
    }

    @Override
    protected boolean checkProperty(String propertyName, IStatus status, Map<String, Object> properties) {
        if (IGdbLaunchVMConstants.PROP_OS_ID_KNOWN.equals(propertyName) ||
            IGdbLaunchVMConstants.PROP_OS_ID.equals(propertyName) ||
            IGdbLaunchVMConstants.PROP_CORES_ID_KNOWN.equals(propertyName) ||
            IGdbLaunchVMConstants.PROP_CORES_ID.equals(propertyName) ||
            IGdbLaunchVMConstants.PROP_THREAD_SUMMARY_KNOWN.equals(propertyName) ||
        	IGdbLaunchVMConstants.PROP_THREAD_SUMMARY.equals(propertyName) ||
        	IGdbLaunchVMConstants.PROP_EXIT_CODE_KNOWN.equals(propertyName) ||
        	IGdbLaunchVMConstants.PROP_EXIT_CODE.equals(propertyName ) || 
        	ILaunchVMConstants.PROP_ELEMENT_SELECTED_KNOWN.equals(propertyName) ||
        	ILaunchVMConstants.PROP_ELEMENT_SELECTED.equals(propertyName))
        {
        	return true;
        } 
        return super.checkProperty(propertyName, status, properties);
    }
}
