/*******************************************************************************
 * Copyright (c) 2003, 2007 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.managedbuilder.core;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.eclipse.cdt.core.AbstractCExtension;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.model.CoreModel;
import org.eclipse.cdt.core.model.CoreModelUtil;
import org.eclipse.cdt.core.model.ICProject;
import org.eclipse.cdt.core.model.IPathEntry;
import org.eclipse.cdt.core.parser.IScannerInfo;
import org.eclipse.cdt.core.parser.IScannerInfoChangeListener;
import org.eclipse.cdt.core.parser.IScannerInfoProvider;
import org.eclipse.cdt.core.settings.model.ICConfigurationDescription;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICProjectDescription;
import org.eclipse.cdt.core.settings.model.ICProjectDescriptionManager;
import org.eclipse.cdt.core.settings.model.extension.CConfigurationData;
import org.eclipse.cdt.core.settings.model.util.XmlStorageElement;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildProperty;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyManager;
import org.eclipse.cdt.managedbuilder.envvar.IEnvironmentBuildPathsChangeListener;
import org.eclipse.cdt.managedbuilder.envvar.IEnvironmentVariableProvider;
import org.eclipse.cdt.managedbuilder.internal.buildproperties.BuildPropertyManager;
import org.eclipse.cdt.managedbuilder.internal.core.BooleanExpressionApplicabilityCalculator;
import org.eclipse.cdt.managedbuilder.internal.core.BuildDbgUtil;
import org.eclipse.cdt.managedbuilder.internal.core.BuildSettingsUtil;
import org.eclipse.cdt.managedbuilder.internal.core.Builder;
import org.eclipse.cdt.managedbuilder.internal.core.BuilderFactory;
import org.eclipse.cdt.managedbuilder.internal.core.CommonBuilder;
import org.eclipse.cdt.managedbuilder.internal.core.Configuration;
import org.eclipse.cdt.managedbuilder.internal.core.DefaultManagedConfigElement;
import org.eclipse.cdt.managedbuilder.internal.core.FolderInfo;
import org.eclipse.cdt.managedbuilder.internal.core.IMatchKeyProvider;
import org.eclipse.cdt.managedbuilder.internal.core.InputType;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedBuildInfo;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedCommandLineGenerator;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedMakeMessages;
import org.eclipse.cdt.managedbuilder.internal.core.ManagedProject;
import org.eclipse.cdt.managedbuilder.internal.core.Option;
import org.eclipse.cdt.managedbuilder.internal.core.OptionCategory;
import org.eclipse.cdt.managedbuilder.internal.core.OutputType;
import org.eclipse.cdt.managedbuilder.internal.core.ProjectType;
import org.eclipse.cdt.managedbuilder.internal.core.ResourceConfiguration;
import org.eclipse.cdt.managedbuilder.internal.core.Target;
import org.eclipse.cdt.managedbuilder.internal.core.TargetPlatform;
import org.eclipse.cdt.managedbuilder.internal.core.Tool;
import org.eclipse.cdt.managedbuilder.internal.core.ToolChain;
import org.eclipse.cdt.managedbuilder.internal.dataprovider.BuildConfigurationData;
import org.eclipse.cdt.managedbuilder.internal.dataprovider.ConfigurationDataProvider;
import org.eclipse.cdt.managedbuilder.internal.envvar.EnvironmentVariableProvider;
import org.eclipse.cdt.managedbuilder.internal.macros.BuildMacroProvider;
import org.eclipse.cdt.managedbuilder.macros.IBuildMacroProvider;
import org.eclipse.cdt.managedbuilder.makegen.IManagedBuilderMakefileGenerator;
import org.eclipse.cdt.managedbuilder.makegen.gnu.GnuMakefileGenerator;
import org.eclipse.cdt.managedbuilder.projectconverter.UpdateManagedProjectManager;
import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceStatus;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PluginVersionIdentifier;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.SubProgressMonitor;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.osgi.framework.Bundle;
import org.osgi.framework.Version;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.ProcessingInstruction;

/**
 * This is the main entry point for getting at the build information
 * for the managed build system. 
 */
public class ManagedBuildManager extends AbstractCExtension implements IScannerInfoProvider {

//	private static final QualifiedName buildInfoProperty = new QualifiedName(ManagedBuilderCorePlugin.getUniqueIdentifier(), "managedBuildInfo");	//$NON-NLS-1$
	private static final String ROOT_NODE_NAME = "ManagedProjectBuildInfo";	//$NON-NLS-1$
	public  static final String SETTINGS_FILE_NAME = ".cdtbuild";	//$NON-NLS-1$
	private static final ITarget[] emptyTargets = new ITarget[0];
	public  static final String INTERFACE_IDENTITY = ManagedBuilderCorePlugin.getUniqueIdentifier() + ".ManagedBuildManager";	//$NON-NLS-1$
	public  static final String EXTENSION_POINT_ID = ManagedBuilderCorePlugin.getUniqueIdentifier() + ".buildDefinitions";		//$NON-NLS-1$
	public  static final String EXTENSION_POINT_ID_V2 = ManagedBuilderCorePlugin.getUniqueIdentifier() + ".ManagedBuildInfo";	//$NON-NLS-1$
	private static final String REVISION_ELEMENT_NAME = "managedBuildRevision";	//$NON-NLS-1$
	private static final String VERSION_ELEMENT_NAME = "fileVersion";	//$NON-NLS-1$
	private static final String MANIFEST_VERSION_ERROR ="ManagedBuildManager.error.manifest.version.error";	//$NON-NLS-1$
	private static final String PROJECT_VERSION_ERROR ="ManagedBuildManager.error.project.version.error";	//$NON-NLS-1$
	private static final String PROJECT_FILE_ERROR = "ManagedBuildManager.error.project.file.missing";	//$NON-NLS-1$
	private static final String MANIFEST_ERROR_HEADER = "ManagedBuildManager.error.manifest.header";	//$NON-NLS-1$
	public  static final String MANIFEST_ERROR_RESOLVING = "ManagedBuildManager.error.manifest.resolving";	//$NON-NLS-1$
	public  static final String MANIFEST_ERROR_DUPLICATE = "ManagedBuildManager.error.manifest.duplicate";	//$NON-NLS-1$
	public  static final String MANIFEST_ERROR_ICON = "ManagedBuildManager.error.manifest.icon";	//$NON-NLS-1$
	private static final String MANIFEST_ERROR_OPTION_CATEGORY = "ManagedBuildManager.error.manifest.option.category";	//$NON-NLS-1$
	private static final String MANIFEST_ERROR_OPTION_FILTER = "ManagedBuildManager.error.manifest.option.filter";	//$NON-NLS-1$
	private static final String MANIFEST_ERROR_OPTION_VALUEHANDLER = "ManagedBuildManager.error.manifest.option.valuehandler";	//$NON-NLS-1$
	private static final String MANIFEST_ERROR_READ_ONLY = "ManagedBuildManager.error.read_only";  //$NON-NLS-1$
	private static final String MANIFEST_ERROR_WRITE_FAILED = "ManagedBuildManager.error.write_failed";  //$NON-NLS-1$
	
	// Error ID's for OptionValidError()
	public static final int ERROR_CATEGORY = 0;
	public static final int ERROR_FILTER = 1;
	
	public static final String BUILD_TYPE_PROPERTY_ID = "org.eclipse.cdt.build.core.buildType";	//$NON-NLS-1$
	public static final String BUILD_ARTEFACT_TYPE_PROPERTY_ID = "org.eclipse.cdt.build.core.buildArtefactType";	//$NON-NLS-1$

	public static final String BUILD_TYPE_PROPERTY_DEBUG = "org.eclipse.cdt.build.core.buildType.debug";	//$NON-NLS-1$
	public static final String BUILD_TYPE_PROPERTY_RELEASE = "org.eclipse.cdt.build.core.buildType.release";	//$NON-NLS-1$
	public static final String BUILD_ARTEFACT_TYPE_PROPERTY_EXE = "org.eclipse.cdt.build.core.buildArtefactType.exe";	//$NON-NLS-1$
	public static final String BUILD_ARTEFACT_TYPE_PROPERTY_STATICLIB = "org.eclipse.cdt.build.core.buildArtefactType.staticLib";	//$NON-NLS-1$
	public static final String BUILD_ARTEFACT_TYPE_PROPERTY_SHAREDLIB = "org.eclipse.cdt.build.core.buildArtefactType.sharedLib";	//$NON-NLS-1$

	public static final String CFG_DATA_PROVIDER_ID = ManagedBuilderCorePlugin.getUniqueIdentifier() + ".configurationDataProvider"; //$NON-NLS-1$
	
	private static final String NEWLINE = System.getProperty("line.separator");	//$NON-NLS-1$
	
	public static final String INTERNAL_BUILDER_ID = "org.eclipse.cdt.build.core.internal.builder";	//$NON-NLS-1$
	
	// This is the version of the manifest and project files
	private static final PluginVersionIdentifier buildInfoVersion = new PluginVersionIdentifier(4, 0, 0);
	private static final Version version = new Version(4, 0, 0);
	private static Map depCalculatorsMap;
	private static boolean projectTypesLoaded = false;
	private static boolean projectTypesLoading = false;
	// Project types defined in the manifest files
	public static SortedMap projectTypeMap;
	private static List projectTypes;
	// Early configuration initialization extension elements
	private static List startUpConfigElements;
	// Configurations defined in the manifest files
	private static Map extensionConfigurationMap;
	// Resource configurations defined in the manifest files
	private static Map extensionResourceConfigurationMap;
	// Tool-chains defined in the manifest files
	private static SortedMap extensionToolChainMap;
	// Tools defined in the manifest files
	private static SortedMap extensionToolMap;
	// Target Platforms defined in the manifest files
	private static Map extensionTargetPlatformMap;
	// Builders defined in the manifest files
	private static SortedMap extensionBuilderMap;
	// Options defined in the manifest files
	private static Map extensionOptionMap;
	// Option Categories defined in the manifest files
	private static Map extensionOptionCategoryMap;
	// Input types defined in the manifest files
	private static Map extensionInputTypeMap;
	// Output types defined in the manifest files
	private static Map extensionOutputTypeMap;
	// Targets defined in the manifest files (CDT V2.0 object model)
	private static Map extensionTargetMap;
	// "Selected configuraton" elements defined in the manifest files.
	// These are configuration elements that map to objects in the internal
	// representation of the manifest files.  For example, ListOptionValues
	// and enumeratedOptionValues are NOT included.
	// Note that these "configuration elements" are not related to the
	// managed build system "configurations".  
	// From the PDE Guide:
	//  A configuration element, with its attributes and children, directly 
	//  reflects the content and structure of the extension section within the 
	//  declaring plug-in's manifest (plugin.xml) file. 
	private static Map configElementMap;
	
//	private static List sortedToolChains;
//	private static Map builtTypeToToolChainListMap;
	// Listeners interested in build model changes
	private static Map buildModelListeners;
	// Random number for derived object model elements
	private static Random randomNumber;
	// Environment Build Paths Change Listener
	private static IEnvironmentBuildPathsChangeListener fEnvironmentBuildPathsChangeListener;
	
	private static HashMap fSortedToolChains;
	private static HashMap fSortedTools;
	private static HashMap fSortedBuilders;
	
	private static Map fInfoMap = new HashMap();
	
	private static ISorter fToolChainSorter = new ISorter(){
		public void sort() {
			resortToolChains();
		}
	};
	private static ISorter fToolSorter = new ISorter(){
		public void sort() {
			resortTools();
		}
	};
	private static ISorter fBuilderSorter = new ISorter(){
		public void sort() {
			resortBuilders();
		}
	};
	
	private static interface ISorter {
		void sort();
	}

	
	static {
		getEnvironmentVariableProvider().subscribe(
				fEnvironmentBuildPathsChangeListener = new IEnvironmentBuildPathsChangeListener(){
					public void buildPathsChanged(IConfiguration configuration, int buildPathType){
//						if(buildPathType == IEnvVarBuildPath.BUILDPATH_INCLUDE){
//							initializePathEntries(configuration,null);
//							notifyListeners(configuration,null);
//						}
					}
				});
	}
	/**
	 * Returns the next random number.
	 * 
	 * @return int A positive integer
	 */
	public static int getRandomNumber() {
		if (randomNumber == null) {
			// Set the random number seed
			randomNumber = new Random();
			randomNumber.setSeed(System.currentTimeMillis());
		}
		int i = randomNumber.nextInt();
		if (i < 0) {
			i *= -1;
		}
		return i;
	}
	
