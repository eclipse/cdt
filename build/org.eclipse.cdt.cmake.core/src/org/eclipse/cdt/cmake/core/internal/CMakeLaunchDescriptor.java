/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cdt.cmake.core.internal;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.PlatformObject;
import org.eclipse.launchbar.core.ILaunchDescriptor;
import org.eclipse.launchbar.core.ILaunchDescriptorType;

public class CMakeLaunchDescriptor extends PlatformObject implements ILaunchDescriptor {

	private final CMakeLaunchDescriptorType type;
	private final IProject project;

	public CMakeLaunchDescriptor(CMakeLaunchDescriptorType type, IProject project) {
		this.type = type;
		this.project = project;
	}

	@Override
	public String getName() {
		return project.getName();
	}

	@Override
	public ILaunchDescriptorType getType() {
		return type;
	}

	public IProject getProject() {
		return project;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <T> T getAdapter(Class<T> adapter) {
		if (adapter.equals(IProject.class)) {
			return (T) project;
		} else {
			return super.getAdapter(adapter);
		}
	}

}
