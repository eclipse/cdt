/*******************************************************************************
 * Copyright (c) 2006, 2013 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Doug Schaefer, QNX Software Systems - Initial API and implementation
 *     Andrew Gvozdev                      - Ability to use different MinGw versions in different cfg
 *     Yannick Mayeur & Pierre Sachot      - MSYS2 adapatation
 *******************************************************************************/

package org.eclipse.cdt.managedbuilder.gnu.msys2;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.envvar.IEnvironmentVariable;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.internal.core.MSYS2;
import org.eclipse.cdt.internal.core.envvar.EnvironmentVariableManager;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.envvar.IBuildEnvironmentVariable;
import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;
import org.eclipse.cdt.managedbuilder.envvar.IEnvironmentVariableProvider;
import org.eclipse.cdt.managedbuilder.internal.envvar.BuildEnvVar;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 * @noextend This class is not intended to be subclassed by clients.
 * @since 8.4
 */
public class MSYS2EnvironmentVariableSupplier implements IConfigurationEnvironmentVariableSupplier {
	private static final String ENV_PATH = "PATH"; //$NON-NLS-1$
	private static final String BACKSLASH = java.io.File.separator;
	private static final String PATH_DELIMITER = EnvironmentVariableManager.getDefault().getDefaultDelimiter();

	@Override
	public IBuildEnvironmentVariable getVariable(String variableName, IConfiguration configuration, IEnvironmentVariableProvider provider) {
		if (variableName.equals(MSYS2.ENV_MSYS2_HOME)) {
			IEnvironmentVariable varMSYS2Home = CCorePlugin.getDefault().getBuildEnvironmentManager().getVariable(MSYS2.ENV_MSYS2_HOME, (ICConfigurationDescription) null, false);
			if (varMSYS2Home == null) {
				// Contribute if the variable does not already come from workspace environment
				String MSYS2Home = MSYS2.getMSYS2Home();
				if (MSYS2Home == null) {
					// If the variable is not defined still show it in the environment variables list as a hint to user
					MSYS2Home = ""; //$NON-NLS-1$
				}
				return new BuildEnvVar(MSYS2.ENV_MSYS2_HOME, new Path(MSYS2Home).toOSString(), IBuildEnvironmentVariable.ENVVAR_REPLACE);
			}
			return null;

		} else if (variableName.equals(MSYS2.ENV_MSYS2_HOME)) {
			IEnvironmentVariable varMsysHome = CCorePlugin.getDefault().getBuildEnvironmentManager().getVariable(MSYS2.ENV_MSYS2_HOME, (ICConfigurationDescription) null, false);
			if (varMsysHome == null) {
				// Contribute if the variable does not already come from workspace environment
				String msysHome = MSYS2.getMSysHome();
				if (msysHome == null) {
					// If the variable is not defined still show it in the environment variables list as a hint to user
					msysHome = ""; //$NON-NLS-1$
				}
				return new BuildEnvVar(MSYS2.ENV_MSYS2_HOME, new Path(msysHome).toOSString(), IBuildEnvironmentVariable.ENVVAR_REPLACE);
			}
			return null;

		} else if (variableName.equals(ENV_PATH)) {
			@SuppressWarnings("nls")
			String path = "${" + MSYS2.ENV_MSYS2_HOME + "}" + BACKSLASH + "bin" + PATH_DELIMITER
					+ "${" + MSYS2.ENV_MSYS2_HOME + "}" + BACKSLASH + "bin" + PATH_DELIMITER
					+ "${" + MSYS2.ENV_MSYS2_HOME + "}" + BACKSLASH + "usr" + BACKSLASH + "bin";
			return new BuildEnvVar(ENV_PATH, path, IBuildEnvironmentVariable.ENVVAR_PREPEND);
		}

		return null;
	}

	@Override
	public IBuildEnvironmentVariable[] getVariables(IConfiguration configuration, IEnvironmentVariableProvider provider) {
		return new IBuildEnvironmentVariable[] {
				getVariable(MSYS2.ENV_MSYS2_HOME, configuration, provider),
				getVariable(MSYS2.ENV_MSYS2_HOME, configuration, provider),
				getVariable(ENV_PATH, configuration, provider),
			};
	}

}