	/**
	 * Returns the list of project types that are defined by this project,
	 * projects referenced by this project, and by the extensions. 
	 * 
	 * @return IProjectType[]
	 */
	public static IProjectType[] getDefinedProjectTypes() {
		try {
			// Make sure the extensions are loaded
			loadExtensions();
		} catch (BuildException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Get the project types for this project and all referenced projects
		List definedTypes = null;
		// To Do

		// Create the array and copy the elements over
		int size = projectTypes != null ? 
				projectTypes.size()	+ (definedTypes != null ? definedTypes.size() : 0) :
				0;

		IProjectType[] types = new IProjectType[size];
		
		int n = 0;
		for (int i = 0; i < projectTypes.size(); ++i)
			types[n++] = (IProjectType)projectTypes.get(i);
		
		if (definedTypes != null)
			for (int i = 0; i < definedTypes.size(); ++i)
				types[n++] = (IProjectType)definedTypes.get(i);
				
		return types;
	}
	
	/**
	 * Returns the project type with the passed in ID
	 * 
	 * @param String 
	 * @return IProjectType
	 */
	public static IProjectType getProjectType(String id) {
		try {
			// Make sure the extensions are loaded
			loadExtensions();
		} catch (BuildException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return (IProjectType)getExtensionProjectTypeMap().get(id);
	}
	
	public static Version getVersion(){
		return version;
	}
	
	protected static Map getExtensionDepCalcMap() {
		try {
			loadExtensions();
		} catch (BuildException e) {
		}
		if (depCalculatorsMap == null) {
			depCalculatorsMap = new HashMap();
		}
		return depCalculatorsMap;
	}
	
	/* (non-Javadoc)
	 * Safe accessor for the map of IDs to ProjectTypes
	 * 
	 * @return Map
	 */
	public static SortedMap getExtensionProjectTypeMap() {
		try {
			loadExtensions();
		} catch (BuildException e) {
		}
		if (projectTypeMap == null) {
			projectTypeMap = new TreeMap();
		}
		return projectTypeMap;
	}

	/* (non-Javadoc)
	 * Safe accessor for the map of IDs to Configurations
	 * 
	 * @return Map
	 */
	protected static Map getExtensionConfigurationMap() {
		if (extensionConfigurationMap == null) {
			extensionConfigurationMap = new HashMap();
		}
		return extensionConfigurationMap;
	}

	/* (non-Javadoc)
	 * Safe accessor for the map of IDs to Resource Configurations
	 * 
	 * @return Map
	 */
	protected static Map getExtensionResourceConfigurationMap() {
		if (extensionResourceConfigurationMap == null) {
			extensionResourceConfigurationMap = new HashMap();
		}
		return extensionResourceConfigurationMap;
	}

	/* (non-Javadoc)
	 * Safe accessor for the map of IDs to ToolChains
	 * 
	 * @return Map
	 */
	public static SortedMap getExtensionToolChainMap() {
		try {
			loadExtensions();
		} catch (BuildException e) {
		}

		if (extensionToolChainMap == null) {
			extensionToolChainMap =  new TreeMap();
		}
		return extensionToolChainMap;
	}

	/* (non-Javadoc)
	 * Safe accessor for the map of IDs to Tools
	 * 
	 * @return Map
	 */
	public static SortedMap getExtensionToolMap() {
		try {
			loadExtensions();
		} catch (BuildException e) {
		}
		if (extensionToolMap == null) {
			extensionToolMap = new TreeMap();
		}
		return extensionToolMap;
	}

	/* (non-Javadoc)
	 * Safe accessor for the map of IDs to TargetPlatforms
	 * 
	 * @return Map
	 */
	protected static Map getExtensionTargetPlatformMap() {
		if (extensionTargetPlatformMap == null) {
			extensionTargetPlatformMap = new HashMap();
		}
		return extensionTargetPlatformMap;
	}

	/* (non-Javadoc)
	 * Safe accessor for the map of IDs to Builders
	 * 
	 * @return Map
	 */
	public static SortedMap getExtensionBuilderMap() {
		try {
			loadExtensions();
		} catch (BuildException e) {
		}
		if (extensionBuilderMap == null) {
			extensionBuilderMap = new TreeMap();
		}
		return extensionBuilderMap;
	}

	/* (non-Javadoc)
	 * Safe accessor for the map of IDs to Options
	 * 
	 * @return Map
	 */
	protected static Map getExtensionOptionMap() {
		if (extensionOptionMap == null) {
			extensionOptionMap = new HashMap();
		}
		return extensionOptionMap;
	}

	/* (non-Javadoc)
	 * Safe accessor for the map of IDs to Option Categories
	 * 
	 * @return Map
	 */
	protected static Map getExtensionOptionCategoryMap() {
		if (extensionOptionCategoryMap == null) {
			extensionOptionCategoryMap = new HashMap();
		}
		return extensionOptionCategoryMap;
	}

	/* (non-Javadoc)
	 * Safe accessor for the map of IDs to InputTypes
	 * 
	 * @return Map
	 */
	protected static Map getExtensionInputTypeMap() {
		if (extensionInputTypeMap == null) {
			extensionInputTypeMap = new HashMap();
		}
		return extensionInputTypeMap;
	}

	/* (non-Javadoc)
	 * Safe accessor for the map of IDs to OutputTypes
	 * 
	 * @return Map
	 */
	protected static Map getExtensionOutputTypeMap() {
		if (extensionOutputTypeMap == null) {
			extensionOutputTypeMap = new HashMap();
		}
		return extensionOutputTypeMap;
	}

	/* (non-Javadoc)
	 * Safe accessor for the map of IDs to Targets (CDT V2.0 object model)
	 * 
	 * @return Map
	 */
	protected static Map getExtensionTargetMap() {
		if (extensionTargetMap == null) {
			extensionTargetMap = new HashMap();
		}
		return extensionTargetMap;
	}

	/**
	 * Returns the targets owned by this project.  If none are owned,
	 * an empty array is returned.
	 * 
	 * @param project
	 * @return
	 */
	public static ITarget[] getTargets(IResource resource) {
		IManagedBuildInfo buildInfo = getBuildInfo(resource);
		
		if (buildInfo != null) {
			List targets = buildInfo.getTargets();
			return (ITarget[])targets.toArray(new ITarget[targets.size()]);
		}
		return emptyTargets;
	}

	/**
	 * Returns the project type from the manifest with the ID specified in the argument
	 *  or <code>null</code>.
	 * 
	 * @param id
	 * @return IProjectType
	 */
	public static IProjectType getExtensionProjectType(String id) {
		try {
			// Make sure the extensions are loaded
			loadExtensions();
		} catch (BuildException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return (IProjectType) getExtensionProjectTypeMap().get(id);
	}

	/**
	 * Returns the configuration from the manifest with the ID specified in the argument
	 *  or <code>null</code>.
	 * 
	 * @param id
	 * @return IConfiguration
	 */
	public static IConfiguration getExtensionConfiguration(String id) {
		try {
			// Make sure the extensions are loaded
			loadExtensions();
		} catch (BuildException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return (IConfiguration) getExtensionConfigurationMap().get(id);
	}

	/**
	 * Returns the resource configuration from the manifest with the ID specified in the argument
	 *  or <code>null</code>.
	 * 
	 * @param id
	 * @return IResourceConfiguration
	 */
	public static IResourceConfiguration getExtensionResourceConfiguration(String id) {
		try {
			// Make sure the extensions are loaded
			loadExtensions();
		} catch (BuildException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return (IResourceConfiguration) getExtensionResourceConfigurationMap().get(id);
	}

	/**
	 * Returns the tool-chain from the manifest with the ID specified in the argument
	 *  or <code>null</code>.
	 * 
	 * @param id
	 * @return IToolChain
	 */
	public static IToolChain getExtensionToolChain(String id) {
		try {
			// Make sure the extensions are loaded
			loadExtensions();
		} catch (BuildException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return (IToolChain) getExtensionToolChainMap().get(id);
	}

	/**
	 * Returns the tool from the manifest with the ID specified in the argument
	 *  or <code>null</code>.
	 * 
	 * @param id
	 * @return ITool
	 */
	public static ITool getExtensionTool(String id) {
		try {
			// Make sure the extensions are loaded
			loadExtensions();
		} catch (BuildException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return (ITool) getExtensionToolMap().get(id);
	}

	/**
	 * Returns the target platform from the manifest with the ID specified in the argument
	 *  or <code>null</code>.
	 * 
	 * @param id
	 * @return ITargetPlatform
	 */
	public static ITargetPlatform getExtensionTargetPlatform(String id) {
		try {
			// Make sure the extensions are loaded
			loadExtensions();
		} catch (BuildException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return (ITargetPlatform) getExtensionTargetPlatformMap().get(id);
	}

	/**
	 * Returns the builder from the manifest with the ID specified in the argument
	 *  or <code>null</code>.
	 * 
	 * @param id
	 * @return IBuilder
	 */
	public static IBuilder getExtensionBuilder(String id) {
		try {
			// Make sure the extensions are loaded
			loadExtensions();
		} catch (BuildException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return (IBuilder) getExtensionBuilderMap().get(id);
	}
	
	public static IBuilder getExtensionBuilder(IBuilder builder) {
		for(;builder != null && !builder.isExtensionElement(); builder = builder.getSuperClass());
		return builder;
	}


	/**
	 * Returns the option from the manifest with the ID specified in the argument
	 *  or <code>null</code>.
	 * 
	 * @param id
	 * @return IOption
	 */
	public static IOption getExtensionOption(String id) {
		try {
			// Make sure the extensions are loaded
			loadExtensions();
		} catch (BuildException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return (IOption) getExtensionOptionMap().get(id);
	}

	/**
	 * Returns the InputType from the manifest with the ID specified in the argument
	 *  or <code>null</code>.
	 * 
	 * @param id
	 * @return IInputType
	 */
	public static IInputType getExtensionInputType(String id) {
		try {
			// Make sure the extensions are loaded
			loadExtensions();
		} catch (BuildException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return (IInputType) getExtensionInputTypeMap().get(id);
	}

	/**
	 * Returns the OutputType from the manifest with the ID specified in the argument
	 *  or <code>null</code>.
	 * 
	 * @param id
	 * @return IOutputType
	 */
	public static IOutputType getExtensionOutputType(String id) {
		try {
			// Make sure the extensions are loaded
			loadExtensions();
		} catch (BuildException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return (IOutputType) getExtensionOutputTypeMap().get(id);
	}

	/**
	 * Returns the target from the manifest with the ID specified in the argument
	 *  or <code>null</code> - CDT V2.0 object model.
	 * 
	 * @param id
	 * @return ITarget
	 */
	public static ITarget getExtensionTarget(String id) {
		try {
			// Make sure the extensions are loaded
			loadExtensions();
		} catch (BuildException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		return (ITarget) getExtensionTargetMap().get(id);
	}
	
	/**
	 * Answers the result of a best-effort search to find a target with the 
	 * specified ID, or <code>null</code> if one is not found.
	 * 
	 * @param resource
	 * @param id
	 * @return
	 */
	public static ITarget getTarget(IResource resource, String id) {
		ITarget target = null;
		// Check if the target is spec'd in the build info for the resource
		if (resource != null) {
			IManagedBuildInfo buildInfo = getBuildInfo(resource);
			if (buildInfo != null)
				target = buildInfo.getTarget(id);
		}
		// OK, check the extension map
		if (target == null) {
			target = (ITarget)getExtensionTargetMap().get(id);
		}
		return target;
	}

	/**
	 * Sets the default configuration for the project. Note that this will also
	 * update the default target if needed.
	 *  
	 * @param project
	 * @param newDefault
	 */
	public static void setDefaultConfiguration(IProject project, IConfiguration newDefault) {
		if (project == null || newDefault == null) {
			return;
		}
		// Set the default in build information for the project 
		IManagedBuildInfo info = getBuildInfo(project);
		if (info != null) {
			info.setDefaultConfiguration(newDefault);
		}
	}
	
	/**
	 * Sets the currently selected configuration.  This is used while the project
	 * property pages are displayed
	 * 
	 * @param project
	 * @param target
	 */
	public static void setSelectedConfiguration(IProject project, IConfiguration config) {
		if (project == null) {
			return;
		}
		// Set the default in build information for the project 
		IManagedBuildInfo info = getBuildInfo(project);
		if (info != null) {
			info.setSelectedConfiguration(config);
		}
	}

	/**
	 * @param targetId
	 * @return
	 */
	public static IManagedBuilderMakefileGenerator getBuildfileGenerator(IConfiguration config) {
		IToolChain toolChain = config.getToolChain();
		if(toolChain != null){
			IBuilder builder = toolChain.getBuilder();
			if(builder != null)
				return builder.getBuildFileGenerator();
		} 
		// If no generator is defined, return the default GNU generator
		return new GnuMakefileGenerator();
	}
	
	/**
	 * load tool provider defined or default (if not found) command line generator special for selected tool
	 * @param toolId - id selected id
	 * @return IManagedCommandLineGenerator
	 */
	public static IManagedCommandLineGenerator getCommandLineGenerator(IConfiguration config, String toolId) {
		ITool tool = config.getTool(toolId);
		if (tool != null) {
			return tool.getCommandLineGenerator();
		}
		return ManagedCommandLineGenerator.getCommandLineGenerator();
	}

    /** 
     * Targets may have a scanner config discovery profile defined that knows  
     * how to discover built-in compiler defines and includes search paths. 
     * Find the profile for the target specified.
     * 
     * @param string the unique id of the target to search for
     * @return scanner configuration discovery profile id
     */
    public static String getScannerInfoProfileId(IConfiguration config) {
        IToolChain toolChain = config.getToolChain();
        return toolChain.getScannerConfigDiscoveryProfileId();
    }
    
	/**
	 * Gets the currently selected target.  This is used while the project
	 * property pages are displayed
	 * 
	 * @param project
	 * @return target
	 */
	public static IConfiguration getSelectedConfiguration(IProject project) {
		if (project == null) {
			return null;
		}
		// Set the default in build information for the project 
		IManagedBuildInfo info = getBuildInfo(project);
		if (info != null) {
			return info.getSelectedConfiguration();
		}
		return null;
	}
	
	/* (non-Javadoc)
	 * 
	 * @param config
	 * @param option
	 */
	private static void notifyListeners(IConfiguration config, IOption option) {
		// Continue if change is something that effect the scanner
		try {
			//an option can be null in the case of calling this method from the environment
			//build path change listener
			if (config.isTemporary() ||
					(option != null && option.getValueType() != IOption.INCLUDE_PATH 
							&& option.getValueType() != IOption.PREPROCESSOR_SYMBOLS
							&& option.getValueType() != IOption.INCLUDE_FILES
							&& option.getValueType() != IOption.LIBRARY_PATHS
							&& option.getValueType() != IOption.LIBRARY_FILES
							&& option.getValueType() != IOption.MACRO_FILES
							&& option.getValueType() != IOption.UNDEF_INCLUDE_PATH
							&& option.getValueType() != IOption.UNDEF_PREPROCESSOR_SYMBOLS
							&& option.getValueType() != IOption.UNDEF_INCLUDE_FILES
							&& option.getValueType() != IOption.UNDEF_LIBRARY_PATHS
							&& option.getValueType() != IOption.UNDEF_LIBRARY_FILES
							&& option.getValueType() != IOption.UNDEF_MACRO_FILES
							)) {
				return;
			}
		} catch (BuildException e) {return;}
		
		// Figure out if there is a listener for this change
		IResource resource = config.getOwner();
		List listeners = (List) getBuildModelListeners().get(resource);
		if (listeners == null) {
			return;
		}
		ListIterator iter = listeners.listIterator();
		while (iter.hasNext()) {
			((IScannerInfoChangeListener)iter.next()).changeNotification(resource, (IScannerInfo)getBuildInfo(resource));
		}
	}

	public static void initializePathEntries(IConfiguration config, IOption option){
		try{
			if(config.isTemporary() ||
					(option != null
					&& option.getValueType() != IOption.INCLUDE_PATH
					&& option.getValueType() != IOption.PREPROCESSOR_SYMBOLS
					&& option.getValueType() != IOption.INCLUDE_FILES
					&& option.getValueType() != IOption.LIBRARY_PATHS
					&& option.getValueType() != IOption.LIBRARY_FILES
					&& option.getValueType() != IOption.MACRO_FILES
					&& option.getValueType() != IOption.UNDEF_INCLUDE_PATH
					&& option.getValueType() != IOption.UNDEF_PREPROCESSOR_SYMBOLS
					&& option.getValueType() != IOption.UNDEF_INCLUDE_FILES
					&& option.getValueType() != IOption.UNDEF_LIBRARY_PATHS
					&& option.getValueType() != IOption.UNDEF_LIBRARY_FILES
					&& option.getValueType() != IOption.UNDEF_MACRO_FILES
					))
				return;
		} catch (BuildException e){
			return;
		}
		
		try {
			updateCoreSettings(config);
		} catch (CoreException e) {
		}
			
	}
	
	public static void initializePathEntries(IResourceConfiguration resConfig, IOption option){
		IConfiguration cfg = resConfig.getParent();
		if(cfg != null)
			initializePathEntries(cfg,option);
	}
	
	private static void notifyListeners(IResourceInfo resConfig, IOption option) {
		// Continue if change is something that effect the scanreser
		try {
			if (resConfig.getParent().isTemporary() ||
					(option != null && option.getValueType() != IOption.INCLUDE_PATH 
				&& option.getValueType() != IOption.PREPROCESSOR_SYMBOLS
				&& option.getValueType() != IOption.INCLUDE_FILES
				&& option.getValueType() != IOption.LIBRARY_PATHS
				&& option.getValueType() != IOption.LIBRARY_FILES
				&& option.getValueType() != IOption.MACRO_FILES
				&& option.getValueType() != IOption.UNDEF_INCLUDE_PATH
				&& option.getValueType() != IOption.UNDEF_PREPROCESSOR_SYMBOLS
				&& option.getValueType() != IOption.UNDEF_INCLUDE_FILES
				&& option.getValueType() != IOption.UNDEF_LIBRARY_PATHS
				&& option.getValueType() != IOption.UNDEF_LIBRARY_FILES
				&& option.getValueType() != IOption.UNDEF_MACRO_FILES
					)) {
				return;
			}
		} catch (BuildException e) {return;}
		
		// Figure out if there is a listener for this change
		IResource resource = resConfig.getParent().getOwner();
		List listeners = (List) getBuildModelListeners().get(resource);
		if (listeners == null) {
			return;
		}
		ListIterator iter = listeners.listIterator();
		while (iter.hasNext()) {
			((IScannerInfoChangeListener)iter.next()).changeNotification(resource, (IScannerInfo)getBuildInfo(resource));
		}
	}

	/**
	 * Adds the version of the managed build system to the project 
	 * specified in the argument.
	 * 
	 * @param newProject the project to version
	 */
	public static void setNewProjectVersion(IProject newProject) {
		// Get the build info for the argument
		ManagedBuildInfo info = findBuildInfo(newProject, true);
		if(info != null)
			info.setVersion(buildInfoVersion.toString());		
	}

	/**
	 * Set the boolean value for an option for a given config.
	 * 
	 * @param config The configuration the option belongs to.
	 * @param holder The holder/parent of the option.
	 * @param option The option to set the value for.
	 * @param value The boolean that the option should contain after the change.
	 * 
	 * @return IOption The modified option.  This can be the same option or a newly created option.
	 * 
	 * @since 3.0 - The type and name of the <code>ITool tool</code> parameter
	 *        has changed to <code>IHoldsOptions holder</code>. Client code
	 *        assuming <code>ITool</code> as type, will continue to work unchanged.
	 */
	public static IOption setOption(IConfiguration config, IHoldsOptions holder, IOption option, boolean value) {
		IOption retOpt;
		try {
			// Request a value change and set dirty if real change results
			retOpt = config.setOption(holder, option, value);
			if (retOpt.getValueHandler().handleValue(
					config, 
					holder, 
					retOpt,
					retOpt.getValueHandlerExtraArgument(), 
					IManagedOptionValueHandler.EVENT_APPLY)) {
				// TODO : Event is handled successfully and returned true.
				// May need to do something here say log a message.
			} else {
				// Event handling Failed. 
			} 
//			initializePathEntries(config,retOpt);
//			notifyListeners(config, retOpt);
		} catch (BuildException e) {
			return null;
		}
		return retOpt;
	}

	/**
	 * Set the boolean value for an option for a given config.
	 * 
	 * @param resConfig The resource configuration the option belongs to.
	 * @param holder The holder/parent of the option.
	 * @param option The option to set the value for.
	 * @param value The boolean that the option should contain after the change.
	 * 
	 * @return IOption The modified option.  This can be the same option or a newly created option.
	 * 
	 * @since 3.0 - The type and name of the <code>ITool tool</code> parameter
	 *        has changed to <code>IHoldsOptions holder</code>. Client code
	 *        assuming <code>ITool</code> as type, will continue to work unchanged.
	 */
	public static IOption setOption(IResourceInfo resConfig, IHoldsOptions holder, IOption option, boolean value) {
		IOption retOpt;
		try {
			// Request a value change and set dirty if real change results
			retOpt = resConfig.setOption(holder, option, value);
			if (retOpt.getValueHandler().handleValue(
					resConfig, 
					holder, 
					retOpt,
					retOpt.getValueHandlerExtraArgument(), 
					IManagedOptionValueHandler.EVENT_APPLY)) {
				// TODO : Event is handled successfully and returned true.
				// May need to do something here say log a message.
			} else {
				// Event handling Failed. 
			} 
	//		initializePathEntries(resConfig,retOpt);
			notifyListeners(resConfig, retOpt);
		} catch (BuildException e) {
			return null;
		}
		return retOpt;
	}
	/**
	 * Set the string value for an option for a given config.
	 * 
	 * @param config The configuration the option belongs to.
	 * @param holder The holder/parent of the option.
	 * @param option The option to set the value for.
	 * @param value The value that the option should contain after the change.
	 * 
	 * @return IOption The modified option.  This can be the same option or a newly created option.
	 * 
	 * @since 3.0 - The type and name of the <code>ITool tool</code> parameter
	 *        has changed to <code>IHoldsOptions holder</code>. Client code
	 *        assuming <code>ITool</code> as type, will continue to work unchanged.
	 */
	public static IOption setOption(IConfiguration config, IHoldsOptions holder, IOption option, String value) {
		IOption retOpt;
		try {
			retOpt = config.setOption(holder, option, value);
			if (retOpt.getValueHandler().handleValue(
					config, 
					holder, 
					retOpt,
					retOpt.getValueHandlerExtraArgument(), 
					IManagedOptionValueHandler.EVENT_APPLY)) {
				// TODO : Event is handled successfully and returned true.
				// May need to do something here say log a message.
			} else {
				// Event handling Failed. 
			} 
//			initializePathEntries(config,retOpt);
//			notifyListeners(config, retOpt);
		} catch (BuildException e) {
			return null;
		}
		return retOpt;
	}
	
	/**
	 * Set the string value for an option for a given  resource config.
	 * 
	 * @param resConfig The resource configuration the option belongs to.
	 * @param holder The holder/parent of the option.
	 * @param option The option to set the value for.
	 * @param value The value that the option should contain after the change.
	 * 
	 * @return IOption The modified option.  This can be the same option or a newly created option.
	 * 
	 * @since 3.0 - The type and name of the <code>ITool tool</code> parameter
	 *        has changed to <code>IHoldsOptions holder</code>. Client code
	 *        assuming <code>ITool</code> as type, will continue to work unchanged.
	 */
	public static IOption setOption(IResourceInfo resConfig, IHoldsOptions holder, IOption option, String value) {
		IOption retOpt;
		try {
			retOpt = resConfig.setOption(holder, option, value);
			if (retOpt.getValueHandler().handleValue(
					resConfig, 
					holder, 
					retOpt,
					retOpt.getValueHandlerExtraArgument(), 
					IManagedOptionValueHandler.EVENT_APPLY)) {
				// TODO : Event is handled successfully and returned true.
				// May need to do something here say log a message.
			} else {
				// Event handling Failed. 
			} 
	//		initializePathEntries(resConfig,retOpt);
			notifyListeners(resConfig, retOpt);
		} catch (BuildException e) {
			return null;
		}
		return retOpt;
	}
/**
	 * Set the string array value for an option for a given config.
	 * 
	 * @param config The configuration the option belongs to.
	 * @param holder The holder/parent of the option.
	 * @param option The option to set the value for.
	 * @param value The values the option should contain after the change.
	 * 
	 * @return IOption The modified option.  This can be the same option or a newly created option.
	 * 
	 * @since 3.0 - The type and name of the <code>ITool tool</code> parameter
	 *        has changed to <code>IHoldsOptions holder</code>. Client code
	 *        assuming <code>ITool</code> as type, will continue to work unchanged.
	 */
	public static IOption setOption(IConfiguration config, IHoldsOptions holder, IOption option, String[] value) {
		IOption retOpt;
		try {
			retOpt = config.setOption(holder, option, value);
			if (retOpt.getValueHandler().handleValue(
					config, 
					holder, 
					retOpt,
					retOpt.getValueHandlerExtraArgument(), 
					IManagedOptionValueHandler.EVENT_APPLY)) {
				// TODO : Event is handled successfully and returned true.
				// May need to do something here say log a message.
			} else {
				// Event handling Failed. 
			} 
//			initializePathEntries(config,retOpt);
//			notifyListeners(config, retOpt);				
		} catch (BuildException e) {
			return null;
		}
		return retOpt;
	}

	/**
	 * Set the string array value for an option for a given resource config.
	 * 
	 * @param resConfig The resource configuration the option belongs to.
	 * @param holder The holder/parent of the option.
	 * @param option The option to set the value for.
	 * @param value The values the option should contain after the change.
	 * 
	 * @return IOption The modified option.  This can be the same option or a newly created option.
	 * 
	 * @since 3.0 - The type and name of the <code>ITool tool</code> parameter
	 *        has changed to <code>IHoldsOptions holder</code>. Client code
	 *        assuming <code>ITool</code> as type, will continue to work unchanged.
	 */
	public static IOption setOption(IResourceInfo resConfig, IHoldsOptions holder, IOption option, String[] value) {
		IOption retOpt;
		try {
			retOpt = resConfig.setOption(holder, option, value);
			if (retOpt.getValueHandler().handleValue(
					resConfig, 
					holder, 
					retOpt,
					retOpt.getValueHandlerExtraArgument(), 
					IManagedOptionValueHandler.EVENT_APPLY)) {
				// TODO : Event is handled successfully and returned true.
				// May need to do something here say log a message.
			} else {
				// Event handling Failed. 
			} 
//			initializePathEntries(resConfig,retOpt);
			notifyListeners(resConfig, retOpt);				
		} catch (BuildException e) {
			return null;
		}
		return retOpt;
	}
	
	public static IOption setOption(IResourceInfo resConfig, IHoldsOptions holder, IOption option, OptionStringValue[] value) {
		IOption retOpt;
		try {
			retOpt = resConfig.setOption(holder, option, value);
			if (retOpt.getValueHandler().handleValue(
					resConfig, 
					holder, 
					retOpt,
					retOpt.getValueHandlerExtraArgument(), 
					IManagedOptionValueHandler.EVENT_APPLY)) {
				// TODO : Event is handled successfully and returned true.
				// May need to do something here say log a message.
			} else {
				// Event handling Failed. 
			} 
//			initializePathEntries(resConfig,retOpt);
			notifyListeners(resConfig, retOpt);				
		} catch (BuildException e) {
			return null;
		}
		return retOpt;
	}

	/**
	 * @param config
	 * @param tool
	 * @param command
	 */
	public static void setToolCommand(IConfiguration config, ITool tool, String command) {
		// The tool may be a reference.
		if (tool instanceof IToolReference) {
			// If so, just set the command in the reference
			((IToolReference)tool).setToolCommand(command);
		} else {
			config.setToolCommand(tool, command);
		}
	}

	public static void setToolCommand(IResourceConfiguration resConfig, ITool tool, String command) {
		// The tool may be a reference.
		if (tool instanceof IToolReference) {
			// If so, just set the command in the reference
			((IToolReference)tool).setToolCommand(command);
		} else {
			resConfig.setToolCommand(tool, command);
		}
	}

	public static boolean saveBuildInfoLegacy(IProject project, boolean force) {
		// Create document
		Exception err = null;
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.newDocument();
			
			// Get the build information for the project
			ManagedBuildInfo buildInfo = (ManagedBuildInfo) getBuildInfo(project);

			// Save the build info
			if (buildInfo != null && 
					!buildInfo.isReadOnly() &&
					buildInfo.isValid() &&
					(force == true || buildInfo.isDirty())) {
				// For post-2.0 projects, there will be a version
				String projectVersion = buildInfo.getVersion();
				if (projectVersion != null) {
					ProcessingInstruction instruction = doc.createProcessingInstruction(VERSION_ELEMENT_NAME, projectVersion);
					doc.appendChild(instruction);
				}
				Element rootElement = doc.createElement(ROOT_NODE_NAME);	
				doc.appendChild(rootElement);
				buildInfo.serializeLegacy(doc, rootElement);
		
				// Transform the document to something we can save in a file
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.METHOD, "xml");	//$NON-NLS-1$
				transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");	//$NON-NLS-1$
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(stream);
				transformer.transform(source, result);
				
				// Save the document
				IFile projectFile = project.getFile(SETTINGS_FILE_NAME);
				String utfString = stream.toString("UTF-8");	//$NON-NLS-1$

				if (projectFile.exists()) {
					if (projectFile.isReadOnly()) {						
						// If we are not running headless, and there is a UI Window around, grab it
						// and the associated shell
						IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
						if (window == null) {
							IWorkbenchWindow windows[] = PlatformUI.getWorkbench().getWorkbenchWindows();
							window = windows[0];
						}
						Shell shell = null; 
						if (window != null) {							
							shell = window.getShell();
						}						
	                    // Inform Eclipse that we are intending to modify this file
						// This will provide the user the opportunity, via UI prompts, to fetch the file from source code control
						// reset a read-only file protection to write etc.
						// If there is no shell, i.e. shell is null, then there will be no user UI interaction
						IStatus status = projectFile.getWorkspace().validateEdit(new IFile[]{projectFile}, shell);
						// If the file is still read-only, then we should not attempt the write, since it will
						// just fail - just throw an exception, to be caught below, and inform the user
						// For other non-successful status, we take our chances, attempt the write, and pass
						// along any exception thrown
						if (!status.isOK()) {
						    if (status.getCode() == IResourceStatus.READ_ONLY_LOCAL) {
						    	stream.close();
		    	                throw new IOException(ManagedMakeMessages.getFormattedString(MANIFEST_ERROR_READ_ONLY, projectFile.getFullPath().toString())); //$NON-NLS-1$						
						    }
						}
					}
					projectFile.setContents(new ByteArrayInputStream(utfString.getBytes("UTF-8")), IResource.FORCE, new NullProgressMonitor());	//$NON-NLS-1$
				} else {
					projectFile.create(new ByteArrayInputStream(utfString.getBytes("UTF-8")), IResource.FORCE, new NullProgressMonitor());	//$NON-NLS-1$
				}

				// Close the streams
				stream.close();
			}
		} catch (ParserConfigurationException e) {
			err = e;
		} catch (FactoryConfigurationError e) {
			err = e.getException();
		} catch (TransformerConfigurationException e) {
			err = e;
		} catch (TransformerFactoryConfigurationError e) {
			err = e.getException();
		} catch (TransformerException e) {
			err = e;
		} catch (IOException e) {
			// The save failed
			err = e;
    	} catch (CoreException e) {
	    	// Save to IFile failed
		    err = e;
	    }

		if (err != null) {			
			// Put out an error message indicating that the attempted write to the .cdtbuild project file failed					
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window == null) {
				IWorkbenchWindow windows[] = PlatformUI.getWorkbench().getWorkbenchWindows();
				window = windows[0];
			}

			final Shell shell = window.getShell();
			if (shell != null) {
				final String exceptionMsg = err.getMessage(); 
				shell.getDisplay().syncExec( new Runnable() {
					public void run() {
						MessageDialog.openError(shell, 
								ManagedMakeMessages.getResourceString("ManagedBuildManager.error.write_failed_title"),	//$NON-NLS-1$
								ManagedMakeMessages.getFormattedString(MANIFEST_ERROR_WRITE_FAILED,		//$NON-NLS-1$
										exceptionMsg));
					}
			    } );
			}
		}
		// If we return an honest status when the operation fails, there are instances when the UI behavior
		// is not very good
		// Specifically, if "OK" is clicked by the user from the property page UI, and the return status 
		// from this routine is false, the property page UI will not be closed (note: this is Eclispe code) and
		// the OK button will simply be grayed out
		// At this point, the only way out is to click "Cancel" to get the UI to go away; note however that any
		// property page changes will be sticky, in the UI, which is nonintuitive and confusing
		// Therefore, just always return success, i.e. true, from this routine
		return true;
	}

	public static boolean saveBuildInfo(final IProject project, final boolean force) {
		try {
			return updateBuildInfo(project, force);
		} catch (CoreException e) {
			Throwable cause = e.getStatus().getException();
			if(cause instanceof IllegalArgumentException){
				//can not acquire the root rule
				Job j = new Job("save build info job"){
	
					protected IStatus run(IProgressMonitor monitor) {
						try {
							updateBuildInfo(project, force);
						} catch (CoreException e) {
							return e.getStatus();
						}
						return Status.OK_STATUS;
					}
					
				};
				j.setRule(ResourcesPlugin.getWorkspace().getRoot());
				j.setSystem(true);
				j.schedule();
				return true;
			}
			ManagedBuilderCorePlugin.log(e);
			return false;
		}
	}

	/**
	 * Saves the build information associated with a project and all resources
	 * in the project to the build info file.
	 * 
	 * @param project
	 * @param force 
	 */
	private static boolean updateBuildInfo(IProject project, boolean force) throws CoreException {
		IManagedBuildInfo info = getBuildInfo(project, false);
		if(info == null)
			return true;

		ICProjectDescription projDes = CoreModel.getDefault().getProjectDescription(project);
		projDes = BuildSettingsUtil.synchBuildInfo(info, projDes, force);

//		try {
			BuildSettingsUtil.checkApplyDescription(project, projDes);
//		} catch (CoreException e) {
//			return false;
//		}
		return true;
		/*
		// Create document
		Exception err = null;
		try {
			DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document doc = builder.newDocument();
			
			// Get the build information for the project
			ManagedBuildInfo buildInfo = (ManagedBuildInfo) getBuildInfo(project);

			// Save the build info
			if (buildInfo != null && 
					!buildInfo.isReadOnly() &&
					buildInfo.isValid() &&
					(force == true || buildInfo.isDirty())) {
				// For post-2.0 projects, there will be a version
				String projectVersion = buildInfo.getVersion();
				if (projectVersion != null) {
					ProcessingInstruction instruction = doc.createProcessingInstruction(VERSION_ELEMENT_NAME, projectVersion);
					doc.appendChild(instruction);
				}
				Element rootElement = doc.createElement(ROOT_NODE_NAME);	
				doc.appendChild(rootElement);
				buildInfo.serialize(doc, rootElement);
		
				// Transform the document to something we can save in a file
				ByteArrayOutputStream stream = new ByteArrayOutputStream();
				Transformer transformer = TransformerFactory.newInstance().newTransformer();
				transformer.setOutputProperty(OutputKeys.METHOD, "xml");	//$NON-NLS-1$
				transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8"); //$NON-NLS-1$
				transformer.setOutputProperty(OutputKeys.INDENT, "yes");	//$NON-NLS-1$
				DOMSource source = new DOMSource(doc);
				StreamResult result = new StreamResult(stream);
				transformer.transform(source, result);
				
				// Save the document
				IFile projectFile = project.getFile(SETTINGS_FILE_NAME);
				String utfString = stream.toString("UTF-8");	//$NON-NLS-1$

				if (projectFile.exists()) {
					if (projectFile.isReadOnly()) {						
						// If we are not running headless, and there is a UI Window around, grab it
						// and the associated shell
						IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
						if (window == null) {
							IWorkbenchWindow windows[] = PlatformUI.getWorkbench().getWorkbenchWindows();
							window = windows[0];
						}
						Shell shell = null; 
						if (window != null) {							
							shell = window.getShell();
						}						
	                    // Inform Eclipse that we are intending to modify this file
						// This will provide the user the opportunity, via UI prompts, to fetch the file from source code control
						// reset a read-only file protection to write etc.
						// If there is no shell, i.e. shell is null, then there will be no user UI interaction
						IStatus status = projectFile.getWorkspace().validateEdit(new IFile[]{projectFile}, shell);
						// If the file is still read-only, then we should not attempt the write, since it will
						// just fail - just throw an exception, to be caught below, and inform the user
						// For other non-successful status, we take our chances, attempt the write, and pass
						// along any exception thrown
						if (!status.isOK()) {
						    if (status.getCode() == IResourceStatus.READ_ONLY_LOCAL) {
						    	stream.close();
		    	                throw new IOException(ManagedMakeMessages.getFormattedString(MANIFEST_ERROR_READ_ONLY, projectFile.getFullPath().toString())); //$NON-NLS-1$						
						    }
						}
					}
					projectFile.setContents(new ByteArrayInputStream(utfString.getBytes("UTF-8")), IResource.FORCE, new NullProgressMonitor());	//$NON-NLS-1$
				} else {
					projectFile.create(new ByteArrayInputStream(utfString.getBytes("UTF-8")), IResource.FORCE, new NullProgressMonitor());	//$NON-NLS-1$
				}

				// Close the streams
				stream.close();
			}
		} catch (ParserConfigurationException e) {
			err = e;
		} catch (FactoryConfigurationError e) {
			err = e.getException();
		} catch (TransformerConfigurationException e) {
			err = e;
		} catch (TransformerFactoryConfigurationError e) {
			err = e.getException();
		} catch (TransformerException e) {
			err = e;
		} catch (IOException e) {
			// The save failed
			err = e;
    	} catch (CoreException e) {
	    	// Save to IFile failed
		    err = e;
	    }

		if (err != null) {			
			// Put out an error message indicating that the attempted write to the .cdtbuild project file failed					
			IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
			if (window == null) {
				IWorkbenchWindow windows[] = PlatformUI.getWorkbench().getWorkbenchWindows();
				window = windows[0];
			}

			final Shell shell = window.getShell();
			if (shell != null) {
				final String exceptionMsg = err.getMessage(); 
				shell.getDisplay().syncExec( new Runnable() {
					public void run() {
						MessageDialog.openError(shell, 
								ManagedMakeMessages.getResourceString("ManagedBuildManager.error.write_failed_title"),	//$NON-NLS-1$
								ManagedMakeMessages.getFormattedString(MANIFEST_ERROR_WRITE_FAILED,		//$NON-NLS-1$
										exceptionMsg));
					}
			    } );
			}
		}
		// If we return an honest status when the operation fails, there are instances when the UI behavior
		// is not very good
		// Specifically, if "OK" is clicked by the user from the property page UI, and the return status 
		// from this routine is false, the property page UI will not be closed (note: this is Eclispe code) and
		// the OK button will simply be grayed out
		// At this point, the only way out is to click "Cancel" to get the UI to go away; note however that any
		// property page changes will be sticky, in the UI, which is nonintuitive and confusing
		// Therefore, just always return success, i.e. true, from this routine
		return true;
		*/
	}
	
	public static void updateCoreSettings(IProject project) throws CoreException {
		updateBuildInfo(project, true);
	}
	
	public static void updateCoreSettings(IConfiguration cfg) throws CoreException{
		IProject project = cfg.getOwner().getProject();
		ICProjectDescription projDes = CoreModel.getDefault().getProjectDescription(project);
		if(projDes != null){
			if(BuildSettingsUtil.applyConfiguration(cfg, projDes, true)){
				BuildSettingsUtil.checkApplyDescription(project, projDes);
			}
		}
	}
	public static void updateCoreSettings(IProject project, IConfiguration[] cfgs) throws CoreException{
		updateCoreSettings(project, cfgs, false);
	}
	
	public static void updateCoreSettings(IProject project, IConfiguration[] cfgs, boolean avoidSerialization) throws CoreException{
		if(cfgs == null){
			IManagedBuildInfo info = getBuildInfo(project);
			if(info != null && info.isValid() && info.getManagedProject() != null)
				cfgs = info.getManagedProject().getConfigurations(); 
		}
		
		if(cfgs == null || cfgs.length == 0)
			return;
		
		ICProjectDescription projDes = CoreModel.getDefault().getProjectDescription(project);
		boolean updated = false;
		if(projDes != null){
			for(int i = 0; i < cfgs.length; i++){
				if(BuildSettingsUtil.applyConfiguration(cfgs[i], projDes, true)){
					updated = true;
				}
			}
			if(updated){
				BuildSettingsUtil.checkApplyDescription(project, projDes, avoidSerialization);
			}
		}
	}		
	
	/**
	 * @param resource
	 */
	public static void removeBuildInfo(IResource resource) {
		/*
		IManagedBuildInfo info = findBuildInfo(resource, false);
		if(info != null){
			IConfiguration[] configs = info.getManagedProject().getConfigurations();
			//  Send an event to each configuration and if they exist, its resource configurations
			for (int i=0; i < configs.length; ++i) {
				ManagedBuildManager.performValueHandlerEvent(configs[i], IManagedOptionValueHandler.EVENT_CLOSE);
			}

			info.setValid(false);
			
			try {
				resource.setSessionProperty(buildInfoProperty, null);
			} catch (CoreException e) {
			}
		}
		*/
	}

	/**
	 * Resets the build information for the project and configuration specified in the arguments. 
	 * The build information will contain the settings defined in the plugin manifest. 
	 * 
	 * @param project
	 * @param configuration
	 */
	public static void resetConfiguration(IProject project, IConfiguration configuration) {
		// reset the configuration
		((Configuration)configuration).reset();

		performValueHandlerEvent(configuration, 
				IManagedOptionValueHandler.EVENT_SETDEFAULT, false);

	}

	public static void resetResourceConfiguration(IProject project, IResourceConfiguration resConfig) {
		// reset the configuration
		((ResourceConfiguration) resConfig).reset();
		
		performValueHandlerEvent(resConfig, 
				IManagedOptionValueHandler.EVENT_SETDEFAULT);

	}
	
	public static void resetOptionSettings(IResourceInfo rcInfo){
		if(rcInfo instanceof IFileInfo){
			Configuration cfg = (Configuration)rcInfo.getParent();
			IProject project;
			if(cfg.isExtensionElement() || cfg.isPreference())
				project = null;
			else
				project = cfg.getOwner().getProject();
			
			resetResourceConfiguration(project, (IFileInfo)rcInfo);
		} else {
			FolderInfo fo = (FolderInfo)rcInfo;
			fo.resetOptionSettings();
		}
	}
	/**
	 * Adds a ProjectType that is is specified in the manifest to the 
	 * build system. It is available to any element that 
	 * has a reference to it as part of its description.
	 *  
	 * @param projectType 
	 */
	public static void addExtensionProjectType(ProjectType projectType) {
		if (projectTypes == null) {
			projectTypes = new ArrayList();
		}
		
		projectTypes.add(projectType);
		Object previous = getExtensionProjectTypeMap().put(projectType.getId(), projectType);
		if (previous != null) {
			// Report error
			ManagedBuildManager.OutputDuplicateIdError(
					"ProjectType",	//$NON-NLS-1$
					projectType.getId());			
		}
	}
	
	/**
	 * Adds a Configuration that is is specified in the manifest to the 
	 * build system. It is available to any element that 
	 * has a reference to it as part of its description.
	 *  
	 * @param configuration 
	 */
	public static void addExtensionConfiguration(Configuration configuration) {
		Object previous = getExtensionConfigurationMap().put(configuration.getId(), configuration);
		if (previous != null) {
			// Report error
			ManagedBuildManager.OutputDuplicateIdError(
					"Configuration",	//$NON-NLS-1$
					configuration.getId());			
		}
	}
	
	/**
	 * Adds a Resource Configuration that is is specified in the manifest to the 
	 * build system. It is available to any element that 
	 * has a reference to it as part of its description.
	 *  
	 * @param resourceConfiguration 
	 */
	public static void addExtensionResourceConfiguration(ResourceConfiguration resourceConfiguration) {
		Object previous = getExtensionResourceConfigurationMap().put(resourceConfiguration.getId(), resourceConfiguration);
		if (previous != null) {
			// Report error
			ManagedBuildManager.OutputDuplicateIdError(
					"ResourceConfiguration",	//$NON-NLS-1$
					resourceConfiguration.getId());			
		}
	}
	
	/**
	 * Adds a ToolChain that is is specified in the manifest to the 
	 * build system. It is available to any element that 
	 * has a reference to it as part of its description.
	 *  
	 * @param toolChain 
	 */
	public static void addExtensionToolChain(ToolChain toolChain) {
		Object previous = getExtensionToolChainMap().put(toolChain.getId(), toolChain);
		if (previous != null) {
			// Report error
			ManagedBuildManager.OutputDuplicateIdError(
					"ToolChain",	//$NON-NLS-1$
					toolChain.getId());			
		}
	}
		
	/**
	 * Adds a tool that is is specified in the manifest to the 
	 * build system. This tool is available to any target that 
	 * has a reference to it as part of its description. This 
	 * permits a tool that is common to many targets to be defined 
	 * only once.   
	 *  
	 * @param tool
	 */
	public static void addExtensionTool(Tool tool) {
		Object previous = getExtensionToolMap().put(tool.getId(), tool);
		if (previous != null) {
			// Report error
			ManagedBuildManager.OutputDuplicateIdError(
					"Tool",	//$NON-NLS-1$
					tool.getId());			
		}
	}
	
	/**
	 * Adds a TargetPlatform that is is specified in the manifest to the 
	 * build system. It is available to any element that 
	 * has a reference to it as part of its description.
	 *  
	 * @param targetPlatform 
	 */
	public static void addExtensionTargetPlatform(TargetPlatform targetPlatform) {
		Object previous = getExtensionTargetPlatformMap().put(targetPlatform.getId(), targetPlatform);
		if (previous != null) {
			// Report error
			ManagedBuildManager.OutputDuplicateIdError(
					"TargetPlatform",	//$NON-NLS-1$
					targetPlatform.getId());			
		}
	}
	
	/**
	 * Adds a Builder that is is specified in the manifest to the 
	 * build system. It is available to any element that 
	 * has a reference to it as part of its description.
	 *  
	 * @param Builder 
	 */
	public static void addExtensionBuilder(Builder builder) {
		Object previous = getExtensionBuilderMap().put(builder.getId(), builder);
		if (previous != null) {
			// Report error
			ManagedBuildManager.OutputDuplicateIdError(
					"Builder",	//$NON-NLS-1$
					builder.getId());			
		}
	}
	
	/**
	 * Adds a Option that is is specified in the manifest to the 
	 * build system. It is available to any element that 
	 * has a reference to it as part of its description.
	 *  
	 * @param option 
	 */
	public static void addExtensionOption(Option option) {
		Object previous = getExtensionOptionMap().put(option.getId(), option);
		if (previous != null) {
			// Report error
			ManagedBuildManager.OutputDuplicateIdError(
					"Option",	//$NON-NLS-1$
					option.getId());			
		}
	}
	
	/**
	 * Adds a OptionCategory that is is specified in the manifest to the 
	 * build system. It is available to any element that 
	 * has a reference to it as part of its description.
	 *  
	 * @param optionCategory 
	 */
	public static void addExtensionOptionCategory(OptionCategory optionCategory) {
		Object previous = getExtensionOptionCategoryMap().put(optionCategory.getId(), optionCategory);
		if (previous != null) {
			// Report error
			ManagedBuildManager.OutputDuplicateIdError(
					"OptionCategory",	//$NON-NLS-1$
					optionCategory.getId());			
		}
	}
	
	/**
	 * Adds an InputType that is is specified in the manifest to the 
	 * build system. It is available to any element that 
	 * has a reference to it as part of its description.
	 *  
	 * @param inputType 
	 */
	public static void addExtensionInputType(InputType inputType) {
		Object previous = getExtensionInputTypeMap().put(inputType.getId(), inputType);
		if (previous != null) {
			// Report error
			ManagedBuildManager.OutputDuplicateIdError(
					"InputType",	//$NON-NLS-1$
					inputType.getId());			
		}
	}
	
	/**
	 * Adds an OutputType that is is specified in the manifest to the 
	 * build system. It is available to any element that 
	 * has a reference to it as part of its description.
	 *  
	 * @param outputType 
	 */
	public static void addExtensionOutputType(OutputType outputType) {
		Object previous = getExtensionOutputTypeMap().put(outputType.getId(), outputType);
		if (previous != null) {
			// Report error
			ManagedBuildManager.OutputDuplicateIdError(
					"OutputType",	//$NON-NLS-1$
					outputType.getId());			
		}
	}
	
	/**
	 * Adds a Target that is is specified in the manifest to the 
	 * build system. It is available to any CDT 2.0 object model element that 
	 * has a reference to it as part of its description.
	 *  
	 * @param target
	 */
	public static void addExtensionTarget(Target target) {
		getExtensionTargetMap().put(target.getId(), target);
	}
	
	/**
	 * Creates a new project instance for the resource based on the parent project type.
	 * 
	 * @param resource
	 * @param parentTarget
	 * @return new <code>ITarget</code> with settings based on the parent passed in the arguments
	 * @throws BuildException
	 */
	public static IManagedProject createManagedProject(IResource resource, IProjectType parent)
		throws BuildException
	{
		return new ManagedProject(resource, parent);
	}
	
	/**
	 * Creates a new target for the resource based on the parentTarget.
	 * 
	 * @param resource
	 * @param parentTarget
	 * @return new <code>ITarget</code> with settings based on the parent passed in the arguments
	 * @throws BuildException
	 */
	public static ITarget createTarget(IResource resource, ITarget parentTarget)
		throws BuildException
	{
		IResource owner = parentTarget.getOwner();
		
		if (owner != null && owner.equals(resource))
			// Already added
			return parentTarget; 
			
		if (resource instanceof IProject) {
			// Must be an extension target
			if (owner != null)
				throw new BuildException(ManagedMakeMessages.getResourceString("ManagedBuildManager.error.owner_not_null")); //$NON-NLS-1$
		} else {
			// Owner must be owned by the project containing this resource
			if (owner == null)
				throw new BuildException(ManagedMakeMessages.getResourceString("ManagedBuildManager.error.null_owner")); //$NON-NLS-1$
			if (!owner.equals(resource.getProject()))
				throw new BuildException(ManagedMakeMessages.getResourceString("ManagedBuildManager.error.owner_not_project")); //$NON-NLS-1$
		}
		
		// Passed validation so create the target.
		return new Target(resource, parentTarget);
	}
	
	/**
	 * @param resource
	 * @return
	 */
	public static IStatus initBuildInfoContainer(IResource resource) {
		return Status.OK_STATUS;
		/*
		ManagedBuildInfo buildInfo = null;

		// Get the build info associated with this project for this session
		try {
			buildInfo = findBuildInfo(resource.getProject(), true);
			initBuildInfoContainer(buildInfo);
		} catch (CoreException e) {
			return new Status(IStatus.ERROR, 
				ManagedBuilderCorePlugin.getUniqueIdentifier(), 
				IStatus.ERROR, 
				e.getLocalizedMessage(), 
				e);
		}
		return new Status(IStatus.OK, 
			ManagedBuilderCorePlugin.getUniqueIdentifier(), 
			IStatus.OK, 
			ManagedMakeMessages.getFormattedString("ManagedBuildInfo.message.init.ok", resource.getName()),	//$NON-NLS-1$ 
			null);
			*/
	}
	
	/* (non-Javadoc)
	 * Private helper method to intialize the path entry container once and 
	 * only once when the build info is first loaded or created.
	 * 
	 * @param info
	 * @throws CoreException
	 */
	private static void initBuildInfoContainer(ManagedBuildInfo info) throws CoreException {
		if (info == null) {
			throw new CoreException(new Status(IStatus.ERROR, 
					ManagedBuilderCorePlugin.getUniqueIdentifier(), 
					IStatus.ERROR, 
					new String(), 
					null));
		}
					
		if (info.isContainerInited()) return;
		// Now associate the path entry container with the project
		ICProject cProject = info.getCProject();

		synchronized (cProject) {

			// This does not block the workspace or trigger delta events
		IPathEntry[] entries = cProject.getRawPathEntries();
		// Make sure the container for this project is in the path entries
		List newEntries = new ArrayList(Arrays.asList(entries));
		if (!newEntries.contains(ManagedBuildInfo.containerEntry)) {
			// In this case we should trigger an init and deltas
			newEntries.add(ManagedBuildInfo.containerEntry);
			cProject.setRawPathEntries((IPathEntry[])newEntries.toArray(new IPathEntry[newEntries.size()]), new NullProgressMonitor());
		}
		info.setContainerInited(true);
		
		}  //  end synchronized
	}
	
	private static boolean isVersionCompatible(IExtension extension) {
		// We can ignore the qualifier
		PluginVersionIdentifier version = null;

		// Get the version of the manifest
		IConfigurationElement[] elements = extension.getConfigurationElements();
		
		// Find the version string in the manifest
		for (int index = 0; index < elements.length; ++index) {
			IConfigurationElement element = elements[index];
			if (element.getName().equals(REVISION_ELEMENT_NAME)) {
				version = new PluginVersionIdentifier(element.getAttribute(VERSION_ELEMENT_NAME));
				break;
			}
		}
		
		if (version == null) {
			// This is a 1.2 manifest and we are compatible for now
			return true;
		}
		return(buildInfoVersion.isGreaterOrEqualTo(version));
	}
	
	/* (non-Javadoc)
	 * Determine if the .cdtbuild file is present, which will determine if build information
	 * can be loaded externally or not. Return true if present, false otherwise. 	 
	 */
	private static boolean canLoadBuildInfo(final IProject project) {
		IFile file = project.getFile(SETTINGS_FILE_NAME);
	    if (file == null) return false;
		File cdtbuild = file.getLocation().toFile();
		if (cdtbuild == null) return false;
		return cdtbuild.exists();
	}

	/* (non-Javadoc)
	 * Load the build information for the specified resource from its project
	 * file. Pay attention to the version number too.
	 */
	private static ManagedBuildInfo loadBuildInfo(final IProject project) throws Exception {
		ManagedBuildInfo buildInfo = null;
		IFile file = project.getFile(SETTINGS_FILE_NAME);
		File cdtbuild = file.getLocation().toFile();
		if (!cdtbuild.exists()) {
			// If we cannot find the .cdtbuild project file, throw an exception and let the user know
			throw new BuildException(ManagedMakeMessages.getFormattedString(PROJECT_FILE_ERROR, project.getName()));
		}
			
		// So there is a project file, load the information there
		InputStream stream = new FileInputStream(cdtbuild);
		try {
			DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = parser.parse(stream);
			String fileVersion = null;
			
			// Get the first element in the project file
			Node rootElement = document.getFirstChild();
			
			// Since 2.0 this will be a processing instruction containing version
			if (rootElement.getNodeType() != Node.PROCESSING_INSTRUCTION_NODE) {
				// This is a 1.2 project and it must be updated
			} else {
				// Make sure that the version is compatible with the manager
				fileVersion = rootElement.getNodeValue();
				PluginVersionIdentifier version = new PluginVersionIdentifier(fileVersion);
				if (buildInfoVersion.isGreaterThan(version)) {
					// This is >= 2.0 project, but earlier than the current MBS version - it may need to be updated
				} else {
					// This is a 
					//  isCompatibleWith will return FALSE, if:
					//   o  The major versions are not equal
					//   o  The major versions are equal, but the remainder of the .cdtbuild version # is
					//      greater than the MBS version #
					if (!buildInfoVersion.isCompatibleWith(version)) {
						throw new BuildException(ManagedMakeMessages.getFormattedString(PROJECT_VERSION_ERROR, project.getName())); 
					}
				}
			}
			
			// Now get the project root element (there should be only one)
			NodeList nodes = document.getElementsByTagName(ROOT_NODE_NAME);
			if (nodes.getLength() > 0) {
				Node node = nodes.item(0);
				
				//  Create the internal representation of the project's MBS information
				buildInfo = new ManagedBuildInfo(project, new XmlStorageElement((Element)node), true, fileVersion);
				if (fileVersion != null) {
	//				buildInfo.setVersion(fileVersion);
					PluginVersionIdentifier version = new PluginVersionIdentifier(fileVersion);
					PluginVersionIdentifier version21 = new PluginVersionIdentifier("2.1");		//$NON-NLS-1$
					//  CDT 2.1 is the first version using the new MBS model
					if (version.isGreaterOrEqualTo(version21)) {
						//  Check to see if all elements could be loaded correctly - for example, 
						//  if references in the project file could not be resolved to extension
						//  elements
						if (buildInfo.getManagedProject() == null ||
							(!buildInfo.getManagedProject().isValid())) {
							//  The load failed
							throw  new Exception(ManagedMakeMessages.getFormattedString("ManagedBuildManager.error.id.nomatch", project.getName())); //$NON-NLS-1$
						}
						
						// Each ToolChain/Tool/Builder element maintain two separate
						// converters if available
						// 0ne for previous Mbs versions and one for current Mbs version
						// walk through the project hierarchy and call the converters
						// written for previous mbs versions
						if ( checkForMigrationSupport(buildInfo, false) != true ) {
							// display an error message that the project is not loadable
							if (buildInfo.getManagedProject() == null ||
									(!buildInfo.getManagedProject().isValid())) {
									//  The load failed
									throw  new Exception(ManagedMakeMessages.getFormattedString("ManagedBuildManager.error.id.nomatch", project.getName())); //$NON-NLS-1$
							}
						}
					}
				}

				//  Upgrade the project's CDT version if necessary
				if (!UpdateManagedProjectManager.isCompatibleProject(buildInfo)) {
					UpdateManagedProjectManager.updateProject(project, buildInfo);
				}
				//  Check to see if the upgrade (if required) succeeded 
				if (buildInfo.getManagedProject() == null ||
					(!buildInfo.getManagedProject().isValid())) {
					//  The load failed
					throw  new Exception(ManagedMakeMessages.getFormattedString("ManagedBuildManager.error.id.nomatch", project.getName())); //$NON-NLS-1$
				}

				//  Walk through the project hierarchy and call the converters
				//  written for current mbs version
				if ( checkForMigrationSupport(buildInfo, true) != true ) {
					// display an error message.that the project is no loadable
					if (buildInfo.getManagedProject() == null ||
							(!buildInfo.getManagedProject().isValid())) {
							//  The load failed
							throw  new Exception(ManagedMakeMessages.getFormattedString("ManagedBuildManager.error.id.nomatch", project.getName())); //$NON-NLS-1$
						}
				}
				
				IConfiguration[] configs = buildInfo.getManagedProject().getConfigurations();
				//  Send an event to each configuration and if they exist, its resource configurations
				for (int i=0; i < configs.length; ++i) {
					ManagedBuildManager.performValueHandlerEvent(configs[i], IManagedOptionValueHandler.EVENT_OPEN);
				}
				//  Finish up
				//project.setSessionProperty(buildInfoProperty, buildInfo);
				setLoaddedBuildInfo(project, buildInfo);
			}
		} catch (Exception e) {
			throw e;
		}
		
		buildInfo.setValid(true);
		return buildInfo;
	}

	/* (non-Javadoc)
	 * This method loads all of the managed build system manifest files
	 * that have been installed with CDT.  An internal hierarchy of
	 * objects is created that contains the information from the manifest
	 * files.  The information is then accessed through the ManagedBuildManager.
	 * 
	 * Since the class does not have a constructor but all public methods
	 * call this method first, it is effectively a startup method
	 */
	private static void loadExtensions() throws BuildException {
		if (projectTypesLoaded)
			return;

		loadExtensionsSynchronized();
	}
	
	private synchronized static void loadExtensionsSynchronized() throws BuildException {
		// Do this once
		if (projectTypesLoaded)
				return;

		// This routine gets called recursively.  If so, just return
		if (projectTypesLoading)
			return;
		projectTypesLoading = true;
		
		//The list of the IManagedBuildDefinitionsStartup callbacks 
		List buildDefStartupList = null;
		// Get the extensions that use the current CDT managed build model
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(EXTENSION_POINT_ID);
		if( extensionPoint != null) {
			IExtension[] extensions = extensionPoint.getExtensions();
			if (extensions != null) {
				
				// First call the constructors of the internal classes that correspond to the
				// build model elements
				for (int i = 0; i < extensions.length; ++i) {
					IExtension extension = extensions[i];
					// Can we read this manifest
					if (!isVersionCompatible(extension)) {
						//  The version of the Plug-in is greater than what the manager thinks it understands
						//  Display error message
						IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
						if(window == null){
							IWorkbenchWindow windows[] = PlatformUI.getWorkbench().getWorkbenchWindows();
							window = windows[0];
						}

						final Shell shell = window.getShell();
						final String errMsg = ManagedMakeMessages.getFormattedString(MANIFEST_VERSION_ERROR, extension.getUniqueIdentifier());
						shell.getDisplay().asyncExec( new Runnable() {
							public void run() {
								MessageDialog.openError(shell, 
										ManagedMakeMessages.getResourceString("ManagedBuildManager.error.manifest_load_failed_title"),	//$NON-NLS-1$
										errMsg);
							}
						} );
					} else {			
						// Get the "configuraton elements" defined in the plugin.xml file.
						// Note that these "configuration elements" are not related to the
						// managed build system "configurations".  
						// From the PDE Guide:
						//  A configuration element, with its attributes and children, directly 
						//  reflects the content and structure of the extension section within the 
						//  declaring plug-in's manifest (plugin.xml) file. 
						IConfigurationElement[] elements = extension.getConfigurationElements();
						String revision = null;
						
						// Get the managedBuildRevsion of the extension.
						for (int j = 0; j < elements.length; j++) {
							IConfigurationElement element = elements[j];
							
							if( element.getName().equals(REVISION_ELEMENT_NAME) ) {	
								revision = element.getAttribute(VERSION_ELEMENT_NAME);
								break;
							}
						}
						
						// Get the value of 'ManagedBuildRevision' attribute
						loadConfigElements(DefaultManagedConfigElement.convertArray(elements, extension), revision);
					}
				}
				
				// Call the start up config extensions. These may rely on the standard elements
				// having already been loaded so we wait to call them from here.
				if (startUpConfigElements != null) {
					buildDefStartupList = new ArrayList(startUpConfigElements.size());

					for (Iterator iter = startUpConfigElements.iterator(); iter.hasNext();) {
						IManagedBuildDefinitionsStartup customConfigLoader;
						try {
							customConfigLoader = createStartUpConfigLoader((DefaultManagedConfigElement)iter.next());
							
							//need to save the startup for the future notifications
							buildDefStartupList.add(customConfigLoader);
							
							// Now we can perform any actions on the build configurations
							// in an extended plugin before the build configurations have been resolved
							customConfigLoader.buildDefsLoaded();					
						} catch (CoreException e) {}
					}
				}
				
				// Then call resolve.
				//
				// Here are notes on "references" within the managed build system.
				// References are "pointers" from one model element to another.
				// These are encoded in manifest and managed build system project files (.cdtbuild)
				// using unique string IDs (e.g. "cdt.managedbuild.tool.gnu.c.linker").
				// These string IDs are "resolved" to pointers to interfaces in model 
				// elements in the in-memory represent of the managed build system information.
				//
				// Here are the current "rules" for references:
				//  1.  A reference is always resolved to an interface pointer in the
				//      referenced object.
				//  2.  A reference is always TO an extension object - that is, an object
				//      loaded from a manifest file or a dynamic element provider.  It cannot
				//      be to an object loaded from a managed build system project file (.cdtbuild).
				//

				Iterator projectTypeIter = getExtensionProjectTypeMap().values().iterator();
				while (projectTypeIter.hasNext()) {
					try {
						ProjectType projectType = (ProjectType)projectTypeIter.next();
						projectType.resolveReferences();
					} catch (Exception ex) {
						// TODO: log
						ex.printStackTrace();
					}
				}
				Iterator configurationIter = getExtensionConfigurationMap().values().iterator();
				while (configurationIter.hasNext()) {
					try {
						Configuration configuration = (Configuration)configurationIter.next();
						configuration.resolveReferences();
					} catch (Exception ex) {
						// TODO: log
						ex.printStackTrace();
					}
				}
				Iterator resConfigIter = getExtensionResourceConfigurationMap().values().iterator();
				while (resConfigIter.hasNext()) {
					try {
						ResourceConfiguration resConfig = (ResourceConfiguration)resConfigIter.next();
						resConfig.resolveReferences();
					} catch (Exception ex) {
						// TODO: log
						ex.printStackTrace();
					}
				}
				Iterator toolChainIter = getExtensionToolChainMap().values().iterator();
				while (toolChainIter.hasNext()) {
					try {
						ToolChain toolChain = (ToolChain)toolChainIter.next();
						toolChain.resolveReferences();
					} catch (Exception ex) {
						// TODO: log
						ex.printStackTrace();
					}
				}
				Iterator toolIter = getExtensionToolMap().values().iterator();
				while (toolIter.hasNext()) {
					try {
						Tool tool = (Tool)toolIter.next();
						tool.resolveReferences();
					} catch (Exception ex) {
						// TODO: log
						ex.printStackTrace();
					}
				}
				Iterator targetPlatformIter = getExtensionTargetPlatformMap().values().iterator();
				while (targetPlatformIter.hasNext()) {
					try {
						TargetPlatform targetPlatform = (TargetPlatform)targetPlatformIter.next();
						targetPlatform.resolveReferences();
					} catch (Exception ex) {
						// TODO: log
						ex.printStackTrace();
					}
				}
				Iterator builderIter = getExtensionBuilderMap().values().iterator();
				while (builderIter.hasNext()) {
					try {
						Builder builder = (Builder)builderIter.next();
						builder.resolveReferences();
					} catch (Exception ex) {
						// TODO: log
						ex.printStackTrace();
					}
				}
				Iterator optionIter = getExtensionOptionMap().values().iterator();
				while (optionIter.hasNext()) {
					try {
						Option option = (Option)optionIter.next();
						option.resolveReferences();
					} catch (Exception ex) {
						// TODO: log
						ex.printStackTrace();
					}
				}
				Iterator optionCatIter = getExtensionOptionCategoryMap().values().iterator();
				while (optionCatIter.hasNext()) {
					try {
						OptionCategory optionCat = (OptionCategory)optionCatIter.next();
						optionCat.resolveReferences();
					} catch (Exception ex) {
						// TODO: log
						ex.printStackTrace();
					}
				}
			}
		}

		// Get the extensions that use the CDT 2.0 build model
		extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(EXTENSION_POINT_ID_V2);
		if( extensionPoint != null) {
			IExtension[] extensions = extensionPoint.getExtensions();
			String revision = null;
			
			if (extensions != null) {
				if (extensions.length > 0) {
					
					// Call the constructors of the internal classes that correspond to the
					// V2.0 build model elements.  Some of these objects are converted to new model objects.
					// Others can use the same classes.
					for (int i = 0; i < extensions.length; ++i) {
						IExtension extension = extensions[i];
						// Can we read this manifest
						if (!isVersionCompatible(extension)) {
							//The version of the Plug-in is greater than what the manager thinks it understands
							throw new BuildException(ManagedMakeMessages.getResourceString(MANIFEST_VERSION_ERROR));
						}			
						IConfigurationElement[] elements = extension.getConfigurationElements();					
						
						// Get the managedBuildRevsion of the extension.
						for (int j = 0; j < elements.length; j++) {
							IConfigurationElement element = elements[j];
							if(element.getName().equals(REVISION_ELEMENT_NAME)) {
								revision = element.getAttribute(VERSION_ELEMENT_NAME);		
								break;
							}
						}
						// If the "fileVersion" attribute is missing, then default revision is "1.2.0"
						if (revision == null)
							revision = "1.2.0"; 	//$NON-NLS-1$
						loadConfigElementsV2(DefaultManagedConfigElement.convertArray(elements, extension), revision);
					}
					// Resolve references
					Iterator targetIter = getExtensionTargetMap().values().iterator();
					while (targetIter.hasNext()) {
						try {
							Target target = (Target)targetIter.next();
							target.resolveReferences();
						} catch (Exception ex) {
							// TODO: log
							ex.printStackTrace();
						}
					}
					// The V2 model can also add top-level Tools - they need to be "resolved" 
					Iterator toolIter = getExtensionToolMap().values().iterator();
					while (toolIter.hasNext()) {
						try {
							Tool tool = (Tool)toolIter.next();
							tool.resolveReferences();
						} catch (Exception ex) {
							// TODO: log
							ex.printStackTrace();
						}
					}
					// Convert the targets to the new model
					targetIter = getExtensionTargetMap().values().iterator();
					while (targetIter.hasNext()) {
						try {
							Target target = (Target)targetIter.next();
							//  Check to see if it has already been converted - if not, do it
							if (target.getCreatedProjectType() == null) {
								target.convertToProjectType(revision);
							}
						} catch (Exception ex) {
							// TODO: log
							ex.printStackTrace();
						}
					}
					// Resolve references for new ProjectTypes
					Iterator projTypeIter = getExtensionProjectTypeMap().values().iterator();
					while (projTypeIter.hasNext()) {
						try {
							ProjectType projType = (ProjectType)projTypeIter.next();
							projType.resolveReferences();
						} catch (Exception ex) {
							// TODO: log
							ex.printStackTrace();
						}
					}

					// TODO:  Clear the target and configurationV2 maps so that the object can be garbage collected
					//        We can't do this yet, because the UpdateManagedProjectAction class may need these elements later
					//        Can we change UpdateManagedProjectAction to see the converted model elements?
					//targetIter = getExtensionTargetMap().values().iterator();
					//while (targetIter.hasNext()) {
					//	try {
					//		Target target = (Target)targetIter.next();
					//		ManagedBuildManager.removeConfigElement(target);
					//		getExtensionTargetMap().remove(target);
					//	} catch (Exception ex) {
					//		// TODO: log
					//		ex.printStackTrace();
					//	}
					//}
					//getExtensionConfigurationV2Map().clear();
				}
			}
		}
		
		// configs resolved...
		// Call the start up config extensions again now that configs have been resolved. 
		if (buildDefStartupList != null) {
			for (Iterator iter = buildDefStartupList.iterator(); iter.hasNext();) {
				IManagedBuildDefinitionsStartup customConfigLoader = (IManagedBuildDefinitionsStartup)iter.next();

				// Now we can perform any actions on the build configurations
				// in an extended plugin now that all build configruations have been resolved
				customConfigLoader.buildDefsResolved();					
			}
		}
		
		performAdjustments();
		projectTypesLoading = false;
		projectTypesLoaded = true;
	}
	
	private static void performAdjustments(){
		IProjectType types[] = getDefinedProjectTypes();
		for(int i = 0; i < types.length; i++){
			IConfiguration cfgs[] = types[i].getConfigurations();
			for(int j = 0; j < cfgs.length; j++){
				adjustConfig(cfgs[j]);
			}
		}

		for(int i = 0; i < types.length; i++){
			IConfiguration cfgs[] = types[i].getConfigurations();
			for(int j = 0; j < cfgs.length; j++){
				performValueHandlerEvent(cfgs[j], IManagedOptionValueHandler.EVENT_LOAD);
			}
		}

	}
	
	private static void adjustConfig(IConfiguration cfg){
		IResourceInfo rcInfos[] = cfg.getResourceInfos();
		for(int i = 0 ; i < rcInfos.length; i++){
			if(rcInfos[i] instanceof IFolderInfo){
				IFolderInfo info = (IFolderInfo)rcInfos[i];
				IToolChain tc = info.getToolChain();
				adjustHolder(info, tc);

				ITool tools[] = tc.getTools();
				for(int j = 0; j < tools.length; j++){
					adjustHolder(info, tools[j]);
				}
			} else if (rcInfos[i] instanceof IFileInfo){
				IFileInfo info = (IFileInfo)rcInfos[i];
				ITool rcTools[] = info.getTools();
				for(int j = 0; j < rcTools.length; j++){
					adjustHolder(info, rcTools[j]);
				}
				
			}
		}
		
		IResourceConfiguration rcCfgs[] = cfg.getResourceConfigurations();
		
		for(int i = 0; i <rcCfgs.length; i++){
		}
		
	}
	
	private static void adjustHolder(IResourceInfo rcInfo, IHoldsOptions holder){
		IOption options[] = holder.getOptions();
		
		for(int i = 0; i < options.length; i++){
			Option option = (Option)options[i];
			BooleanExpressionApplicabilityCalculator calc = 
				option.getBooleanExpressionCalculator(true);
			
			if(calc != null)
				calc.adjustOption(rcInfo,holder,option, true);
		}
	}
	

	private static void loadConfigElements(IManagedConfigElement[] elements, String revision) {
		for (int toolIndex = 0; toolIndex < elements.length; ++toolIndex) {
			try {
				IManagedConfigElement element = elements[toolIndex];
				// Load the top level elements, which in turn load their children
				if (element.getName().equals(IProjectType.PROJECTTYPE_ELEMENT_NAME)) {
					new ProjectType(element, revision);
				} else if (element.getName().equals(IConfiguration.CONFIGURATION_ELEMENT_NAME)) {
					new Configuration((ProjectType)null, element, revision);
				} else if (element.getName().equals(IToolChain.TOOL_CHAIN_ELEMENT_NAME)) {
					new ToolChain((IFolderInfo)null, element, revision);
				} else if (element.getName().equals(ITool.TOOL_ELEMENT_NAME)) {
					new Tool((ProjectType)null, element, revision);
				} else if (element.getName().equals(ITargetPlatform.TARGET_PLATFORM_ELEMENT_NAME)) {
					new TargetPlatform((ToolChain)null, element, revision);
				} else if (element.getName().equals(IBuilder.BUILDER_ELEMENT_NAME)) {
					new Builder((ToolChain)null, element, revision);
				} else if (element.getName().equals(IManagedConfigElementProvider.ELEMENT_NAME)) {
					// don't allow nested config providers.
					if (element instanceof DefaultManagedConfigElement) {
						IManagedConfigElement[] providedConfigs;
						IManagedConfigElementProvider provider = createConfigProvider(
								(DefaultManagedConfigElement)element);
						providedConfigs = provider.getConfigElements();
						loadConfigElements(providedConfigs, revision);	// This must use the current build model
					}
				} else if (element.getName().equals(IManagedBuildDefinitionsStartup.BUILD_DEFINITION_STARTUP)) {
					if (element instanceof DefaultManagedConfigElement) {
					// Cache up early configuration extension elements so was can call them after
					// other configuration elements have loaded.
						if (startUpConfigElements == null)
							startUpConfigElements = new ArrayList();
						startUpConfigElements.add(element);
					}
				} else {
					// TODO: Report an error (log?)
				}
			} catch (Exception ex) {
				// TODO: log
				ex.printStackTrace();
			}
		}
	}

	private static void loadConfigElementsV2(IManagedConfigElement[] elements, String revision) {
		for (int toolIndex = 0; toolIndex < elements.length; ++toolIndex) {
			try {
				IManagedConfigElement element = elements[toolIndex];
				// Load the top level elements, which in turn load their children
				if (element.getName().equals(ITool.TOOL_ELEMENT_NAME)) {
					new Tool(element, revision);
				} else if (element.getName().equals(ITarget.TARGET_ELEMENT_NAME)) {
					new Target(element,revision);
				} else if (element.getName().equals(IManagedConfigElementProvider.ELEMENT_NAME)) {
					// don't allow nested config providers.
					if (element instanceof DefaultManagedConfigElement) {
						IManagedConfigElement[] providedConfigs;
						IManagedConfigElementProvider provider = createConfigProvider(
								(DefaultManagedConfigElement)element);
						providedConfigs = provider.getConfigElements();
						loadConfigElementsV2(providedConfigs, revision);	// This must use the 2.0 build model
					}
				}
			} catch (Exception ex) {
				// TODO: log
				ex.printStackTrace();
			}
		}
	}
	
	/*
	 * Creates a new build information object and associates it with the 
	 * resource in the argument. Note that the information contains no 
	 * build target or configuation information. It is the responsibility 
	 * of the caller to populate it. It is also important to note that the 
	 * caller is responsible for associating an IPathEntryContainer with the 
	 * build information after it has been populated. 
	 * <p>
	 * The typical sequence of calls to add a new build information object to 
	 * a managed build project is
	 * <p><pre>
	 * ManagedBuildManager.createBuildInfo(project);
	 * &#047;&#047; Do whatever initialization you need here
	 * ManagedBuildManager.createTarget(project);
	 * ManagedBuildManager.initBuildInfoContainer(project);
	 *   
	 * @param resource The resource the build information is associated with
	 */
	public static ManagedBuildInfo createBuildInfo(IResource resource) {
		IProject proj = resource.getProject();
		ManagedBuildInfo buildInfo = new ManagedBuildInfo(proj);
		try {
			setLoaddedBuildInfo(proj, buildInfo);
		} catch (CoreException e) {
			ManagedBuilderCorePlugin.log(e);
			buildInfo = null;
		}
		return buildInfo;
	}
	
	public static void setLoaddedBuildInfo(IProject project, IManagedBuildInfo info) throws CoreException{
		// Associate the build info with the project for the duration of the session
		//project.setSessionProperty(buildInfoProperty, info);
//		IResourceRuleFactory rcRf = ResourcesPlugin.getWorkspace().getRuleFactory();
//		ISchedulingRule rule = rcRf.modifyRule(project);
//		IJobManager mngr = Job.getJobManager();

//		try {
//			mngr.beginRule(rule, null);
			doSetLoaddedInfo(project, info);
//		} catch (IllegalArgumentException e) {
//			// TODO: set anyway for now
//			doSetLoaddedInfo(project, info);
//		}finally {
//			mngr.endRule(rule);
//		}
	}
	
	private synchronized static void doSetLoaddedInfo(IProject project, IManagedBuildInfo info){
		if(info != null){
			fInfoMap.put(project, info);
			if(BuildDbgUtil.DEBUG)
				BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD, "build info load: build info set for project " + project.getName()); //$NON-NLS-1$
		}else{
			fInfoMap.remove(project);
			if(BuildDbgUtil.DEBUG)
				BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD, "build info load: build info CLEARED for project " + project.getName()); //$NON-NLS-1$
		}
	}
	
	private static IManagedConfigElementProvider createConfigProvider(
		DefaultManagedConfigElement element) throws CoreException {

		return (IManagedConfigElementProvider)element.getConfigurationElement().
			createExecutableExtension(IManagedConfigElementProvider.CLASS_ATTRIBUTE);
	}

	
	private static IManagedBuildDefinitionsStartup createStartUpConfigLoader(
			DefaultManagedConfigElement element) throws CoreException {
		
			return (IManagedBuildDefinitionsStartup)element.getConfigurationElement().createExecutableExtension(IManagedBuildDefinitionsStartup.CLASS_ATTRIBUTE);
		}
	
	/**
	 * @param project
	 * @return
	 */
	public static boolean manages(IResource resource) {
		ICProjectDescription des = CoreModel.getDefault().getProjectDescription(resource.getProject(), false);
		if(des == null){
			return false;
		}

		ICConfigurationDescription cfgDes = des.getActiveConfiguration();
		IConfiguration cfg = ManagedBuildManager.getConfigurationForDescription(cfgDes);
		if(cfg != null)
			return true;
		return false;
		

		//		// The managed build manager manages build information for the 
//		// resource IFF it it is a project and has a build file with the proper
//		// root element
//		IProject project = null;
//		if (resource instanceof IProject){
//			project = (IProject)resource;
//		} else if (resource instanceof IFile) {
//			project = ((IFile)resource).getProject();
//		} else {
//			return false;
//		}
//		IFile file = project.getFile(SETTINGS_FILE_NAME);
//		if (file.exists()) {
//			try {
//				InputStream stream = file.getContents();
//				DocumentBuilder parser = DocumentBuilderFactory.newInstance().newDocumentBuilder();
//				Document document = parser.parse(stream);
//				NodeList nodes = document.getElementsByTagName(ROOT_NODE_NAME);
//				return (nodes.getLength() > 0);
//			} catch (Exception e) {
//				return false;
//			}
//		}
//		return false;
	}

	/* (non-Javadoc)
	 * Provate helper method that first checks to see if a build information 
	 * object has been associated with the project for the current workspace 
	 * session. If one cannot be found, one is created from the project file 
	 * associated with the argument. If there is no prject file or the load 
	 * fails for some reason, the method will return <code>null</code> 
	 *  
	 * @param resource
	 * @return
	 */
	private static ManagedBuildInfo findBuildInfo(IResource rc, boolean forceLoad) {

		if (rc == null){
			if(BuildDbgUtil.DEBUG)
				BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD, "build info load: null resource"); //$NON-NLS-1$
			return null;
		}

		ManagedBuildInfo buildInfo = null;
		IProject proj = rc.getProject();

		// Check if there is any build info associated with this project for this session
		try {
			buildInfo = getLoaddedBuildInfo(proj);
		} catch (CoreException e) {
			if(BuildDbgUtil.DEBUG)
				BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD, "build info load: core exception while getting the loaded info: " + e.getLocalizedMessage()); //$NON-NLS-1$
			return null;
		}
	
		if(buildInfo == null /*&& forceLoad*/){
			int flags = forceLoad ? 0 : ICProjectDescriptionManager.GET_IF_LOADDED;
			
			if(BuildDbgUtil.DEBUG)
				BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD, "build info load: build info is NOT loadded" + (forceLoad ? " forceload" : "")); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			ICProjectDescription projDes = CoreModel.getDefault().getProjectDescriptionManager().getProjectDescription(proj, flags);
			if(projDes != null){
				if(BuildDbgUtil.DEBUG)
					BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD, "build info load: project description is obtained, qwerying the loaded build info"); //$NON-NLS-1$
				try {
					buildInfo = getLoaddedBuildInfo(proj);
				} catch (CoreException e) {
					if(BuildDbgUtil.DEBUG)
						BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD, "build info load: core exception while getting the loaded info (2): " + e.getLocalizedMessage()); //$NON-NLS-1$
					return null;
				}
				
				if(buildInfo == null){
					if(BuildDbgUtil.DEBUG)
						BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD, "build info load: info is null, trying the cfg data provider"); //$NON-NLS-1$
					
					buildInfo = ConfigurationDataProvider.getLoaddedBuildInfo(projDes);
					if(buildInfo != null){
						if(BuildDbgUtil.DEBUG)
							BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD, "build info load: info found, setting as loadded"); //$NON-NLS-1$
						
						try {
							setLoaddedBuildInfo(proj, buildInfo);
						} catch (CoreException e) {
							if(BuildDbgUtil.DEBUG)
								BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD, "build info load: core exception while setting loaded description, ignoring; : " + e.getLocalizedMessage()); //$NON-NLS-1$
						}
					}
						
				}

			} else if(BuildDbgUtil.DEBUG){
				BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD, "build info load: project description in null"); //$NON-NLS-1$
			}

			
			if(buildInfo == null){
				if(BuildDbgUtil.DEBUG)
					BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD, "build info load: info is null, querying the update mngr"); //$NON-NLS-1$
				buildInfo = UpdateManagedProjectManager.getConvertedManagedBuildInfo(proj);
			}
		}
