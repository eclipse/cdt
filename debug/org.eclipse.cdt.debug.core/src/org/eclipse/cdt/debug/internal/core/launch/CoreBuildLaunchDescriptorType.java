package org.eclipse.cdt.debug.internal.core.launch;

import org.eclipse.cdt.core.build.ICBuildConfigurationManager;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.launchbar.core.ILaunchDescriptor;
import org.eclipse.launchbar.core.ILaunchDescriptorType;
import org.eclipse.launchbar.core.internal.Activator;

/**
 * The launch descriptor type for launch objects built with the Core Build System.
 */
public class CoreBuildLaunchDescriptorType implements ILaunchDescriptorType {

	@Override
	public ILaunchDescriptor getDescriptor(Object launchObject) throws CoreException {
		if (launchObject instanceof IProject) {
			// Make sure it's a new style build
			IProject project = (IProject) launchObject;
			if (Activator.getService(ICBuildConfigurationManager.class).supports(project)) {
				return new CoreBuildProjectLaunchDescriptor(this, project);
			}
		}
		// TODO IBinary
		return null;
	}

}
