/*******************************************************************************
 * Copyright (c) 2014 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Doug Schaefer
 *******************************************************************************/
package org.eclipse.cdt.launchbar.core;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationType;
import org.eclipse.debug.core.ILaunchManager;

/**
 * The provider of launch configurations of a given type for a given descriptor type
 * and a given target type.
 */
public interface ILaunchConfigurationProvider {

	/**
	 * Does this provider own this launch configuration. If so, make sure the launch descriptor
	 * is properly constructed by sending in a launch object to the launch manager.
	 * And return that object.
	 * 
	 * @param configuration
	 * @return launch object that relates to this config
	 * @throws CoreException
	 */
	Object launchConfigurationAdded(ILaunchConfiguration configuration) throws CoreException;

	/**
	 * A launch configuration has been removed.
	 * 
	 * @param configuration
	 * @return was the launch configuration removed by this provider?
	 * @throws CoreException
	 */
	boolean launchConfigurationRemoved(ILaunchConfiguration configuration) throws CoreException;

	/**
	 * Returns the launch configuration type for configurations created by this provider.
	 * 
	 * @return launch configuration type
	 * @throws CoreException 
	 */
	ILaunchConfigurationType getLaunchConfigurationType() throws CoreException;

	/**
	 * Create a launch configuration for the descriptor to launch on the target.
	 * 
	 * @param descriptor
	 * @param target
	 * @return launch configuration
	 * @throws CoreException 
	 */
	ILaunchConfiguration createLaunchConfiguration(ILaunchManager launchManager, ILaunchDescriptor descriptor) throws CoreException;

}