//		if (buildInfo == null && resource instanceof IProject)
//			buildInfo = findBuildInfoSynchronized((IProject)resource, forceLoad);
/*		
		// Nothing in session store, so see if we can load it from cdtbuild
		if (buildInfo == null && resource instanceof IProject) {
			try {
				buildInfo = loadBuildInfo((IProject)resource);
			} catch (Exception e) {
				// TODO:  Issue error reagarding not being able to load the project file (.cdtbuild)
			}
			
			try {
				// Check if the project needs its container initialized
				initBuildInfoContainer(buildInfo);
			} catch (CoreException e) {
				// We can live without a path entry container if the build information is valid
			}
		}
*/
		if(buildInfo != null)
			buildInfo.updateOwner(proj);
		
		if(BuildDbgUtil.DEBUG){
			if(buildInfo == null)
				BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD, "build info load: build info is null"); //$NON-NLS-1$
//			else
//				BuildDbgUtil.getInstance().traceln(BuildDbgUtil.BUILD_INFO_LOAD, "build info load: build info found");
		}

		return buildInfo;
	}
	
	synchronized static ManagedBuildInfo getLoaddedBuildInfo(IProject project) throws CoreException{
		// Check if there is any build info associated with this project for this session
		ManagedBuildInfo buildInfo = (ManagedBuildInfo)fInfoMap.get(project);//project.getSessionProperty(buildInfoProperty);
			// Make sure that if a project has build info, that the info is not corrupted
		if (buildInfo != null) {
			buildInfo.updateOwner(project);
		}
		return buildInfo;	
	}

	/* (non-Javadoc)
	 * Determine if build information can be found. Various attempts are made
	 * to find the information, and if successful, true is returned; false otherwise.
	 * Typically, this routine would be called prior to findBuildInfo, to deterimine
	 * if findBuildInfo should be called to actually do the loading of build
	 * information, if possible
	 * @param resource
	 * @return
	 */
	private static boolean canFindBuildInfo(IResource resource) {

		if (resource == null) return false;

		// Make sure the extension information is loaded first
		try {
			loadExtensions();
		} catch (BuildException e) {
			e.printStackTrace();
			return false;
		}
		
		ManagedBuildInfo buildInfo = null;

		// Check if there is any build info associated with this project for this session
		try {
			buildInfo = (ManagedBuildInfo)getLoaddedBuildInfo(resource.getProject());
		} catch (CoreException e) {
			// Continue, to see if any of the upcoming checks are successful
		}
		
		if (buildInfo == null && resource instanceof IProject) {
			// Check weather getBuildInfo is called from converter
			buildInfo = UpdateManagedProjectManager.getConvertedManagedBuildInfo((IProject)resource);
			if (buildInfo != null) return true;
			// Check if the build information can be loaded from the .cdtbuild file
			return canLoadBuildInfo(((IProject)resource));
		}

		return (buildInfo != null);
	}

	/* (non-Javadoc)
	 * this method is called if managed build info session property
	 * was not set. The caller will use the project rule 
	 * to synchronize with other callers
	 * findBuildInfoSynchronized could also be called from project converter
	 * in this case the ManagedBuildInfo saved in the converter would be returned
	 *  
	 * @param resource
	 * @return
	 */
