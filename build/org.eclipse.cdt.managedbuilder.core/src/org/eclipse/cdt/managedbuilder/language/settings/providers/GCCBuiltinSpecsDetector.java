/*******************************************************************************
 * Copyright (c) 2009, 2013 Andrew Gvozdev and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Andrew Gvozdev - initial API and implementation
 *******************************************************************************/

package org.eclipse.cdt.managedbuilder.language.settings.providers;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.core.EFSExtensionProvider;
import org.eclipse.cdt.core.language.settings.providers.ILanguageSettingsEditableProvider;
import org.eclipse.cdt.core.language.settings.providers.IWorkingDirectoryTracker;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.internal.core.Cygwin;
import org.eclipse.cdt.managedbuilder.core.ManagedBuilderCorePlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;

/**
 * Language settings provider to detect built-in compiler settings for GCC compiler.
 * <p>
 * <strong>EXPERIMENTAL</strong>. This class interface is not stable yet as
 * it is not currently (CDT 8.1, Juno) clear how it may need to be used in future.
 * There is no guarantee that this API will work or that it will remain the same.
 * Please do not use this API without consulting with the CDT team.
 * </p>
 * @noextend This class is not intended to be subclassed by clients.
 *
 * @since 8.1
 */
public class GCCBuiltinSpecsDetector extends ToolchainBuiltinSpecsDetector implements ILanguageSettingsEditableProvider {
	// ID must match the tool-chain definition in org.eclipse.cdt.managedbuilder.core.buildDefinitions extension point
	private static final String GCC_TOOLCHAIN_ID = "cdt.managedbuild.toolchain.gnu.base";  //$NON-NLS-1$
	private static final String ENV_PATH = "PATH"; //$NON-NLS-1$

	private enum State {NONE, EXPECTING_LOCAL_INCLUDE, EXPECTING_SYSTEM_INCLUDE, EXPECTING_FRAMEWORKS}
	private State state = State.NONE;

	@SuppressWarnings("nls")
	private static final AbstractOptionParser[] optionParsers = {
			new IncludePathOptionParser("#include \"(\\S.*)\"", "$1", ICSettingEntry.BUILTIN | ICSettingEntry.READONLY | ICSettingEntry.LOCAL),
			new IncludePathOptionParser("#include <(\\S.*)>", "$1", ICSettingEntry.BUILTIN | ICSettingEntry.READONLY),
			new IncludePathOptionParser("#framework <(\\S.*)>", "$1", ICSettingEntry.BUILTIN | ICSettingEntry.READONLY | ICSettingEntry.FRAMEWORKS_MAC),
			new MacroOptionParser("#define\\s+(\\S*\\(.*?\\))\\s*(.*)", "$1", "$2", ICSettingEntry.BUILTIN | ICSettingEntry.READONLY),
			new MacroOptionParser("#define\\s+(\\S*)\\s*(.*)", "$1", "$2", ICSettingEntry.BUILTIN | ICSettingEntry.READONLY),
	};

	/**
	 * @since 8.2
	 */
	@Override
	public String getToolchainId() {
		return GCC_TOOLCHAIN_ID;
	}

	@Override
	protected AbstractOptionParser[] getOptionParsers() {
		return optionParsers;
	}

	/**
	 * Create a list from one item.
	 */
	private List<String> makeList(String line) {
		List<String> list = new ArrayList<String>();
		list.add(line);
		return list;
	}

	@SuppressWarnings("nls")
	@Override
	protected List<String> parseOptions(String line) {
		line = line.trim();

		// contribution of -dD option
		if (line.startsWith("#define")) {
			return makeList(line);
		}

		// contribution of includes
		if (line.equals("#include \"...\" search starts here:")) {
			state = State.EXPECTING_LOCAL_INCLUDE;
		} else if (line.equals("#include <...> search starts here:")) {
			state = State.EXPECTING_SYSTEM_INCLUDE;
		} else if (line.startsWith("End of search list.")) {
			state = State.NONE;
		} else if (line.equals("Framework search starts here:")) {
			state = State.EXPECTING_FRAMEWORKS;
		} else if (line.startsWith("End of framework search list.")) {
			state = State.NONE;
		} else if (state==State.EXPECTING_LOCAL_INCLUDE) {
			// making that up for the parser to figure out
			line = "#include \""+line+"\"";
			return makeList(line);
		} else {
			String frameworkIndicator = "(framework directory)";
			if (state==State.EXPECTING_SYSTEM_INCLUDE) {
				// making that up for the parser to figure out
				if (line.contains(frameworkIndicator)) {
					line = "#framework <"+line.replace(frameworkIndicator, "").trim()+">";
				} else {
					line = "#include <"+line+">";
				}
				return makeList(line);
			} else if (state==State.EXPECTING_FRAMEWORKS) {
				// making that up for the parser to figure out
				line = "#framework <"+line.replace(frameworkIndicator, "").trim()+">";
				return makeList(line);
			}
		}

		return null;
	}

	@Override
	public void startup(ICConfigurationDescription cfgDescription, IWorkingDirectoryTracker cwdTracker) throws CoreException {
		super.startup(cfgDescription, cwdTracker);

		state = State.NONE;
	}

	@Override
	public void shutdown() {
		state = State.NONE;

		super.shutdown();
	}

	/**
	 * EFSExtensionProvider for Cygwin translations
	 */
	private class CygwinEFSExtensionProvider extends EFSExtensionProvider {
		private String envPathValue;

		/**
		 * Constructor.
		 * @param envPathValue - Value of environment variable $PATH.
		 */
		public CygwinEFSExtensionProvider(String envPathValue) {
			this.envPathValue = envPathValue;
		}

		@Override
		public String getMappedPath(URI locationURI) {
			String windowsPath = null;
			try {
				String cygwinPath = getPathFromURI(locationURI);
				windowsPath = Cygwin.cygwinToWindowsPath(cygwinPath, envPathValue);
			} catch (Exception e) {
				ManagedBuilderCorePlugin.log(e);
			}
			if (windowsPath != null) {
				return windowsPath;
			}

			return super.getMappedPath(locationURI);
		}
	}

	@Override
	protected EFSExtensionProvider getEFSProvider() {
		if (Platform.getOS().equals(Platform.OS_WIN32)) {
			String envPathValue = environmentMap != null ? environmentMap.get(ENV_PATH) : null;
			return new CygwinEFSExtensionProvider(envPathValue);
		} else {
			return super.getEFSProvider();
		}
	}

	@Override
	public GCCBuiltinSpecsDetector cloneShallow() throws CloneNotSupportedException {
		return (GCCBuiltinSpecsDetector) super.cloneShallow();
	}

	@Override
	public GCCBuiltinSpecsDetector clone() throws CloneNotSupportedException {
		return (GCCBuiltinSpecsDetector) super.clone();
	}


}
