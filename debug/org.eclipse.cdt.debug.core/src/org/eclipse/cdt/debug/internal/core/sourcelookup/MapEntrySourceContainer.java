/*******************************************************************************
 * Copyright (c) 2004, 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.debug.internal.core.sourcelookup; 

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.IOCase;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.debug.core.CDebugCorePlugin;
import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.internal.core.model.ExternalTranslationUnit;
import org.eclipse.cdt.internal.core.resources.ResourceLookup;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.sourcelookup.ISourceContainerType;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.containers.AbstractSourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;

import com.ibm.icu.text.MessageFormat;
 
/**
 * The source container that maps a backend path to the local filesystem path.
 */
public class MapEntrySourceContainer extends AbstractSourceContainer {
	/**
	 * Unique identifier for the map entry source container type
	 * (value <code>org.eclipse.cdt.debug.core.containerType.mapEntry</code>).
	 */
	public static final String TYPE_ID = CDebugCorePlugin.getUniqueIdentifier() + ".containerType.mapEntry"; //$NON-NLS-1$

	private IPath fLocalPath;
	private String fBackend;

	/**
	 * Constructor for MapEntrySourceContainer.
	 */
	public MapEntrySourceContainer() {
		fBackend = ""; //$NON-NLS-1$
		fLocalPath = Path.EMPTY;
	}

	/**
	 * Constructor for MapEntrySourceContainer.
	 * @deprecated Use {@link #MapEntrySourceContainer(String, IPath)}
	 */
	@Deprecated
	public MapEntrySourceContainer(IPath backend, IPath local) {
		fBackend = backend.toOSString();
		fLocalPath = local;
	}

	/**
	 * Constructor for MapEntrySourceContainer.
	 */
	public MapEntrySourceContainer(String backend, IPath local) {
		fBackend = backend;
		fLocalPath = local;
	}

	/**
	 * Creates an IPath from a string which may be a Win32 path.
	 * <p>
	 * <p>
	 * ("new Path(...)" won't work in Unix when using a Win32 path: the
	 * backslash separator and the device notation are completely munged.)
	 * Copied from org.eclipse.cdt.debug.edc.internal.PathUtils
	 *
	 * @param path
	 * @return converted string
	 */
	public static IPath createPath(String path) {
		if (path == null)
			return null;

		// Check for windows full-path formatting.
		if (path.matches("^([a-zA-Z])[:](.*)$")) { //$NON-NLS-1$
			String device = null;
			String missingfile = path.replace("\\", "/"); //$NON-NLS-1$ //$NON-NLS-2$
			int idx = missingfile.indexOf(":"); //$NON-NLS-1$
			if ( idx > 0 ) {
				device = missingfile.substring(0, idx + 1);
				missingfile = missingfile.substring(idx + 1);
			}
			return new Path(device, missingfile);
		}		

		int idx = 0;
		// Cygwin or UNC path
		if (path.startsWith("//")) { //$NON-NLS-1$
			String network;
			idx = path.indexOf("/", 2); //$NON-NLS-1$
			if (idx > 0) {
				network = path.substring(0, idx);
				path = path.substring(idx);
			} else {
				network = path;
				path = ""; //$NON-NLS-1$
			}
			return new Path(network, path).makeUNC(true);
		}		
		
		// fallthrough
		return new Path(path);
	}

	@Override
	public Object[] findSourceElements(String name) throws CoreException {
		IPath path = null;

		if (name != null) {
			// First try the non-canonical comparison
			final String backend = getBackend();
			if (IOCase.INSENSITIVE.checkStartsWith(name, backend)) {
				String suffix = name.substring(backend.length());
				// checkStartsWith only verifies that the paths are the same up
				// to getBackend(), however if name=/hello2/a.c and backend=/hello
				// then checkStartsWith will be true, so we have to further check
				// that we are on a separator
				if (backend.endsWith("/") || backend.endsWith("\\") || //$NON-NLS-1$ //$NON-NLS-2$
						suffix.startsWith("/") || suffix.startsWith("\\")) { //$NON-NLS-1$ //$NON-NLS-2$
					path = getLocalPath().append(suffix);
				}
			}

			// Then if we have not matched, try the legacy way of canonicalizing
			// the paths and comparing them
			if (path == null) {
				IPath input = createPath(name);
				IPath backendPath = createPath(backend);
				if (backendPath.isPrefixOf(input)) {
					IPath suffix = input.removeFirstSegments(backendPath.segmentCount());
					path = getLocalPath().append(suffix);
				}
			}
		}

		if (path != null) {
			IFile[] wsFiles = ResourceLookup.findFilesForLocation(path);
			ArrayList<IFile> list = new ArrayList<IFile>();
			for (int j = 0; j < wsFiles.length; ++j) {
				if (wsFiles[j].exists()) {
					list.add(wsFiles[j]);
					if (!isFindDuplicates())
						break;
				}
			}
			if (list.size() > 0) 
				return list.toArray();

			File file = path.toFile();

			// The file is not already in the workspace so try to create an external translation unit for it.
			ISourceLookupDirector director = getDirector();
			if (director != null && file.exists() && file.isFile()) {
				ILaunchConfiguration launchConfiguration = director.getLaunchConfiguration();
				if (launchConfiguration != null) {
					String projectName = launchConfiguration.getAttribute(ICDTLaunchConfigurationConstants.ATTR_PROJECT_NAME, ""); //$NON-NLS-1$
					if (projectName.length() > 0) {
						ICProject project = CoreModel.getDefault().getCModel().getCProject(projectName);
						if (project != null) {
							String id;
							try {
								final IPath location= Path.fromOSString(file.getCanonicalPath());
								id = CoreModel.getRegistedContentTypeId(project.getProject(), location.lastSegment());
								return new ExternalTranslationUnit[] { new ExternalTranslationUnit(project, location, id) };
							} catch (IOException e) {
								CDebugCorePlugin.log(e);
							}
						}
					}									
				}
			}

			if (file.exists() && file.isFile()) {
				return new Object[] { new LocalFileStorage(file) };
			}
		}
		return EMPTY;
	}

	@Override
	public String getName() {
		return MessageFormat.format("{0} - {1}", new Object[] { getBackend(), getLocalPath().toOSString() }); //$NON-NLS-1$
	}

	@Override
	public ISourceContainerType getType() {
		return getSourceContainerType(TYPE_ID);
	}
	
	public IPath getLocalPath() {
		return fLocalPath;
	}

	/**
	 * @deprecated use {@link #getBackend()} instead
	 */
	@Deprecated
	public IPath getBackendPath() {
		return createPath(getBackend());
	}

	public String getBackend() {
		return fBackend;
	}

	public void setLocalPath(IPath local) {
		fLocalPath = local;
	}

	/**
	 * @deprecated use {@link #setBackend(String)} instead
	 */
	@Deprecated
	public void setBackendPath(IPath backend) {
		fBackend = backend.toOSString();
	}

	public void setBackend(String backend) {
		fBackend = backend;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof MapEntrySourceContainer))
			return false;
		MapEntrySourceContainer entry = (MapEntrySourceContainer)o;
		return (entry.getBackend().equals(getBackend()) && entry.getLocalPath().equals(getLocalPath()));
	}

	public MapEntrySourceContainer copy() {
		return new MapEntrySourceContainer(fBackend, fLocalPath);
	}
}