/*	synchronized private static ManagedBuildInfo findBuildInfoSynchronized(IProject project, boolean forceLoad) {
		ManagedBuildInfo buildInfo = null;

		// Check if there is any build info associated with this project for this session
		try {
			buildInfo = (ManagedBuildInfo)project.getSessionProperty(buildInfoProperty);
			// Make sure that if a project has build info, that the info is not corrupted
			if (buildInfo != null) {
				buildInfo.updateOwner(project);
			}
		} catch (CoreException e) {
	//		return null;
		}
		
		if(buildInfo == null && forceLoad){
			// Make sure the extension information is loaded first
			try {
				loadExtensions();
			} catch (BuildException e) {
				e.printStackTrace();
				return null;
			}

			
			// Check weather getBuildInfo is called from converter
			buildInfo = UpdateManagedProjectManager.getConvertedManagedBuildInfo(project);
			
			// Nothing in session store, so see if we can load it from cdtbuild
			if (buildInfo == null) {
				try {
					buildInfo = loadBuildInfo(project);
				} catch (Exception e) {
					// Issue error regarding not being able to load the project file (.cdtbuild)
					if (buildInfo == null) {
						buildInfo = createBuildInfo(project); 
					}
					buildInfo.setValid(false);
					//  Display error message
					IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
					if(window == null){
						IWorkbenchWindow windows[] = PlatformUI.getWorkbench().getWorkbenchWindows();
						window = windows[0];
					}

					final Shell shell = window.getShell();
					final String exceptionMsg = e.getMessage(); 
					//using syncExec could cause a dead-lock
					//that is why asyncExec is used
					shell.getDisplay().asyncExec( new Runnable() {
						public void run() {
							MessageDialog.openError(shell, 
									ManagedMakeMessages.getResourceString("ManagedBuildManager.error.open_failed_title"),	//$NON-NLS-1$
									ManagedMakeMessages.getFormattedString("ManagedBuildManager.error.open_failed",			//$NON-NLS-1$
											exceptionMsg));
						}
					} );
				}

				if (buildInfo != null && !buildInfo.isContainerInited()) {
					//  NOTE:  If this is called inside the above rule, then an IllegalArgumentException can
					//         occur when the CDT project file is saved - it uses the Workspace Root as the scheduling rule.
					//         
					try {
						// Check if the project needs its container initialized
						initBuildInfoContainer(buildInfo);
					} catch (CoreException e) {
						// We can live without a path entry container if the build information is valid
					}
				}
			}
		}

		return buildInfo;
	}
*/	
	/**
	 * Finds, but does not create, the managed build information for the 
	 * argument.
	 * Loads the build info in case it is not currently loadded
	 * Calling this method is the same as calling getBuildInfo(IResource resource, boolean forceLoad)
	 * with the "forceLoad" argument set to true
	 * 
	 * 
	 * @see ManagedBuildManager#initBuildInfo(IResource)
	 * @param resource The resource to search for managed build information on.
	 * @return IManagedBuildInfo The build information object for the resource.
	 */
	public static IManagedBuildInfo getBuildInfo(IResource resource) {
		return getBuildInfo(resource, true);
	}

	public static synchronized IManagedBuildInfo getOldStyleBuildInfo(IProject project) throws CoreException {
		IManagedBuildInfo info = null;
		try {
			info = getLoaddedBuildInfo(project);
		} catch (CoreException e) {
		}
		
		if(info == null){
			try {
				info = loadBuildInfo(project);
				
				if(info != null)
					setLoaddedBuildInfo(project, info);
			} catch (Exception e) {
				throw new CoreException(new Status(IStatus.ERROR, ManagedBuilderCorePlugin.getUniqueIdentifier(), e.getLocalizedMessage(), e));
			}
		}
		
		return info;
		
	}

	public static synchronized IManagedBuildInfo getBuildInfoLegacy(IProject project){
		try {
			return getOldStyleBuildInfo(project);
		} catch (CoreException e) {
			ManagedBuilderCorePlugin.log(e);
			return null;
		}
	}
	/**
	 * Finds, but does not create, the managed build information for the 
	 * argument.
	 * If the build info is not currently loadded and "forceLoad" argument is set to true,
	 * loads the build info from the .cdtbuild file
	 * In case "forceLoad" is false, does not load the build info and returns null in case it is not loadded
	 * 
	 * @see ManagedBuildManager#initBuildInfo(IResource)
	 * @param resource The resource to search for managed build information on.
	 * @param forceLoad specifies whether the build info should be loadded in case it is not loadded currently.
	 * @return IManagedBuildInfo The build information object for the resource.
	 */
	public static IManagedBuildInfo getBuildInfo(IResource resource, boolean forceLoad) {
		return findBuildInfo(resource.getProject(), forceLoad);
	}

	/**
	 * Determines if the managed build information for the 
	 * argument can be found.
	 * 
	 * @see ManagedBuildManager#initBuildInfo(IResource)
	 * @param resource The resource to search for managed build information on.
	 * @return boolean True if the build info can be found; false otherwise.
	 */
	public static boolean canGetBuildInfo(IResource resource) {
		return canFindBuildInfo(resource.getProject());
	}

	/**
	 * Answers the current version of the managed builder plugin.
	 * 
	 * @return the current version of the managed builder plugin
	 */
	public static PluginVersionIdentifier getBuildInfoVersion() {
		return buildInfoVersion;
	}

	/**
	 * Get the full URL for a path that is relative to the plug-in 
	 * in which .buildDefinitions are defined
	 * 
	 * @return the full URL for a path relative to the .buildDefinitions
	 *         plugin
	 */
	public static URL getURLInBuildDefinitions(DefaultManagedConfigElement element, IPath path) {
		
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry().getExtensionPoint(EXTENSION_POINT_ID);
		if( extensionPoint != null) {
			IExtension[] extensions = extensionPoint.getExtensions();
			if (extensions != null) {
				
				// Iterate over all extensions that contribute to .buildDefinitions
				for (int i = 0; i < extensions.length; ++i) {
					IExtension extension = extensions[i];
					
					// Determine whether the configuration element that is
					// associated with the path, is valid for the extension that
					// we are currently processing.
					//
					// Note: If not done, icon file names would have to be unique
					// across several plug-ins.
					if (element.getExtension().getExtensionPointUniqueIdentifier() 
						 == extension.getExtensionPointUniqueIdentifier())
					{
						// Get the path-name
						Bundle bundle = Platform.getBundle( extension.getNamespace() );
						URL url = Platform.find(bundle, path);
						if ( url != null )
						{
							try {
								return Platform.asLocalURL(url);
							} catch (IOException e) {
								// Ignore the exception
								return null;
							}
						}
						else
						{
							// Print a warning
							OutputIconError(path.toString());
						}
					}
				}
			}			
		}		
		return null;
	}	
	
	/*
	 * @return
	 */
	private static Map getBuildModelListeners() {
		if (buildModelListeners == null) {
			buildModelListeners = new HashMap();
		}
		return buildModelListeners;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.parser.IScannerInfoProvider#getScannerInformation(org.eclipse.core.resources.IResource)
	 */
	public IScannerInfo getScannerInformation(IResource resource) {
		return (IScannerInfo) getBuildInfo(resource.getProject());
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.parser.IScannerInfoProvider#subscribe(org.eclipse.cdt.core.parser.IScannerInfoChangeListener)
	 */
	public synchronized void subscribe(IResource resource, IScannerInfoChangeListener listener) {
		IResource project = null;
		if (resource instanceof IProject) {
			project = resource;
		} else if (resource instanceof IFile) {
			project = ((IFile)resource).getProject();
		} else {
			return;
		}
		// Get listeners for this resource
		Map map = getBuildModelListeners();
		List list = (List) map.get(project);
		if (list == null) {
			// Create a new list
			list = new ArrayList();
		}
		if (!list.contains(listener)) {
			// Add the new listener for the resource
			list.add(listener);
			map.put(project, list);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.parser.IScannerInfoProvider#unsubscribe(org.eclipse.cdt.core.parser.IScannerInfoChangeListener)
	 */
	public synchronized void unsubscribe(IResource resource, IScannerInfoChangeListener listener) {
		IResource project = null;
		if (resource instanceof IProject) {
			project = resource;
		} else if (resource instanceof IFile) {
			project = ((IFile)resource).getProject();
		} else {
			return;
		}
		// Remove the listener
		Map map = getBuildModelListeners();
		List list = (List) map.get(project);
		if (list != null && !list.isEmpty()) {
			// The list is not empty so try to remove listener
			list.remove(listener);
			map.put(project, list);
		}
	}
	
	private static Map getConfigElementMap() {
		if (configElementMap == null) {
			configElementMap = new HashMap();
		}
		return configElementMap;
	}
	
	/**
	 * This method public for implementation reasons.  Not intended for use 
	 * by clients.
	 */
	public static void putConfigElement(IBuildObject buildObj, IManagedConfigElement configElement) {
		getConfigElementMap().put(buildObj, configElement);
	}
	
	/**
	 * Removes an item from the map
	 */
	private static void removeConfigElement(IBuildObject buildObj) {
		getConfigElementMap().remove(buildObj);
	}

	/**
	 * This method public for implementation reasons.  Not intended for use 
	 * by clients.
	 */
	public static IManagedConfigElement getConfigElement(IBuildObject buildObj) {
		return (IManagedConfigElement)getConfigElementMap().get(buildObj);
	}
	
	public static void OptionValidError(int errorId, String id) {
		String[] msgs = new String[1];
		msgs[0] = id;
		switch (errorId) {
		case ERROR_CATEGORY:
			ManagedBuildManager.OutputManifestError(
					ManagedMakeMessages.getFormattedString(ManagedBuildManager.MANIFEST_ERROR_OPTION_CATEGORY, msgs));
			break;
		case ERROR_FILTER:
			ManagedBuildManager.OutputManifestError(
					ManagedMakeMessages.getFormattedString(ManagedBuildManager.MANIFEST_ERROR_OPTION_FILTER, msgs));
			break;
		}
	}
	
	public static void OptionValueHandlerError(String attribute, String id) {
		String[] msgs = new String[2];
		msgs[0] = attribute;
		msgs[1] = id;
		ManagedBuildManager.OutputManifestError(
			ManagedMakeMessages.getFormattedString(ManagedBuildManager.MANIFEST_ERROR_OPTION_VALUEHANDLER, msgs));
	}
	
	public static void OutputResolveError(String attribute, String lookupId, String type, String id) {
		String[] msgs = new String[4];
		msgs[0] = attribute;
		msgs[1] = lookupId;
		msgs[2] = type;
		msgs[3] = id;
		ManagedBuildManager.OutputManifestError(
			ManagedMakeMessages.getFormattedString(ManagedBuildManager.MANIFEST_ERROR_RESOLVING, msgs));
	}
	
	public static void OutputDuplicateIdError(String type, String id) {
		String[] msgs = new String[2];
		msgs[0] = type;
		msgs[1] = id;
		ManagedBuildManager.OutputManifestError(
			ManagedMakeMessages.getFormattedString(ManagedBuildManager.MANIFEST_ERROR_DUPLICATE, msgs));
	}
	
	public static void OutputManifestError(String message) {
		System.err.println(ManagedMakeMessages.getResourceString(MANIFEST_ERROR_HEADER) + message + NEWLINE);
	}
	
	public static void OutputIconError(String iconLocation) {
		String[] msgs = new String[1];
		msgs[0]= iconLocation;
		ManagedBuildManager.OutputManifestError(
			ManagedMakeMessages.getFormattedString(ManagedBuildManager.MANIFEST_ERROR_ICON, msgs));
	}
	
	/**
	 * Returns the instance of the Environment Variable Provider
	 * 
	 * @return IEnvironmentVariableProvider
	 */
	public static IEnvironmentVariableProvider getEnvironmentVariableProvider(){
		return EnvironmentVariableProvider.getDefault();
	}
	
	/**
	 * Returns the version, if 'id' contains a valid version
	 * Returns null if 'id' does not contain a valid version 
	 * Returns null if 'id' does not contain a version
	 * 
	 * @param idAndVersion
	 * @return String
	 */
	
	public static String getVersionFromIdAndVersion(String idAndVersion) {
				
//		 Get the index of the separator '_' in tool id.
		int index = idAndVersion.lastIndexOf('_');

		//Validate the version number if exists.		
		if ( index != -1) {
			// Get the version number from tool id.
			String version = idAndVersion.substring(index+1);
			IStatus status = PluginVersionIdentifier.validateVersion(version);
			
			// If there is a valid version then return 'version'
			if ( status.isOK())
				return version;
		}
		// If there is no version information or not a valid version, return null
		return null;
	}
	
	/**
	 * If the input to this function contains 'id & a valid version', it returns only the 'id' part
	 * Otherwise it returns the received input back.
	 * 
	 * @param idAndVersion
	 * @return String
	 */
	public static String getIdFromIdAndVersion(String idAndVersion) {
		// If there is a valid version return only 'id' part
		if ( getVersionFromIdAndVersion(idAndVersion) != null) {
			// Get the index of the separator '_' in tool id.
			int index = idAndVersion.lastIndexOf('_');
			return idAndVersion.substring(0,index);
		}
		else {
			// if there is no version or no valid version
			return idAndVersion;
		}
	}

	/**
	 * Returns the instance of the Build Macro Provider
	 * 
	 * @return IBuildMacroProvider
	 */
	public static IBuildMacroProvider getBuildMacroProvider(){
		return BuildMacroProvider.getDefault();
	}
	
	/**
	 * Send event to value handlers of relevant configuration including
	 * all its child resource configurations, if they exist.
	 * 
	 * @param IConfiguration configuration for which to send the event
	 * @param event to be sent
	 * 
	 * @since 3.0
	 */
	public static void performValueHandlerEvent(IConfiguration config, int event) {
		performValueHandlerEvent(config, event, true);		
	}
	
	/**
	 * Send event to value handlers of relevant configuration.
	 * 
	 * @param IConfiguration configuration for which to send the event
	 * @param event to be sent
	 * @param doChildren - if true, also perform the event for all 
	 *        resource configurations that are children if this configuration. 
	 * 
	 * @since 3.0
	 */
	public static void performValueHandlerEvent(IConfiguration config, int event, boolean doChildren) {

		IToolChain toolChain = config.getToolChain();
		if (toolChain == null)
			return;
		
		IOption[] options = toolChain.getOptions();
		// Get global options directly under Toolchain (not associated with a particular tool)
		// This has to be sent to all the Options associated with this configuration.
		for (int i = 0; i < options.length; ++i) {
			// Ignore invalid options
			if (options[i].isValid()) {
				// Call the handler
				if (options[i].getValueHandler().handleValue(
						config, 
						toolChain, 
						options[i], 
						options[i].getValueHandlerExtraArgument(), 
						event)) {
					// TODO : Event is handled successfully and returned true.
					// May need to do something here say logging a message.
				} else {
					// Event handling Failed. 
				}
			}
		}

		// Get options associated with tools under toolChain
		ITool[] tools = config.getFilteredTools();
		for (int i = 0; i < tools.length; ++i) {
			IOption[] toolOptions = tools[i].getOptions();
			for (int j = 0; j < toolOptions.length; ++j) {
				// Ignore invalid options
				if (toolOptions[j].isValid()) {
					// Call the handler
					if (toolOptions[j].getValueHandler().handleValue(
							config, 
							tools[i], 
							toolOptions[j], 
							toolOptions[j].getValueHandlerExtraArgument(), 
							event)) {
						// TODO : Event is handled successfully and returned true.
						// May need to do something here say logging a message.
					} else {
						// Event handling Failed. 
					}
				}
			}
		}
		
		// Call backs for Resource Configurations associated with this config.
		if (doChildren == true) {
			IResourceConfiguration[] resConfigs = config.getResourceConfigurations();
			for (int j=0; j < resConfigs.length; ++j) {
				ManagedBuildManager.performValueHandlerEvent(resConfigs[j], event);
			}			
		}
	}
	
	/**
	 * Send event to value handlers of relevant configuration.
	 * 
	 * @param IResourceConfiguration configuration for which to send the event
	 * @param event to be sent
	 * 
	 * @since 3.0
	 */
	public static void performValueHandlerEvent(IResourceInfo config, int event) {

		// Note: Resource configurations have no toolchain options
		
		// Get options associated with the resource configuration
		ITool[] tools = config instanceof IFileInfo ? 
				((IFileInfo)config).getToolsToInvoke() :
					((IFolderInfo)config).getFilteredTools();
		for (int i = 0; i < tools.length; ++i) {
			IOption[] toolOptions = tools[i].getOptions();
			for (int j = 0; j < toolOptions.length; ++j) {
				// Ignore invalid options
				if (toolOptions[j].isValid()) {
					// Call the handler
					if (toolOptions[j].getValueHandler().handleValue(
							config, 
							tools[i], 
							toolOptions[j], 
							toolOptions[j].getValueHandlerExtraArgument(), 
							event)) {
						// TODO : Event is handled successfully and returned true.
						// May need to do something here say logging a message.
					} else {
						// Event handling Failed. 
					}
				}
			}
		}
	}
	
	private static boolean checkForMigrationSupport(ManagedBuildInfo buildInfo,
			boolean forCurrentMbsVersion) {
		
		IConfigurationElement element = null;

		// Get the managed project from buildInfo
		IManagedProject managedProject = buildInfo.getManagedProject();
		
		IProjectType projectType = managedProject.getProjectType();
		if (forCurrentMbsVersion) {
			element = ((ProjectType) projectType)
					.getCurrentMbsVersionConversionElement();
		} else {
			element = ((ProjectType) projectType)
					.getPreviousMbsVersionConversionElement();
		}

		if (element != null) {
			// If there is a converter element for projectType, invoke it.
			// projectType converter should take care of invoking converters of
			// it's children
			 
			if (invokeConverter(managedProject, element) == null) {
				buildInfo.getManagedProject().setValid(false);
				return false;
			}			
		} else {
			// other wise, walk through the hierarchy of the project and
			// call the converters if available for each configuration
			IConfiguration[] configs = managedProject.getConfigurations();
			for (int i = 0; i < configs.length; i++) {
				IConfiguration configuration = configs[i];
				IToolChain toolChain = configuration.getToolChain();

				if (forCurrentMbsVersion) {
					element = ((ToolChain) toolChain)
							.getCurrentMbsVersionConversionElement();
				} else {
					element = ((ToolChain) toolChain)
							.getPreviousMbsVersionConversionElement();
				}

				if (element != null) {
					// If there is a converter element for toolChain, invoke it
					// toolChain converter should take care of invoking
					// converters of it's children
					if (invokeConverter(toolChain, element) == null) {
						buildInfo.getManagedProject().setValid(false);
						return false;
					}
				} else {
					// If there are no converters for toolChain, walk through
					// it's children
					ITool[] tools = toolChain.getTools();
					for (int j = 0; j < tools.length; j++) {
						ITool tool = tools[j];
						if (forCurrentMbsVersion) {
							element = ((Tool) tool)
									.getCurrentMbsVersionConversionElement();
						} else {
							element = ((Tool) tool)
									.getPreviousMbsVersionConversionElement();
						}
						if (element != null) {
							if (invokeConverter(tool, element) == null) {
								buildInfo.getManagedProject().setValid(false);
								return false;
							}
						}
					}
					IBuilder builder = toolChain.getBuilder();
					if (builder != null) {
						if (forCurrentMbsVersion) {
							element = ((Builder) builder)
									.getCurrentMbsVersionConversionElement();
						} else {
							element = ((Builder) builder)
									.getPreviousMbsVersionConversionElement();
						}

						if (element != null) {
							if (invokeConverter(builder, element) == null) {
								buildInfo.getManagedProject().setValid(false);
								return false;
							}
						}
					}
				}

				// walk through each resource configuration and look if there
				// are any converters
				// available. If so, invoke them.
				IResourceConfiguration[] resourceConfigs = configuration
						.getResourceConfigurations();
				if ((resourceConfigs != null) && (resourceConfigs.length > 0)) {
					for (int j = 0; j < resourceConfigs.length; j++) {
						IResourceConfiguration resConfig = resourceConfigs[j];
						ITool[] resTools = resConfig.getTools();
						for (int k = 0; k < resTools.length; k++) {
							ITool resTool = resTools[k];
							if (forCurrentMbsVersion) {
								element = ((Tool) resTool)
										.getCurrentMbsVersionConversionElement();
							} else {
								element = ((Tool) resTool)
										.getPreviousMbsVersionConversionElement();
							}
							if (element != null) {
								if (invokeConverter(resTool, element) == null) {
									buildInfo.getManagedProject().setValid(
											false);
									return false;
								}
							}
						}
					}
				} // end of if			
			}
		}
		// If control comes here, it means either there is no converter element
		// or converters are invoked successfully
		
		return true;
	}

	private static IBuildObject invokeConverter(IBuildObject buildObject, IConfigurationElement element) {

		if (element != null) {
			IConvertManagedBuildObject convertBuildObject = null;
			String toId = element.getAttribute("toId"); //$NON-NLS-1$
			String fromId = element.getAttribute("fromId"); //$NON-NLS-1$

			try {
				convertBuildObject = (IConvertManagedBuildObject) element
						.createExecutableExtension("class"); //$NON-NLS-1$
			} catch (CoreException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			if (convertBuildObject != null) {
				// invoke the converter
				return convertBuildObject.convert(buildObject, fromId, toId, false);
			}
		}
		// if control comes here, it means that either 'convertBuildObject' is null or 
		// converter did not convert the object successfully
		return null;
	}
		
	/*
	 * Generic Converter function.
	 * If the converter is available for the given Build Object, it calls the corresponding converter.
	 * It returns null if there are no converters or if the conversion is not successful
	 * It returns 'IBuildObject' if the conversion is successful.
	 */

	public static IBuildObject convert(IBuildObject buildObj, String toId,
			boolean userhasConfirmed) {

		String tmpToId = null;

		// Get the Converter Extension Point
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry()
				.getExtensionPoint("org.eclipse.cdt.managedbuilder.core", //$NON-NLS-1$
						"projectConverter"); //$NON-NLS-1$
		if (extensionPoint != null) {
			// Get the extensions
			IExtension[] extensions = extensionPoint.getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				// Get the configuration elements of each extension
				IConfigurationElement[] configElements = extensions[i]
						.getConfigurationElements();
				for (int j = 0; j < configElements.length; j++) {

					IConfigurationElement element = configElements[j];
					if (element.getName().equals("converter") && (isBuildObjectApplicableForConversion(buildObj, element) == true)) { //$NON-NLS-1$
						tmpToId = element.getAttribute("toId");	//$NON-NLS-1$
						if (tmpToId.equals(toId)) {
							return invokeConverter(buildObj, element);
						}
					}
				}
			}
		}
		return null;
	}

	/*
	 * Generic routine for checking the availability of converters for the given
	 * Build Object. 
	 * @param IBuildObject, This function takes a 'IBuildObject' as an argument 
	 * @return true if there are converters for the given Build Object
	 * @return false if there are no converters
	 */

	public static boolean hasTargetConversionElements(IBuildObject buildObj) {

		// Get the Converter Extension Point
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry()
				.getExtensionPoint("org.eclipse.cdt.managedbuilder.core", //$NON-NLS-1$
						"projectConverter"); //$NON-NLS-1$
		if (extensionPoint != null) {
			// Get the extensions
			IExtension[] extensions = extensionPoint.getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				// Get the configuration elements of each extension
				IConfigurationElement[] configElements = extensions[i]
						.getConfigurationElements();
				for (int j = 0; j < configElements.length; j++) {

					IConfigurationElement element = configElements[j];
					if (element.getName().equals("converter") && (isBuildObjectApplicableForConversion(buildObj, element) == true)) //$NON-NLS-1$
						return true;
				}
			}
		}
		return false;
	}

	/*
	 * Generic function for getting the list of converters for the given Build Object
	 */

	public static Map getConversionElements(IBuildObject buildObj) {

		Map conversionTargets = new HashMap();

		// Get the Converter Extension Point
		IExtensionPoint extensionPoint = Platform.getExtensionRegistry()
				.getExtensionPoint("org.eclipse.cdt.managedbuilder.core", //$NON-NLS-1$
						"projectConverter"); //$NON-NLS-1$
		if (extensionPoint != null) {
			// Get the extensions
			IExtension[] extensions = extensionPoint.getExtensions();
			for (int i = 0; i < extensions.length; i++) {
				// Get the configuration elements of each extension
				IConfigurationElement[] configElements = extensions[i]
						.getConfigurationElements();
				for (int j = 0; j < configElements.length; j++) {
					IConfigurationElement element = configElements[j];
					if (element.getName().equals("converter") && (isBuildObjectApplicableForConversion(buildObj, element) == true)) { //$NON-NLS-1$
						conversionTargets.put((String) element
								.getAttribute("name"), element); //$NON-NLS-1$
					}
				}
			}
		}
		return conversionTargets;
	}

	/*
	 * Generic function that checks whether the given conversion element can be used to convert the given 
	 * build object. It returns true if the given build object is convertable, otherwise it returns false.
	 */

	private static boolean isBuildObjectApplicableForConversion(
			IBuildObject buildObj, IConfigurationElement element) {

		String id = null;
		String fromId = element.getAttribute("fromId"); //$NON-NLS-1$

		// Check whether the current converter can be used for conversion

		if (buildObj instanceof IProjectType) {
			IProjectType projType = (IProjectType) buildObj;
			
			// Check whether the converter's 'fromId' and the
			// given projType 'id' are equal			
			while (projType != null) {
				id = projType.getId();

				if (fromId.equals(id)) {
					return true;
				}
				projType = projType.getSuperClass();
			}
		} else if (buildObj instanceof IToolChain) {
			IToolChain toolChain = (IToolChain) buildObj;
			
			// Check whether the converter's 'fromId' and the
			// given toolChain 'id' are equal
			while (toolChain != null) {
				id = toolChain.getId();

				if (fromId.equals(id)) {
					return true;
				}
				toolChain = toolChain.getSuperClass();
			}
		} else if (buildObj instanceof ITool) {
			ITool tool = (ITool) buildObj;
			
			// Check whether the converter's 'fromId' and the
			// given tool 'id' are equal
			while (tool != null) {
				id = tool.getId();

				if (fromId.equals(id)) {
					return true;
				}
				tool = tool.getSuperClass();
			}
		} else if (buildObj instanceof IBuilder) {
			IBuilder builder = (IBuilder) buildObj;
			
			// Check whether the converter's 'fromId' and the
			// given builder 'id' are equal
			while (builder != null) {
				id = builder.getId();

				if (fromId.equals(id)) {
					return true;
				}
				builder = builder.getSuperClass();
			}
		}
		return false;
	}

	/*
	 * if the suffix is null, then the random number will be appended to the superId
	 */
	static public String calculateChildId(String superId, String suffix){
		if(suffix == null)
			suffix = new Integer(getRandomNumber()).toString();

		String version = getVersionFromIdAndVersion(superId);
        if(version != null)
            return ManagedBuildManager.getIdFromIdAndVersion(superId) + "." + suffix + "_" + version;             //$NON-NLS-1$ //$NON-NLS-2$
        return superId + "." + suffix;                     //$NON-NLS-1$
	}

	/**
	 * Calculate a relative path given the full path to a folder and a file 
	 */
	public static IPath calculateRelativePath(IPath container, IPath contents){
		IPath path = contents;
		if(container.isPrefixOf(contents)){
			path = contents.setDevice(null).removeFirstSegments(container.segmentCount());
		} else {
			String file = null;
			container = container.addTrailingSeparator();
			if(!contents.hasTrailingSeparator()){
				file = contents.lastSegment();
				contents = contents.removeLastSegments(1);
				contents = contents.addTrailingSeparator();
			}
			
			IPath prefix = contents;
			for(;prefix.segmentCount() > 0 && !prefix.isPrefixOf(container);prefix = prefix.removeLastSegments(1)){
			}
			if(prefix.segmentCount() > 0){
				int diff = container.segmentCount() - prefix.segmentCount();
				StringBuffer buff = new StringBuffer();
				while(diff-- > 0)
					buff.append("../");	//$NON-NLS-1$
				path = new Path(buff.toString()).append(contents.removeFirstSegments(prefix.segmentCount()));
				if(file != null)
					path = path.append(file);
			}
		}
		return path;
	}
	
/*	private static IBuildObject getBuildObjectFromDataObject(CDataObject data){
		if(data instanceof BuildConfigurationData)
			return ((BuildConfigurationData)data).getConfiguration();
		else if(data instanceof BuildFolderData)
			return ((BuildFolderData)data).getFolderInfo();
		else if(data instanceof BuildFileData)
			return ((BuildFileData)data).getFileInfo();
		return null;
	}
*/	
	private static final boolean TEST_CONSISTANCE = false;

	public static IConfiguration getConfigurationForDescription(ICConfigurationDescription cfgDes){
		return getConfigurationForDescription(cfgDes, TEST_CONSISTANCE);
	}

	private static IConfiguration getConfigurationForDescription(ICConfigurationDescription cfgDes, boolean checkConsistance){
		if(cfgDes == null)
			return null;
		CConfigurationData cfgData = cfgDes.getConfigurationData();
		if(cfgData instanceof BuildConfigurationData){
			IConfiguration cfg = ((BuildConfigurationData)cfgData).getConfiguration();
			if(checkConsistance){
				if(cfgDes != getDescriptionForConfiguration(cfg, false)){
					throw new IllegalStateException();
				}
			}
			return cfg;
		}
		return null;
	}
	
	public static int optionTypeToEntryKind(int type){
		switch(type){
		case IOption.INCLUDE_PATH:
			return ICLanguageSettingEntry.INCLUDE_PATH;
		case IOption.PREPROCESSOR_SYMBOLS:
			return ICLanguageSettingEntry.MACRO;
		case IOption.INCLUDE_FILES:
			return ICLanguageSettingEntry.INCLUDE_FILE;
		case IOption.LIBRARY_PATHS:
			return ICLanguageSettingEntry.LIBRARY_PATH;
		case IOption.LIBRARY_FILES:
			return ICLanguageSettingEntry.LIBRARY_FILE;
		case IOption.MACRO_FILES:
			return ICLanguageSettingEntry.MACRO_FILE;
//		case IOption.LIBRARIES:
//			return ICLanguageSettingEntry.LIBRARY_PATH;
		}
		return 0;
	}
	
	public static int optionUndefTypeToEntryKind(int type){
		switch(type){
		case IOption.UNDEF_INCLUDE_PATH:
			return ICLanguageSettingEntry.INCLUDE_PATH;
		case IOption.UNDEF_PREPROCESSOR_SYMBOLS:
			return ICLanguageSettingEntry.MACRO;
		case IOption.UNDEF_INCLUDE_FILES:
			return ICLanguageSettingEntry.INCLUDE_FILE;
		case IOption.UNDEF_LIBRARY_PATHS:
			return ICLanguageSettingEntry.LIBRARY_PATH;
		case IOption.UNDEF_LIBRARY_FILES:
			return ICLanguageSettingEntry.LIBRARY_FILE;
		case IOption.UNDEF_MACRO_FILES:
			return ICLanguageSettingEntry.MACRO_FILE;
//		case IOption.LIBRARIES:
//			return ICLanguageSettingEntry.LIBRARY_PATH;
		}
		return 0;
	}

	public static int entryKindToOptionType(int kind){
		switch(kind){
		case ICLanguageSettingEntry.INCLUDE_PATH:
			return IOption.INCLUDE_PATH;
		case ICLanguageSettingEntry.INCLUDE_FILE:
			return IOption.INCLUDE_FILES;
		case ICLanguageSettingEntry.MACRO:
			return IOption.PREPROCESSOR_SYMBOLS;
		case ICLanguageSettingEntry.MACRO_FILE:
			return IOption.MACRO_FILES;
		case ICLanguageSettingEntry.LIBRARY_PATH:
			return IOption.LIBRARY_PATHS;//TODO IOption.LIBRARIES;
		case ICLanguageSettingEntry.LIBRARY_FILE:
			return IOption.LIBRARY_FILES;
		}
		return 0;
	}
	
	public static int entryKindToUndefOptionType(int kind){
		switch(kind){
		case ICLanguageSettingEntry.INCLUDE_PATH:
			return IOption.UNDEF_INCLUDE_PATH;
		case ICLanguageSettingEntry.INCLUDE_FILE:
			return IOption.UNDEF_INCLUDE_FILES;
		case ICLanguageSettingEntry.MACRO:
			return IOption.UNDEF_PREPROCESSOR_SYMBOLS;
		case ICLanguageSettingEntry.MACRO_FILE:
			return IOption.UNDEF_MACRO_FILES;
		case ICLanguageSettingEntry.LIBRARY_PATH:
			return IOption.UNDEF_LIBRARY_PATHS;//TODO IOption.LIBRARIES;
		case ICLanguageSettingEntry.LIBRARY_FILE:
			return IOption.UNDEF_LIBRARY_FILES;
		}
		return 0;
	}
	public static ICConfigurationDescription getDescriptionForConfiguration(IConfiguration cfg){
		return getDescriptionForConfiguration(cfg, TEST_CONSISTANCE);
	}

	private static ICConfigurationDescription getDescriptionForConfiguration(IConfiguration cfg, boolean checkConsistance){
		if(cfg.isExtensionElement())
			return null;
		ICConfigurationDescription des = ((Configuration)cfg).getConfigurationDescription();
		if(des == null){
			if(checkConsistance)
				throw new IllegalStateException();
			if(((Configuration)cfg).isPreference()){
				try {
					des = CCorePlugin.getDefault().getPreferenceConfiguration(CFG_DATA_PROVIDER_ID);
				} catch (CoreException e) {
					ManagedBuilderCorePlugin.log(e);
				}
			} else {
				IProject project = cfg.getOwner().getProject();
				ICProjectDescription projDes = CoreModel.getDefault().getProjectDescription(project, false);
				if(projDes != null){
					des = projDes.getConfigurationById(cfg.getId());
				}
			}
		}
		if(checkConsistance){
			if(cfg != getConfigurationForDescription(des, false)){
				throw new IllegalStateException();
			}
		}
		return des;
	}
	
	public static IPath getBuildFullPath(IConfiguration cfg, IBuilder builder){
		IProject project = cfg.getOwner().getProject();
//		String path = builder.getBuildPath();
		
		IPath buildDirectory = builder.getBuildLocation();
		IPath fullPath = null;
		if (buildDirectory != null && !buildDirectory.isEmpty()) {
			IResource res = project.getParent().findMember(buildDirectory);
			if (res instanceof IContainer && res.exists()) {
				fullPath = res.getFullPath();
			} else {
				IContainer crs[] = ((IWorkspaceRoot)project.getParent()).findContainersForLocation(buildDirectory);
				if(crs.length != 0){
					String projName = project.getName();
					for(int i = 0; i < crs.length; i++){
						IPath path = crs[i].getFullPath();
						if(path.segmentCount() != 0 && path.segment(0).equals(projName)){
							fullPath = path;
							break;
						}
					}
					
					if(fullPath == null){
						fullPath = crs[0].getFullPath();
					}
				}
			}
		} else {
			fullPath = cfg.getOwner().getProject().getFullPath();
			if(builder.isManagedBuildOn())
				fullPath = fullPath.append(cfg.getName());
		}
		
		return fullPath;
	}
	
	public static String locationToFullPath(String path){
		if(!path.startsWith("${"))  //$NON-NLS-1$
			return null;
		int index = path.lastIndexOf('}');
		if(index == -1)
			return null;
		
		String varName = "workspace_loc"; //$NON-NLS-1$
		String str1 = path.substring(2, index);
		String result = null;
		if(str1.startsWith(varName)){
			str1 = str1.substring(varName.length());
			if(str1.length() != 0){
				if(str1.startsWith(":")){ //$NON-NLS-1$
					result = str1.substring(1);
				}
			} else {
				result = "/"; //$NON-NLS-1$
			}
		}
		
		return result;
	}
	
	public static String fullPathToLocation(String path){
		StringBuffer buf = new StringBuffer();
		return buf.append("${").append("workspace_loc:").append(path).append("}").toString(); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
	}

	public static IPath getBuildLocation(IConfiguration cfg, IBuilder builder) {
		if(cfg.getOwner() == null)
			return Path.EMPTY;
		
		IProject project = cfg.getOwner().getProject(); 
		IPath buildDirectory = builder.getBuildLocation();
		if (buildDirectory != null && !buildDirectory.isEmpty()) {
			IResource res = project.getParent().findMember(buildDirectory);
			if (res instanceof IContainer && res.exists()) {
				buildDirectory = res.getLocation();
			}
		} else {
			buildDirectory = project.getLocation();
			if(builder.isManagedBuildOn())
				buildDirectory = buildDirectory.append(cfg.getName());
		}
		return buildDirectory;
	}
	
	public static IBuilder[] createBuilders(IProject project, Map args){
		return ManagedBuilderCorePlugin.createBuilders(project, args);
	}
	
	public static IBuilder createCustomBuilder(IConfiguration cfg, String builderId) throws CoreException{
		return ManagedBuilderCorePlugin.createCustomBuilder(cfg, builderId);
	}
	
	public static IBuilder createCustomBuilder(IConfiguration cfg, IBuilder base){
		return ManagedBuilderCorePlugin.createCustomBuilder(cfg, base);
	}
	
	public static IBuilder createBuilderForEclipseBuilder(IConfiguration cfg, String eclipseBuilderID) throws CoreException {
		return ManagedBuilderCorePlugin.createBuilderForEclipseBuilder(cfg, eclipseBuilderID);
	}
	
/*	public static IToolChain[] getExtensionsToolChains(String propertyType, String propertyValue){
		List all = getSortedToolChains();
		List result = new ArrayList();
		for(int i = 0; i < all.size(); i++){
			List list = (List)all.get(i);
			IToolChain tc = findToolChain(list, propertyType, propertyValue);
			if(tc != null)
				result.add(tc);
		}
		return (IToolChain[])result.toArray(new ToolChain[result.size()]);
	}
*/	
/*	public static void resortToolChains(){
		sortedToolChains = null;
		getSortedToolChains();
	}
*/	
/*	private static List getSortedToolChains(){
		if(sortedToolChains == null){
			sortedToolChains = new ArrayList();
			SortedMap map = getExtensionToolChainMap();
			for(Iterator iter = map.values().iterator(); iter.hasNext();){
				ToolChain tc = (ToolChain)iter.next();
				if(tc.isAbstract())
					continue;
				List list = searchIdentical(sortedToolChains, tc);
				if(list == null){
					list = new ArrayList();
					sortedToolChains.add(list);
				}
				list.add(tc);
				tc.setIdenticalList(list);
			}
		}
		return sortedToolChains;
	}
*/	
//	private static List findIdenticalToolChains(IToolChain tc){
//		ToolChain tCh = (ToolChain)tc;
//		List list = tCh.getIdenticalList();
//		if(list == null){
//			resortToolChains();
//			list = tCh.getIdenticalList();
//			if(list == null){
//				list = new ArrayList(0);
//				tCh.setIdenticalList(list);
//			}
//		}
//
//		return ((ToolChain)tc).getIdenticalList();
//	}
	
	public static IToolChain[] getExtensionToolChains(IProjectType type){
		List result = new ArrayList();
		IConfiguration cfgs[] = type.getConfigurations();
		
		for(int i = 0; i < cfgs.length; i++){
			IToolChain tc = cfgs[i].getToolChain();
			if(tc == null)
				continue;
			
			List list = findIdenticalElements((ToolChain)tc, fToolChainSorter);
			int k = 0;
			for(; k < result.size(); k++){
				if(findIdenticalElements((ToolChain)result.get(k), fToolChainSorter) == list)
					break;
			}
			
			if(k == result.size()){
				result.add(tc);
			}
		}
		return (IToolChain[])result.toArray(new IToolChain[result.size()]);
	}
	
	public static IConfiguration[] getExtensionConfigurations(IToolChain tChain, IProjectType type){
		List list = new ArrayList();
		IConfiguration cfgs[] = type.getConfigurations();
		for(int i = 0; i < cfgs.length; i++){
			IToolChain cur = cfgs[i].getToolChain();
			if(cur != null
					&& findIdenticalElements((ToolChain)cur, fToolChainSorter) == findIdenticalElements((ToolChain)tChain, fToolChainSorter)){
				list.add(cfgs[i]);
			}
		}
		return (IConfiguration[])list.toArray(new Configuration[list.size()]);
	}

	public static IConfiguration getFirstExtensionConfiguration(IToolChain tChain){
		if(tChain.getParent() != null)
			return tChain.getParent();
		
		List list = findIdenticalElements((ToolChain)tChain, fToolChainSorter);
		if(list != null){
			for(int i = 0; i < list.size(); i++){
				ToolChain cur = (ToolChain)list.get(i);
				if(cur.getParent() != null)
					return cur.getParent();
			}
		}
		
		return null;
	}

	public static IConfiguration[] getExtensionConfigurations(IToolChain tChain, String propertyType, String propertyValue){
//		List all = getSortedToolChains();
		List list = findIdenticalElements((ToolChain)tChain, fToolChainSorter);
		List result = new ArrayList();
		boolean tcFound = false;
		if(list != null){
			for(int i = 0; i < list.size(); i++){
				ToolChain cur = (ToolChain)list.get(i);
				if(cur == tChain){
					tcFound = true;
				}
				
				IConfiguration cfg = cur.getParent();
				if(cfg != null){
					IBuildObjectProperties props = cfg.getBuildProperties();
					if(props.containsValue(propertyType, propertyValue)){
						result.add(cfg);
					}
				}
			}
			
		}
		
		if(!tcFound) {
			IConfiguration cfg = tChain.getParent();
			if(cfg != null){
				IBuildObjectProperties props = cfg.getBuildProperties();
				if(props.containsValue(propertyType, propertyValue)){
					result.add(cfg);
				}
			}
		}
		
//		if(result.size() == 0){
//			if(((ToolChain)tChain).supportsValue(propertyType, propertyValue)){
//				IConfiguration cfg = getFirstExtensionConfiguration(tChain);
//				if(cfg != null){
//					result.add(cfg);
//				}
//			}
//		}
		return (IConfiguration[])result.toArray(new IConfiguration[result.size()]);
	}
	
/*	public static IToolChain[] getRealToolChains(){
		List all = getSortedToolChains();
		IToolChain tcs[] = new ToolChain[all.size()];
		for(int i = 0; i < tcs.length; i++){
			List list = (List)all.get(i);
			tcs[i] = (ToolChain)list.get(0);
		}
		return tcs;
	}
*/	
	
	private static HashMap getSortedToolChains(){
		if(fSortedToolChains == null){
			fSortedToolChains = getSortedElements(ManagedBuildManager.getExtensionToolChainMap().values());
		}
		return fSortedToolChains;
	}

	private static HashMap getSortedTools(){
		if(fSortedTools == null){
			fSortedTools = getSortedElements(ManagedBuildManager.getExtensionToolMap().values());
		}
		return fSortedTools;
	}

	private static HashMap getSortedBuilders(){
		if(fSortedBuilders == null){
			fSortedBuilders = getSortedElements(ManagedBuildManager.getExtensionBuilderMap().values());
		}
		return fSortedBuilders;
	}

	private static HashMap getSortedElements(Collection elements){
		HashMap map = new HashMap();
		for(Iterator iter = elements.iterator(); iter.hasNext();){
			IMatchKeyProvider p = (IMatchKeyProvider)iter.next();
			Object key = p.getMatchKey();
			if(key == null)
				continue;

			List list = (List)map.get(key);
			if(list == null){
				list = new ArrayList();
				map.put(key, list);
			}
			list.add(p);
			p.setIdenticalList(list);
		}
		
		for(Iterator iter = map.values().iterator(); iter.hasNext();){
			Collections.sort((List)iter.next());
		}
		return map;
	}
	
	public static IToolChain[] getRealToolChains(){
		HashMap map = getSortedToolChains();
		IToolChain tcs[] = new ToolChain[map.size()];
		int i = 0;
		for(Iterator iter = map.values().iterator(); iter.hasNext();){
			List list = (List)iter.next();
			tcs[i++] = (ToolChain)list.get(0);
		}
		return tcs;
	}

	public static ITool[] getRealTools(){
		HashMap map = getSortedTools();
		ITool ts[] = new Tool[map.size()];
		int i = 0;
		for(Iterator iter = map.values().iterator(); iter.hasNext();){
			List list = (List)iter.next();
			ts[i++] = (Tool)list.get(0);
		}
		return ts;
	}

	public static IBuilder[] getRealBuilders(){
		HashMap map = getSortedBuilders();
		IBuilder bs[] = new Builder[map.size()];
		int i = 0;
		for(Iterator iter = map.values().iterator(); iter.hasNext();){
			List list = (List)iter.next();
			bs[i++] = (Builder)list.get(0);
		}
		return bs;
	}
	
	public static IBuilder getRealBuilder(IBuilder builder){
		IBuilder extBuilder = builder;
		IBuilder realBuilder = null;
		for(;extBuilder != null && !extBuilder.isExtensionElement(); extBuilder = extBuilder.getSuperClass());

		if(extBuilder != null){
			List list = findIdenticalElements((Builder)extBuilder, fBuilderSorter);
			if(list.size() == 0){
				realBuilder = extBuilder;
			} else {
				IBuilder[] realBs = getRealBuilders();
				for(int i = 0; i < realBs.length; i++){
					List rList = findIdenticalElements((Builder)realBs[i], fBuilderSorter);
					if(rList == list){
						realBuilder = realBs[i];
						break;
					}
				}
			}
		} else {
			//TODO:
		}
		return realBuilder;
	}

	public static ITool getRealTool(ITool tool){
		ITool extTool = tool;
		ITool realTool = null;
		for(;extTool != null && !extTool.isExtensionElement(); extTool= extTool.getSuperClass());

		if(extTool != null){
			List list = findIdenticalElements((Tool)extTool, fToolSorter);
			if(list.size() == 0){
				realTool = extTool;
			} else {
				ITool[] realTs = getRealTools();
				for(int i = 0; i < realTs.length; i++){
					List rList = findIdenticalElements((Tool)realTs[i], fToolSorter);
					if(rList == list){
						realTool = realTs[i];
						break;
					}
				}
			}
		} else {
			//TODO:
		}
		return realTool;
	}
	
	public static IToolChain getExtensionToolChain(IToolChain tc){
		IToolChain extTc = tc;
		for(;extTc != null && !extTc.isExtensionElement(); extTc= extTc.getSuperClass());
		return extTc;
	}

	public static IToolChain getRealToolChain(IToolChain tc){
		IToolChain extTc = tc;
		IToolChain realTc = null;
		for(;extTc != null && !extTc.isExtensionElement(); extTc= extTc.getSuperClass());

		if(extTc != null){
			List list = findIdenticalElements((ToolChain)extTc, fToolChainSorter);
			if(list.size() == 0){
				realTc = extTc;
			} else {
				IToolChain[] realTcs = getRealToolChains();
				for(int i = 0; i < realTcs.length; i++){
					List rList = findIdenticalElements((ToolChain)realTcs[i], fToolChainSorter);
					if(rList == list){
						realTc = realTcs[i];
						break;
					}
				}
			}
		} else {
			//TODO:
		}
		return realTc;
	}
	
	public static IToolChain[] findIdenticalToolChains(IToolChain tc){
		List list = findIdenticalElements((ToolChain)tc, fToolChainSorter);
		return (ToolChain[])list.toArray(new ToolChain[list.size()]);
	}

	public static ITool[] findIdenticalTools(ITool tool){
		List list = findIdenticalElements((Tool)tool, fToolSorter);
		return (Tool[])list.toArray(new Tool[list.size()]);
	}

	public static IBuilder[] findIdenticalBuilders(IBuilder b){
		List list = findIdenticalElements((Builder)b, fBuilderSorter);
		return (Builder[])list.toArray(new Builder[list.size()]);
	}

	public static IToolChain[] getExtensionsToolChains(String propertyType, String propertyValue){
		HashMap all = getSortedToolChains();
		List result = new ArrayList();
		for(Iterator iter = all.values().iterator(); iter.hasNext();){
			List list = (List)iter.next();
			IToolChain tc = findToolChain(list, propertyType, propertyValue);
			if(tc != null)
				result.add(tc);
		}
		return (IToolChain[])result.toArray(new ToolChain[result.size()]);
	}
	
	public static void resortToolChains(){
		fSortedToolChains = null;
		getSortedToolChains();
	}

	public static void resortTools(){
		fSortedTools = null;
		getSortedTools();
	}

	public static void resortBuilders(){
		fSortedBuilders = null;
		getSortedBuilders();
	}
	
	private static IToolChain findToolChain(List list, String propertyType, String propertyValue){
		ToolChain bestMatch = null;
		IConfiguration cfg = null;
		IProjectType type = null;
		for(int i = 0; i < list.size(); i++){
			ToolChain tc = (ToolChain)list.get(i);
			if(!tc.supportsValue(propertyType, propertyValue))
				return null;
			
			if(!tc.supportsBuild(true))
				return null;
			
			if(bestMatch == null)
				bestMatch = tc;
			
			IConfiguration tcCfg = tc.getParent();
			if(tcCfg != null){
				if(cfg == null){
					bestMatch = tc;
					cfg = tcCfg;
				}
				
				IBuildObjectProperties props =tcCfg.getBuildProperties();
				IBuildProperty prop = props.getProperty(propertyType);
				if(prop != null && propertyValue.equals(prop.getValue().getId())){
					bestMatch = tc;
					cfg = tcCfg;
				}

				IProjectType tcType = tcCfg.getProjectType();
				if(tcType != null){
					if(type == null){
						type = tcType;
						bestMatch = tc;
					}
					props = tcType.getBuildProperties();
					prop = props.getProperty(propertyType); 
					if(prop != null && propertyValue.equals(prop.getValue().getId())){
						type = tcType;
						bestMatch = tc;
						break;
					}
				}
			}
		}
		
		return bestMatch;
	}

	private static List findIdenticalElements(IMatchKeyProvider p, ISorter sorter){
		List list = p.getIdenticalList();
		if(list == null){
			sorter.sort();
			list = p.getIdenticalList();
			if(list == null){
				list = new ArrayList(0);
				p.setIdenticalList(list);
			}
		}

		return list;
	}
	
	
	
	public static IBuildPropertyManager getBuildPropertyManager(){
		return BuildPropertyManager.getInstance();
	}
	
	/**
	 * Returns the configurations referenced by this configuration.
	 * Returns an empty array if there are no referenced configurations.
	 *
	 * @see CoreModelUtil.getReferencedConfigurations()
	 * @return an array of IConfiguration objects referenced by this IConfiguration
	 */
	public static IConfiguration[] getReferencedConfigurations(IConfiguration config){
		ICConfigurationDescription cfgDes = getDescriptionForConfiguration(config);
		if(cfgDes != null){
			ICConfigurationDescription[] descs= CoreModelUtil.getReferencedConfigurationDescriptions(cfgDes, false);
			List result = new ArrayList();
			for(int i=0; i<descs.length; i++) {
				IConfiguration cfg = getConfigurationForDescription(descs[i]);
				if(cfg != null) {
					result.add(cfg);
				}
			}
			return (IConfiguration[]) result.toArray(new IConfiguration[result.size()]);
		}

		return new Configuration[0];
	}
	
	public static void buildConfigurations(IConfiguration[] configs, IProgressMonitor monitor) throws CoreException{
		buildConfigurations(configs, null, monitor);
	}

	public static void buildConfigurations(IConfiguration[] configs, IBuilder builder, IProgressMonitor monitor) throws CoreException{
		Map map = sortConfigs(configs);
		for(Iterator iter = map.entrySet().iterator(); iter.hasNext();){
			Map.Entry entry = (Map.Entry)iter.next();
			IProject proj = (IProject)entry.getKey();
			IConfiguration[] cfgs = (IConfiguration[])entry.getValue();
			buildConfigurations(proj, cfgs, builder, monitor);
		}
	}

	private static Map sortConfigs(IConfiguration cfgs[]){
		Map cfgMap = new HashMap();
		
		for(int i = 0; i < cfgs.length; i++){
			IConfiguration cfg = cfgs[i];
			IProject proj = cfg.getOwner().getProject();
			Set set = (Set)cfgMap.get(proj);
			if(set == null){
				set = new HashSet();
				cfgMap.put(proj, set);
			}
			
			set.add(cfg);
		}
		
		if(cfgMap.size() != 0){
			for(Iterator iter = cfgMap.entrySet().iterator(); iter.hasNext();){
				Map.Entry entry = (Map.Entry)iter.next();
				Set set = (Set)entry.getValue();
				entry.setValue(set.toArray(new Configuration[set.size()]));
			}
		}
		
		return cfgMap;
	}

	private static void buildConfigurations(final IProject project, IConfiguration[] configs, IBuilder builder, final IProgressMonitor monitor) throws CoreException{
//		final IProject project = configs[0].getOwner().getProject();
		final boolean runAllBuidlers = false;
		final Map map = builder != null ?
				BuilderFactory.createBuildArgs(configs, builder)
				: BuilderFactory.createBuildArgs(configs);
		
		IWorkspaceRunnable op = new IWorkspaceRunnable() {

			/*
			 * (non-Javadoc)
			 * 
			 * @see org.eclipse.core.resources.IWorkspaceRunnable#run(org.eclipse.core.runtime.IProgressMonitor)
			 */
			public void run(IProgressMonitor monitor) throws CoreException {
				if (runAllBuidlers) {
					ICommand[] commands = project.getDescription().getBuildSpec();
					monitor.beginTask("", commands.length); //$NON-NLS-1$
					for (int i = 0; i < commands.length; i++) {
						if (commands[i].getBuilderName().equals(CommonBuilder.BUILDER_ID)) {
							project.build(IncrementalProjectBuilder.FULL_BUILD, CommonBuilder.BUILDER_ID, map, new SubProgressMonitor(monitor, 1));
						} else {
							project.build(IncrementalProjectBuilder.FULL_BUILD, commands[i].getBuilderName(),
							commands[i].getArguments(), new SubProgressMonitor(monitor, 1));
						}
					}
					monitor.done();
				} else {
					project.build(IncrementalProjectBuilder.FULL_BUILD, CommonBuilder.BUILDER_ID, map, monitor);
				}
			}
		};
		try {
			ResourcesPlugin.getWorkspace().run(op, monitor);
		} finally {
			monitor.done();
		}

	}
	
	public static IBuilder getInternalBuilder(){
		return getExtensionBuilder(INTERNAL_BUILDER_ID);
	}
	
	public static ITool getExtensionTool(ITool tool){
		ITool extTool = tool;
		for(;extTool != null && !extTool.isExtensionElement(); extTool = extTool.getSuperClass());
		return extTool;
	}

	public static IInputType getExtensionInputType(IInputType inType){
		IInputType extIT = inType;
		for(;extIT != null && !extIT.isExtensionElement(); extIT = extIT.getSuperClass());
		return extIT;
	}

	public static IConfiguration getPreferenceConfiguration(boolean write){
		try {
			ICConfigurationDescription des = CCorePlugin.getDefault().getPreferenceConfiguration(CFG_DATA_PROVIDER_ID, write);
			if(des != null)
				return getConfigurationForDescription(des);
		} catch (CoreException e) {
			ManagedBuilderCorePlugin.log(e);
		}
		return null;
	}

	public static void setPreferenceConfiguration(IConfiguration cfg) throws CoreException{
		ICConfigurationDescription des = getDescriptionForConfiguration(cfg);
		if(des != null)
			CCorePlugin.getDefault().setPreferenceConfiguration(CFG_DATA_PROVIDER_ID, des);
	}
	
	static synchronized void updateLoaddedInfo(IProject fromProject, IProject toProject, IManagedBuildInfo info){
		try {
			setLoaddedBuildInfo(fromProject, null);
			setLoaddedBuildInfo(toProject, info);
		} catch (CoreException e) {
		}
	}

}
