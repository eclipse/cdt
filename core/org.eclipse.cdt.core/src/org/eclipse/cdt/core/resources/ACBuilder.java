/*******************************************************************************
 * Copyright (c) 2000, 2013 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     IBM Corporation
 *     James Blackburn (Broadcom Corp.)
 *******************************************************************************/
package org.eclipse.cdt.core.resources;


import java.net.URI;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CCorePreferenceConstants;
import org.eclipse.cdt.core.IMarkerGenerator;
import org.eclipse.cdt.core.ProblemMarkerInfo;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.ICModelMarker;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.jobs.ISchedulingRule;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.osgi.util.NLS;

public abstract class ACBuilder extends IncrementalProjectBuilder implements IMarkerGenerator {
	private static final String CONTENTS_CONFIGURATION_IDS = "org.eclipse.cdt.make.core.configurationIds"; //$NON-NLS-1$
	private static final IEclipsePreferences prefs = InstanceScope.INSTANCE.getNode(CCorePlugin.PLUGIN_ID);
	/** @since 5.2 */ // set to true to print build events on the console in debug mode
	protected static final boolean DEBUG_EVENTS = false;

	/**
	 * Constructor for ACBuilder
	 */
	public ACBuilder() {
		super();
	}

	@Override
	public void addMarker(IResource file, int lineNumber, String errorDesc, int severity, String errorVar) {
		ProblemMarkerInfo problemMarkerInfo = new ProblemMarkerInfo(file, lineNumber, errorDesc, severity, errorVar, null);
		addMarker(problemMarkerInfo);
	}

	/*
	 * callback from Output Parser
	 */
	@Override
	public void addMarker(ProblemMarkerInfo problemMarkerInfo) {
		try {
			IProject project = getProject();
			IResource markerResource = problemMarkerInfo.file;
			if (markerResource == null)  {
				markerResource = project;
			}
			String externalLocation = null;
			if (problemMarkerInfo.externalPath != null && ! problemMarkerInfo.externalPath.isEmpty()) {
				externalLocation = problemMarkerInfo.externalPath.toOSString();
			}

			// Try to find matching markers and don't put in duplicates
			IMarker[] markers = markerResource.findMarkers(ICModelMarker.C_MODEL_PROBLEM_MARKER, true, IResource.DEPTH_ONE);
			for (IMarker m : markers) {
				int line = m.getAttribute(IMarker.LINE_NUMBER, -1);
				int sev = m.getAttribute(IMarker.SEVERITY, -1);
				String msg = (String) m.getAttribute(IMarker.MESSAGE);
				if (line == problemMarkerInfo.lineNumber && sev == mapMarkerSeverity(problemMarkerInfo.severity) && msg.equals(problemMarkerInfo.description)) {
					String extloc = (String) m.getAttribute(ICModelMarker.C_MODEL_MARKER_EXTERNAL_LOCATION);
					if (extloc == externalLocation || (extloc != null && extloc.equals(externalLocation))) {
						if (project == null || project.equals(markerResource.getProject())) {
							return;
						}
						String source = (String) m.getAttribute(IMarker.SOURCE_ID);
						if (project.getName().equals(source)) {
							return;
						}
					}
				}
			}

			String type = problemMarkerInfo.getType();
			if (type == null) {
				type = ICModelMarker.C_MODEL_PROBLEM_MARKER;
			}

			IMarker marker = markerResource.createMarker(type);
			marker.setAttribute(IMarker.MESSAGE, problemMarkerInfo.description);
			marker.setAttribute(IMarker.SEVERITY, mapMarkerSeverity(problemMarkerInfo.severity));
			marker.setAttribute(IMarker.LINE_NUMBER, problemMarkerInfo.lineNumber);
			marker.setAttribute(IMarker.CHAR_START, problemMarkerInfo.startChar);
			marker.setAttribute(IMarker.CHAR_END, problemMarkerInfo.endChar);
			if (problemMarkerInfo.variableName != null) {
				marker.setAttribute(ICModelMarker.C_MODEL_MARKER_VARIABLE, problemMarkerInfo.variableName);
			}
			if (externalLocation != null) {
				URI uri = URIUtil.toURI(externalLocation);
				if (uri.getScheme()!=null) {
					marker.setAttribute(ICModelMarker.C_MODEL_MARKER_EXTERNAL_LOCATION, externalLocation);
					String locationText = NLS.bind(CCorePlugin.getResourceString("ACBuilder.ProblemsView.Location"), //$NON-NLS-1$
							problemMarkerInfo.lineNumber, externalLocation);
					marker.setAttribute(IMarker.LOCATION, locationText);
				}
			} else if (problemMarkerInfo.lineNumber==0){
				marker.setAttribute(IMarker.LOCATION, " "); //$NON-NLS-1$
			}
			// Set source attribute only if the marker is being set to a file from different project
			if (project != null && !project.equals(markerResource.getProject())) {
				marker.setAttribute(IMarker.SOURCE_ID, project.getName());
			}

			// Add all other client defined attributes.
			Map<String, String> attributes = problemMarkerInfo.getAttributes();
			if (attributes != null){
				for (Entry<String, String> entry : attributes.entrySet()) {
					marker.setAttribute(entry.getKey(), entry.getValue());
				}
			}
		}
		catch (CoreException e) {
			CCorePlugin.log(e.getStatus());
		}

	}

	int mapMarkerSeverity(int severity) {
		switch (severity) {
		case SEVERITY_ERROR_BUILD :
		case SEVERITY_ERROR_RESOURCE :
			return IMarker.SEVERITY_ERROR;
		case SEVERITY_INFO :
			return IMarker.SEVERITY_INFO;
		case SEVERITY_WARNING :
			return IMarker.SEVERITY_WARNING;
		}
		return IMarker.SEVERITY_ERROR;
	}

	public static boolean needAllConfigBuild() {
		return prefs.getBoolean(CCorePreferenceConstants.PREF_BUILD_ALL_CONFIGS, false);
	}

	public static void setAllConfigBuild(boolean enable) {
		prefs.putBoolean(CCorePreferenceConstants.PREF_BUILD_ALL_CONFIGS, enable);
	}

	/**
	 * Preference for building configurations only when there are resource changes within Eclipse or
	 * when there are changes in its references.
	 * @return true if configurations will be build when project resource changes within Eclipse
	 *         false otherwise
	 * @since 5.1
	 */
	public static boolean buildConfigResourceChanges() {
		//bug 219337
		return prefs.getBoolean(CCorePreferenceConstants.PREF_BUILD_CONFIGS_RESOURCE_CHANGES, false);
	}

	/**
	 * Preference for building configurations only when there are resource changes within Eclipse or
	 * when there are changes in its references.
	 * @param enable
	 * @since 5.1
	 */
	public static void setBuildConfigResourceChanges(boolean enable) {
		prefs.putBoolean(CCorePreferenceConstants.PREF_BUILD_CONFIGS_RESOURCE_CHANGES, enable);
	}

	@SuppressWarnings("nls")
	private static String kindToString(int kind) {
		return (kind==IncrementalProjectBuilder.AUTO_BUILD ? "AUTO_BUILD"
				: kind==IncrementalProjectBuilder.CLEAN_BUILD ? "CLEAN_BUILD"
				: kind==IncrementalProjectBuilder.FULL_BUILD ? "FULL_BUILD"
				: kind==IncrementalProjectBuilder.INCREMENTAL_BUILD ? "INCREMENTAL_BUILD"
				: "[unknown kind]")+"="+kind;
	}

	@SuppressWarnings("nls")
	private String cfgIdToNames(String strIds) {
		IProject project = getProject();
		ICProjectDescription prjDesc = CoreModel.getDefault().getProjectDescription(project, false);
		if (prjDesc == null) {
			return strIds;
		}

		if (strIds == null) {
			return "Active=" + prjDesc.getActiveConfiguration().getName();
		}

		String[] ids = strIds.split("\\|");
		String names="";
		for (String id : ids) {
			ICConfigurationDescription cfgDesc = prjDesc.getConfigurationById(id);
			String name;
			if (cfgDesc != null) {
				name = cfgDesc.getName();
			} else {
				name = id;
			}

			if (names.length() >0 ) {
				names=names+",";
			}
			names = names + name;
		}
		if (names.equals("")) {
			return strIds;
		}
		return names;
	}

	/**
	 * For debugging purpose only. Prints events on the debug console.
	 *
	 * @since 5.2
	 */
	@SuppressWarnings("nls")
	protected void printEvent(int kind, Map<String, String> args) {
		if (DEBUG_EVENTS) {
			String ids = args!=null ? args.get(CONTENTS_CONFIGURATION_IDS) : null;
			System.out.println("t"+Thread.currentThread().getId()+": "
					+ kindToString(kind)
					+ ", " +  getProject()
					+ "[" + cfgIdToNames(ids) +"]"
					+ ", " + this.getClass().getSimpleName()
				);
		}
	}

	@Override
	// This method is overridden with no purpose but to track events in debug mode
	protected void clean(IProgressMonitor monitor) throws CoreException {
		super.clean(monitor);
		if (DEBUG_EVENTS) {
			printEvent(IncrementalProjectBuilder.CLEAN_BUILD, null);
		}
	}

	/**
	 * Default ACBuilder shouldn't require locking the workspace during a CDT Project build.
	 *
	 * Note this may have a detrimental effect on #getDelta().  Derived builders which rely
	 * on #getDelta(...) being accurate should return a WorkspaceRoot scheduling rule.
	 * @since 5.2
	 */
	@Override
	@SuppressWarnings("rawtypes")
	public ISchedulingRule getRule(int trigger, Map args) {
		return null;
	}

}
