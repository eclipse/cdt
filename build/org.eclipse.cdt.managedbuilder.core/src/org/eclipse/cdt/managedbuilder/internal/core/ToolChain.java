/*******************************************************************************
 * Copyright (c) 2004, 2007 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.managedbuilder.internal.core;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.StringTokenizer;

import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.cdt.core.settings.model.extension.CTargetPlatformData;
import org.eclipse.cdt.make.core.scannerconfig.IDiscoveredPathManager;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyType;
import org.eclipse.cdt.managedbuilder.buildproperties.IBuildPropertyValue;
import org.eclipse.cdt.managedbuilder.core.IBuildObject;
import org.eclipse.cdt.managedbuilder.core.IBuildPropertiesRestriction;
import org.eclipse.cdt.managedbuilder.core.IBuilder;
import org.eclipse.cdt.managedbuilder.core.IConfiguration;
import org.eclipse.cdt.managedbuilder.core.IFolderInfo;
import org.eclipse.cdt.managedbuilder.core.IManagedConfigElement;
import org.eclipse.cdt.managedbuilder.core.IManagedIsToolChainSupported;
import org.eclipse.cdt.managedbuilder.core.IManagedProject;
import org.eclipse.cdt.managedbuilder.core.IOptionPathConverter;
import org.eclipse.cdt.managedbuilder.core.IOutputType;
import org.eclipse.cdt.managedbuilder.core.IProjectType;
import org.eclipse.cdt.managedbuilder.core.IResourceInfo;
import org.eclipse.cdt.managedbuilder.core.ITargetPlatform;
import org.eclipse.cdt.managedbuilder.core.ITool;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.core.ManagedBuildManager;
import org.eclipse.cdt.managedbuilder.envvar.IConfigurationEnvironmentVariableSupplier;
import org.eclipse.cdt.managedbuilder.internal.dataprovider.ConfigurationDataProvider;
import org.eclipse.cdt.managedbuilder.internal.enablement.OptionEnablementExpression;
import org.eclipse.cdt.managedbuilder.macros.IConfigurationBuildMacroSupplier;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IConfigurationElement;
import org.eclipse.core.runtime.IExtension;
import org.eclipse.core.runtime.IExtensionPoint;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.PluginVersionIdentifier;

public class ToolChain extends HoldsOptions implements IToolChain, IBuildPropertiesRestriction, IMatchKeyProvider {

	private static final String EMPTY_STRING = new String();

	private static final String REBUILD_STATE = "rebuildState";  //$NON-NLS-1$

	private static final boolean resolvedDefault = true;
	
	//  Superclass
	//  Note that superClass itself is defined in the base and that the methods
	//  getSuperClass() and setSuperClassInternal(), defined in ToolChain must be used  
	//  to access it. This avoids widespread casts from IHoldsOptions to IToolChain.
	private String superClassId;
	//  Parent and children
	private IConfiguration config;
	private List toolList;
	private Map toolMap;
	private TargetPlatform targetPlatform;
	private Builder builder;
	//  Managed Build model attributes
	private String unusedChildren;
	private String errorParserIds;
	private List osList;
	private List archList;
	private String targetToolIds;
	private String secondaryOutputIds;
	private Boolean isAbstract;
    private String scannerConfigDiscoveryProfileId;
	private String versionsSupported;
	private String convertToId;
	private IConfigurationElement managedIsToolChainSupportedElement = null;
	private IManagedIsToolChainSupported managedIsToolChainSupported = null;
	private IConfigurationElement environmentVariableSupplierElement = null;
	private IConfigurationEnvironmentVariableSupplier environmentVariableSupplier = null;
	private IConfigurationElement buildMacroSupplierElement = null;
	private IConfigurationBuildMacroSupplier buildMacroSupplier = null;
	private IConfigurationElement pathconverterElement = null ;
	private IOptionPathConverter optionPathConverter = null ;
	private Boolean supportsManagedBuild;
	private boolean isTest;
	private SupportedProperties supportedProperties;
	private String nonInternalBuilderId;

	//  Miscellaneous
	private boolean isExtensionToolChain = false;
	private boolean isDirty = false;
	private boolean resolved = resolvedDefault;
	//holds the user-defined macros
//	private StorableMacros userDefinedMacros;
	//holds user-defined macros
//	private StorableEnvironment userDefinedEnvironment;

	private IConfigurationElement previousMbsVersionConversionElement = null;
	private IConfigurationElement currentMbsVersionConversionElement = null;
	private boolean rebuildState;
	private BooleanExpressionApplicabilityCalculator booleanExpressionCalculator;
	
	private List identicalList;

	
	private IFolderInfo parentFolderInfo;
	
	private IDiscoveredPathManager.IDiscoveredPathInfo discoveredInfo;
	private Boolean isRcTypeBasedDiscovery;


	/*
	 *  C O N S T R U C T O R S
	 */
	
	/**
	 * This constructor is called to create a tool-chain defined by an extension point in 
	 * a plugin manifest file, or returned by a dynamic element provider
	 * 
	 * @param parent  The IConfiguration parent of this tool-chain, or <code>null</code> if
	 *                defined at the top level
	 * @param element The tool-chain definition from the manifest file or a dynamic element
	 *                provider
	 * @param managedBuildRevision the fileVersion of Managed Build System               
	 */
	public ToolChain(IFolderInfo folderInfo, IManagedConfigElement element, String managedBuildRevision) {
		// setup for resolving
		super(false);
		resolved = false;

		if(folderInfo != null){
			this.config = folderInfo.getParent();
			parentFolderInfo = folderInfo;
		}
		
		isExtensionToolChain = true;
		
		// Set the managedBuildRevision 
		setManagedBuildRevision(managedBuildRevision);

		IManagedConfigElement enablements[] = element.getChildren(OptionEnablementExpression.NAME);
		if(enablements.length > 0)
			booleanExpressionCalculator = new BooleanExpressionApplicabilityCalculator(enablements);

		loadFromManifest(element);
		
		// Hook me up to the Managed Build Manager
		ManagedBuildManager.addExtensionToolChain(this);
		
		// Load the TargetPlatform child
		IManagedConfigElement[] targetPlatforms = 
			element.getChildren(ITargetPlatform.TARGET_PLATFORM_ELEMENT_NAME);
		if (targetPlatforms.length < 1 || targetPlatforms.length > 1) {
			// TODO: Report error
		}
		if (targetPlatforms.length > 0) {
			targetPlatform = new TargetPlatform(this, targetPlatforms[0], managedBuildRevision);
		}
		
		// Load the Builder child
		IManagedConfigElement[] builders = 
			element.getChildren(IBuilder.BUILDER_ELEMENT_NAME);
		if (builders.length < 1 || builders.length > 1) {
			// TODO: Report error
		}
		if (builders.length > 0) {
			builder = new Builder(this, builders[0], managedBuildRevision);
		}

		// Load children
		IManagedConfigElement[] toolChainElements = element.getChildren();
		for (int l = 0; l < toolChainElements.length; ++l) {
			IManagedConfigElement toolChainElement = toolChainElements[l];
			if (loadChild(toolChainElement)) {
				// do nothing
			} else if (toolChainElement.getName().equals(ITool.TOOL_ELEMENT_NAME)) {
				Tool toolChild = new Tool(this, toolChainElement, managedBuildRevision);
				addTool(toolChild);
			}  else if (toolChainElement.getName().equals(SupportedProperties.SUPPORTED_PROPERTIES)){
				loadProperties(toolChainElement);
			} 
		}		
	}

	/**
	 * This constructor is called to create a ToolChain whose attributes and children will be 
	 * added by separate calls.
	 * 
	 * @param Configuration The parent of the tool chain, if any
	 * @param ToolChain The superClass, if any
	 * @param String The id for the new tool chain
	 * @param String The name for the new tool chain
	 * @param boolean Indicates whether this is an extension element or a managed project element
	 */
	public ToolChain(IFolderInfo folderInfo, IToolChain superClass, String Id, String name, boolean isExtensionElement) {
		super(resolvedDefault);
		this.config = folderInfo.getParent();
		parentFolderInfo = folderInfo;
		
		setSuperClassInternal(superClass);
		setManagedBuildRevision(config.getManagedBuildRevision());
		
		if (getSuperClass() != null) {
			superClassId = getSuperClass().getId();
		}
		setId(Id);
		setName(name);
		setVersion(getVersionFromId());
		
		isExtensionToolChain = isExtensionElement;
		if (isExtensionElement) {
			// Hook me up to the Managed Build Manager
			ManagedBuildManager.addExtensionToolChain(this);
		} else {
			setDirty(true);
			setRebuildState(true);
		}
	}

	/**
	 * Create a <code>ToolChain</code> based on the specification stored in the 
	 * project file (.cdtbuild).
	 * 
	 * @param parent The <code>IConfiguration</code> the tool-chain will be added to. 
	 * @param element The XML element that contains the tool-chain settings.
	 * @param managedBuildRevision the fileVersion of Managed Build System			
	 */
	public ToolChain(IFolderInfo folderInfo, ICStorageElement element, String managedBuildRevision) {
		super(resolvedDefault);
		this.config = folderInfo.getParent();
		parentFolderInfo = folderInfo;
		
		isExtensionToolChain = false;
		
		// Set the managedBuildRevision
		setManagedBuildRevision(managedBuildRevision);
		
		// Initialize from the XML attributes
		loadFromProject(element);

		// Load children
		ICStorageElement configElements[] = element.getChildren();
		for (int i = 0; i < configElements.length; ++i) {
			ICStorageElement configElement = configElements[i];
			if (loadChild(configElement)) {
				// do nothing
			} else if (configElement.getName().equals(ITool.TOOL_ELEMENT_NAME)) {
				Tool tool = new Tool(this, configElement, managedBuildRevision);
				addTool(tool);
			}else if (configElement.getName().equals(ITargetPlatform.TARGET_PLATFORM_ELEMENT_NAME)) {
				if (targetPlatform != null) {
					// TODO: report error
				}
				targetPlatform = new TargetPlatform(this, configElement, managedBuildRevision);
			}else if (configElement.getName().equals(IBuilder.BUILDER_ELEMENT_NAME)) {
				if (builder != null) {
					// TODO: report error
				}
				builder = new Builder(this, configElement, managedBuildRevision);
			}/*else if (configElement.getName().equals(StorableMacros.MACROS_ELEMENT_NAME)) {
				//load user-defined macros
				userDefinedMacros = new StorableMacros(configElement);

			}*/
		}
		
		String rebuild = PropertyManager.getInstance().getProperty(this, REBUILD_STATE);
		if(rebuild == null || Boolean.valueOf(rebuild).booleanValue())
			rebuildState = true;

	}

	/**
	 * Create a <code>ToolChain</code> based upon an existing tool chain.
	 * 
	 * @param parent The <code>IConfiguration</code> the tool-chain will be added to. 
	 * @param toolChain The existing tool-chain to clone.
	 */
	public ToolChain(IFolderInfo folderInfo, String Id, String name, Map superIdMap, ToolChain toolChain) {
		super(resolvedDefault);
		this.config = folderInfo.getParent();
		parentFolderInfo = folderInfo;
		setSuperClassInternal(toolChain.getSuperClass());
		if (getSuperClass() != null) {
			if (toolChain.superClassId != null) {
				superClassId = new String(toolChain.superClassId);
			}
		}
		setId(Id);
		setName(name);
		
		// Set the managedBuildRevision and the version
		setManagedBuildRevision(toolChain.getManagedBuildRevision());
		setVersion(getVersionFromId());	

		isExtensionToolChain = false;
		
		//  Copy the remaining attributes
		if(toolChain.versionsSupported != null) {
			versionsSupported = new String(toolChain.versionsSupported);
		}
		if(toolChain.convertToId != null) {
			convertToId = new String(toolChain.convertToId);
		}
		
		if (toolChain.unusedChildren != null) {
			unusedChildren = new String(toolChain.unusedChildren);
		}
		if (toolChain.errorParserIds != null) {
			errorParserIds = new String(toolChain.errorParserIds);
		}
		if (toolChain.osList != null) {
			osList = new ArrayList(toolChain.osList);
		}
		if (toolChain.archList != null) {
			archList = new ArrayList(toolChain.archList);
		}
		if (toolChain.targetToolIds != null) {
			targetToolIds = new String(toolChain.targetToolIds);
		}
		if (toolChain.secondaryOutputIds != null) {
			secondaryOutputIds = new String(toolChain.secondaryOutputIds);
		}
		if (toolChain.isAbstract != null) {
			isAbstract = new Boolean(toolChain.isAbstract.booleanValue());
		}
        if (toolChain.scannerConfigDiscoveryProfileId != null) {
            scannerConfigDiscoveryProfileId = new String(toolChain.scannerConfigDiscoveryProfileId);
        }
        
		isRcTypeBasedDiscovery = toolChain.isRcTypeBasedDiscovery;

       	supportsManagedBuild = toolChain.supportsManagedBuild; 

		managedIsToolChainSupportedElement = toolChain.managedIsToolChainSupportedElement; 
		managedIsToolChainSupported = toolChain.managedIsToolChainSupported; 

		environmentVariableSupplierElement = toolChain.environmentVariableSupplierElement;
		environmentVariableSupplier = toolChain.environmentVariableSupplier;

		buildMacroSupplierElement = toolChain.buildMacroSupplierElement;
		buildMacroSupplier = toolChain.buildMacroSupplier;

		pathconverterElement = toolChain.pathconverterElement ;
		optionPathConverter = toolChain.optionPathConverter ;

		nonInternalBuilderId = toolChain.nonInternalBuilderId;
		
		//  Clone the children in superclass
		boolean copyIds = toolChain.getId().equals(id);
		super.copyChildren(toolChain);
		//  Clone the children
		if (toolChain.builder != null) {
			String subId;
			String subName;
			
			if (toolChain.builder.getSuperClass() != null) {
				subId = copyIds ? toolChain.builder.getId() : ManagedBuildManager.calculateChildId(
							toolChain.builder.getSuperClass().getId(),
							null);
				subName = toolChain.builder.getSuperClass().getName();
			} else {
				subId = copyIds ? toolChain.builder.getId() : ManagedBuildManager.calculateChildId(
						toolChain.builder.getId(),
						null);
				subName = toolChain.builder.getName();
			}

			builder = new Builder(this, subId, subName, toolChain.builder);
		}
//		if (toolChain.targetPlatform != null) 
		{
		ITargetPlatform tpBase = toolChain.getTargetPlatform();
		if(tpBase != null){
		ITargetPlatform extTp = tpBase;
		for(;extTp != null && !extTp.isExtensionElement();extTp = extTp.getSuperClass());

		String subId;
		if(copyIds){
			subId = tpBase != null ? tpBase.getId() : ManagedBuildManager.calculateChildId(getId(), null);
		} else {
			subId = extTp != null ? ManagedBuildManager.calculateChildId(extTp.getId(), null):
				ManagedBuildManager.calculateChildId(getId(), null);
		}
		String subName = tpBase.getName();

//			if (toolChain.targetPlatform.getSuperClass() != null) {
//				subId = toolChain.targetPlatform.getSuperClass().getId() + "." + nnn;		//$NON-NLS-1$
//				subName = toolChain.targetPlatform.getSuperClass().getName(); 
//			} else {
//				subId = toolChain.targetPlatform.getId() + "." + nnn;		//$NON-NLS-1$
//				subName = toolChain.targetPlatform.getName();
//			}
			targetPlatform = new TargetPlatform(this, subId, subName, (TargetPlatform)tpBase);
		}
//		
		}
		
		IConfiguration cfg = parentFolderInfo.getParent();
		if (toolChain.toolList != null) {
			Iterator iter = toolChain.getToolList().listIterator();
			while (iter.hasNext()) {
			    Tool toolChild = (Tool) iter.next();
//				int nnn = ManagedBuildManager.getRandomNumber();
				String subId = null;
//				String tmpId;
				String subName;
//				String version;
				ITool extTool = ManagedBuildManager.getExtensionTool(toolChild);
				Map curIdMap = (Map)superIdMap.get(folderInfo.getPath());
				if(curIdMap != null){
					if(extTool != null)
						subId = (String)curIdMap.get(extTool.getId());
				}
				
				subName = toolChild.getName();
				
				if(subId == null){
					if (extTool != null) {
						subId = copyIds ? toolChild.getId() : ManagedBuildManager.calculateChildId(extTool.getId(), null);
	//					subName = toolChild.getSuperClass().getName();
					} else {
						subId = copyIds ? toolChild.getId() : ManagedBuildManager.calculateChildId(toolChild.getId(), null);
	//					subName = toolChild.getName();
					}
				}
//				version = ManagedBuildManager.getVersionFromIdAndVersion(tmpId);
//				if ( version != null) {		// If the 'tmpId' contains version information
//					subId = ManagedBuildManager.getIdFromIdAndVersion(tmpId) + "." + nnn + "_" + version;		//$NON-NLS-1$ //$NON-NLS-2$
//				} else {
//					subId = tmpId + "." + nnn;		//$NON-NLS-1$
//				}

				//  The superclass for the cloned tool is not the same as the one from the tool being cloned.
				//  The superclasses reside in different configurations. 
				ITool toolSuperClass = null;
				String superId = null;
				//  Search for the tool in this configuration that has the same grand-superClass as the 
				//  tool being cloned
				ITool otherSuperTool = toolChild.getSuperClass();
				if(otherSuperTool != null){
					if(otherSuperTool.isExtensionElement()){
						toolSuperClass = otherSuperTool;
					} else {
						IResourceInfo otherRcInfo = otherSuperTool.getParentResourceInfo();
						IResourceInfo thisRcInfo = cfg.getResourceInfo(otherRcInfo.getPath(), true);
						ITool otherExtTool = ManagedBuildManager.getExtensionTool(otherSuperTool);
						if(otherExtTool != null){
							if(thisRcInfo != null){
								ITool tools[] = thisRcInfo.getTools();
								for(int i = 0; i < tools.length; i++){
									ITool thisExtTool = ManagedBuildManager.getExtensionTool(tools[i]);
									if(otherExtTool.equals(thisExtTool)){
										toolSuperClass = tools[i];
										superId = toolSuperClass.getId();
										break;
									}
								}
							} else {
								superId = copyIds ? otherSuperTool.getId() : ManagedBuildManager.calculateChildId(otherExtTool.getId(), null);
								Map idMap = (Map)superIdMap.get(otherRcInfo.getPath());
								if(idMap == null){
									idMap = new HashMap();
									superIdMap.put(otherRcInfo.getPath(), idMap);
								}
								idMap.put(otherExtTool.getId(), superId);
							}
						}
					}
				}
//				Tool newTool = new Tool(this, (Tool)null, subId, subName, toolChild);
//				addTool(newTool);
				
				Tool newTool = null;
				if(toolSuperClass != null)
					newTool = new Tool(this, toolSuperClass, subId, subName, toolChild);
				else if(superId != null)
					newTool = new Tool(this, superId, subId, subName, toolChild);
				else{
					//TODO: Error
				} 
				if(newTool != null)
					addTool(newTool);

			}
		}
		
		if(copyIds){
			rebuildState = toolChain.rebuildState;
			isDirty = toolChain.isDirty;
		} else {
			setDirty(true);
			setRebuildState(true);
		}
	}

	/*
	 *  E L E M E N T   A T T R I B U T E   R E A D E R S   A N D   W R I T E R S
	 */
	
	/* (non-Javadoc)
	 * Loads the tool-chain information from the ManagedConfigElement specified in the 
	 * argument.
	 * 
	 * @param element Contains the tool-chain information 
	 */
	protected void loadFromManifest(IManagedConfigElement element) {
		ManagedBuildManager.putConfigElement(this, element);
		
		// id
		setId(element.getAttribute(IBuildObject.ID));
		
		// Get the name
		setName(element.getAttribute(IBuildObject.NAME));
		
		// version
		setVersion(getVersionFromId());
		
		// superClass
		superClassId = element.getAttribute(IProjectType.SUPERCLASS);

		// Get the unused children, if any
		unusedChildren = element.getAttribute(IProjectType.UNUSED_CHILDREN); 
		
		// isAbstract
        String isAbs = element.getAttribute(IProjectType.IS_ABSTRACT);
        if (isAbs != null){
    		isAbstract = new Boolean("true".equals(isAbs)); //$NON-NLS-1$
        }
		
		// Get the semicolon separated list of IDs of the error parsers
		errorParserIds = element.getAttribute(ERROR_PARSERS);
		
		// Get the semicolon separated list of IDs of the secondary outputs
		secondaryOutputIds = element.getAttribute(SECONDARY_OUTPUTS);
		
		// Get the target tool id
		targetToolIds = element.getAttribute(TARGET_TOOL);
		
		// Get the scanner config discovery profile id
        scannerConfigDiscoveryProfileId = element.getAttribute(SCANNER_CONFIG_PROFILE_ID);
        String tmp = element.getAttribute(RESOURCE_TYPE_BASED_DISCOVERY);
        if(tmp != null)
        	isRcTypeBasedDiscovery = Boolean.valueOf(tmp); 
        
		// Get the 'versionsSupported' attribute
		versionsSupported =element.getAttribute(VERSIONS_SUPPORTED);
		
		// Get the 'convertToId' attribute
		convertToId = element.getAttribute(CONVERT_TO_ID);
		
		tmp = element.getAttribute(SUPPORTS_MANAGED_BUILD);
		if(tmp != null)
			supportsManagedBuild = Boolean.valueOf(tmp);
		
        tmp = element.getAttribute(IS_SYSTEM);
        if(tmp != null)
        	isTest = Boolean.valueOf(tmp).booleanValue();

		
		// Get the comma-separated list of valid OS
		String os = element.getAttribute(OS_LIST);
		if (os != null) {
			osList = new ArrayList();
			String[] osTokens = os.split(","); //$NON-NLS-1$
			for (int i = 0; i < osTokens.length; ++i) {
				osList.add(osTokens[i].trim());
			}
		}
		
		// Get the comma-separated list of valid Architectures
		String arch = element.getAttribute(ARCH_LIST);
		if (arch != null) {
			archList = new ArrayList();
			String[] archTokens = arch.split(","); //$NON-NLS-1$
			for (int j = 0; j < archTokens.length; ++j) {
				archList.add(archTokens[j].trim());
			}
		}
		
		// Get the isToolchainSupported configuration element
		String managedIsToolChainSupported = element.getAttribute(IS_TOOL_CHAIN_SUPPORTED); 
		if (managedIsToolChainSupported != null && element instanceof DefaultManagedConfigElement) {
			managedIsToolChainSupportedElement = ((DefaultManagedConfigElement)element).getConfigurationElement();			
		} 
		
		// Get the environmentVariableSupplier configuration element
		String environmentVariableSupplier = element.getAttribute(CONFIGURATION_ENVIRONMENT_SUPPLIER); 
		if(environmentVariableSupplier != null && element instanceof DefaultManagedConfigElement){
			environmentVariableSupplierElement = ((DefaultManagedConfigElement)element).getConfigurationElement();
		}

		// Get the configurationMacroSupplier configuration element
		String buildMacroSupplier = element.getAttribute(CONFIGURATION_MACRO_SUPPLIER); 
		if(buildMacroSupplier != null && element instanceof DefaultManagedConfigElement){
			buildMacroSupplierElement = ((DefaultManagedConfigElement)element).getConfigurationElement();
		}

		// optionPathConverter
		String pathconverterTypeName = element.getAttribute(ITool.OPTIONPATHCONVERTER);
		if (pathconverterTypeName != null && element instanceof DefaultManagedConfigElement) {
			pathconverterElement = ((DefaultManagedConfigElement)element).getConfigurationElement();			
		}
		
		nonInternalBuilderId = element.getAttribute(NON_INTERNAL_BUILDER_ID);
	}
	
	
	/* (non-Javadoc)
	 * Initialize the tool-chain information from the XML element 
	 * specified in the argument
	 * 
	 * @param element An XML element containing the tool-chain information 
	 */
	protected void loadFromProject(ICStorageElement element) {
		
		// id
		setId(element.getAttribute(IBuildObject.ID));

		// name
		if (element.getAttribute(IBuildObject.NAME) != null) {
			setName(element.getAttribute(IBuildObject.NAME));
		}

		// version
		setVersion(getVersionFromId());

		// superClass
		superClassId = element.getAttribute(IProjectType.SUPERCLASS);
		if (superClassId != null && superClassId.length() > 0) {
			setSuperClassInternal( ManagedBuildManager.getExtensionToolChain(superClassId) );
			// Check for migration support
			checkForMigrationSupport();
		}

		// Get the unused children, if any
		if (element.getAttribute(IProjectType.UNUSED_CHILDREN) != null) {
				unusedChildren = element.getAttribute(IProjectType.UNUSED_CHILDREN); 
		}
		
		// isAbstract
		if (element.getAttribute(IProjectType.IS_ABSTRACT) != null) {
			String isAbs = element.getAttribute(IProjectType.IS_ABSTRACT);
			if (isAbs != null){
				isAbstract = new Boolean("true".equals(isAbs)); //$NON-NLS-1$
			}
		}
		
		// Get the semicolon separated list of IDs of the error parsers
		if (element.getAttribute(ERROR_PARSERS) != null) {
			errorParserIds = element.getAttribute(ERROR_PARSERS);
		}
		
		// Get the semicolon separated list of IDs of the secondary outputs
		if (element.getAttribute(SECONDARY_OUTPUTS) != null) {
			secondaryOutputIds = element.getAttribute(SECONDARY_OUTPUTS);
		}
		
		// Get the target tool id
		if (element.getAttribute(TARGET_TOOL) != null) {
			targetToolIds = element.getAttribute(TARGET_TOOL);
		}
		
        // Get the scanner config discovery profile id
        if (element.getAttribute(SCANNER_CONFIG_PROFILE_ID) != null) {
            scannerConfigDiscoveryProfileId = element.getAttribute(SCANNER_CONFIG_PROFILE_ID);
        }
        
		// Get the 'versionSupported' attribute
		if (element.getAttribute(VERSIONS_SUPPORTED) != null) {
			versionsSupported = element.getAttribute(VERSIONS_SUPPORTED);
		}
		
		// Get the 'convertToId' id
		if (element.getAttribute(CONVERT_TO_ID) != null) {
			convertToId = element.getAttribute(CONVERT_TO_ID);
		}
		
		// Get the comma-separated list of valid OS
		if (element.getAttribute(OS_LIST) != null) {
			String os = element.getAttribute(OS_LIST);
			if (os != null) {
				osList = new ArrayList();
				String[] osTokens = os.split(","); //$NON-NLS-1$
				for (int i = 0; i < osTokens.length; ++i) {
					osList.add(osTokens[i].trim());
				}
			}
		}
		
		// Get the comma-separated list of valid Architectures
		if (element.getAttribute(ARCH_LIST) != null) {
			String arch = element.getAttribute(ARCH_LIST);
			if (arch != null) {
				archList = new ArrayList();
				String[] archTokens = arch.split(","); //$NON-NLS-1$
				for (int j = 0; j < archTokens.length; ++j) {
					archList.add(archTokens[j].trim());
				}
			}
		}
		
		// Note: optionPathConverter cannot be specified in a project file because
		//       an IConfigurationElement is needed to load it!
		if (pathconverterElement != null) {
			//  TODO:  issue warning?
		}

		// Get the scanner config discovery profile id
        scannerConfigDiscoveryProfileId = element.getAttribute(SCANNER_CONFIG_PROFILE_ID);
        String tmp = element.getAttribute(RESOURCE_TYPE_BASED_DISCOVERY);
        if(tmp != null)
        	isRcTypeBasedDiscovery = Boolean.valueOf(tmp); 

		nonInternalBuilderId = element.getAttribute(NON_INTERNAL_BUILDER_ID);
		
//		String tmp = element.getAttribute(name)
	}

	/**
	 * Persist the tool-chain to the project file.
	 * 
	 * @param doc
	 * @param element
	 */
	public void serialize(ICStorageElement element) {
		try {		
			if (getSuperClass() != null)
				element.setAttribute(IProjectType.SUPERCLASS, getSuperClass().getId());
			
			element.setAttribute(IBuildObject.ID, id);
			
			if (name != null) {
				element.setAttribute(IBuildObject.NAME, name);
			}
	
			if (unusedChildren != null) {
				element.setAttribute(IProjectType.UNUSED_CHILDREN, unusedChildren);
			}
			
			if (isAbstract != null) {
				element.setAttribute(IProjectType.IS_ABSTRACT, isAbstract.toString());
			}
	
			if (errorParserIds != null) {
				element.setAttribute(ERROR_PARSERS, errorParserIds);
			}
	
			if (secondaryOutputIds != null) {
				element.setAttribute(SECONDARY_OUTPUTS, secondaryOutputIds);
			}
	        
	        if (targetToolIds != null) {
	            element.setAttribute(TARGET_TOOL, targetToolIds);
	        }
	        
	        if (scannerConfigDiscoveryProfileId != null) {
	            element.setAttribute(SCANNER_CONFIG_PROFILE_ID, scannerConfigDiscoveryProfileId);
	        }
	
			// versionsSupported
			if (versionsSupported != null) {
				element.setAttribute(VERSIONS_SUPPORTED, versionsSupported);
			}
			
			// convertToId
			if (convertToId != null) {
				element.setAttribute(CONVERT_TO_ID, convertToId);
			}
			
			if (osList != null) {
				Iterator osIter = osList.listIterator();
				String listValue = EMPTY_STRING;
				while (osIter.hasNext()) {
					String current = (String) osIter.next();
					listValue += current;
					if ((osIter.hasNext())) {
						listValue += ","; //$NON-NLS-1$
					}
				}
				element.setAttribute(OS_LIST, listValue);
			}
	
			if (archList != null) {
				Iterator archIter = archList.listIterator();
				String listValue = EMPTY_STRING;
				while (archIter.hasNext()) {
					String current = (String) archIter.next();
					listValue += current;
					if ((archIter.hasNext())) {
						listValue += ","; //$NON-NLS-1$
					}
				}
				element.setAttribute(ARCH_LIST, listValue);
			}
			
			// Serialize elements from my super class
			super.serialize(element);
	
			// Serialize my children
			if (targetPlatform != null) {
				ICStorageElement targetPlatformElement = element.createChild(ITargetPlatform.TARGET_PLATFORM_ELEMENT_NAME);
				targetPlatform.serialize(targetPlatformElement);
			}
			if (builder != null) {
				ICStorageElement builderElement = element.createChild(IBuilder.BUILDER_ELEMENT_NAME);
				builder.serialize(builderElement);
			}
			List toolElements = getToolList();
			Iterator iter = toolElements.listIterator();
			while (iter.hasNext()) {
				Tool tool = (Tool) iter.next();
				ICStorageElement toolElement = element.createChild(ITool.TOOL_ELEMENT_NAME);
				tool.serialize(toolElement);
			}
			
			// Note: isToolChainSupported cannot be specified in a project file because
			//       an IConfigurationElement is needed to load it!
			if (managedIsToolChainSupportedElement != null) {
				//  TODO:  issue warning?
			}
	
			// Note: environmentVariableSupplier cannot be specified in a project file because
			//       an IConfigurationElement is needed to load it!
			if(environmentVariableSupplierElement != null) {
				//  TODO:  issue warning?
			}

			// Note: buildMacroSupplier cannot be specified in a project file because
			//       an IConfigurationElement is needed to load it!
			if(buildMacroSupplierElement != null) {
				//  TODO:  issue warning?
			}

			//serialize user-defined macros
/*			if(userDefinedMacros != null){
				ICStorageElement macrosElement = element.createChild(StorableMacros.MACROS_ELEMENT_NAME);
				userDefinedMacros.serialize(macrosElement);
			}
*/			
			// Note: optionPathConverter cannot be specified in a project file because
			//       an IConfigurationElement is needed to load it!
			if (pathconverterElement != null) {
				//  TODO:  issue warning?
			}

//			if(userDefinedEnvironment != null)
//				EnvironmentVariableProvider.fUserSupplier.storeEnvironment(getParent(),true);

			if(nonInternalBuilderId != null)
				element.setAttribute(NON_INTERNAL_BUILDER_ID, nonInternalBuilderId);

			
			if(scannerConfigDiscoveryProfileId != null)
				element.setAttribute(SCANNER_CONFIG_PROFILE_ID, scannerConfigDiscoveryProfileId);
			if(isRcTypeBasedDiscovery != null)
				element.setAttribute(RESOURCE_TYPE_BASED_DISCOVERY, isRcTypeBasedDiscovery.toString());
			saveRebuildState();

			// I am clean now
			isDirty = false;
		} catch (Exception e) {
			// TODO: issue an error message
		}
}

	/*
	 *  P A R E N T   A N D   C H I L D   H A N D L I N G
	 */

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.build.managed.IToolChain#getConfiguration()
	 */
	public IConfiguration getParent() {
		return config;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.build.managed.IToolChain#createTargetPlatform(ITargetPlatform, String, String, boolean)
	 */
	public ITargetPlatform createTargetPlatform(ITargetPlatform superClass, String id, String name, boolean isExtensionElement) {
		targetPlatform = new TargetPlatform(this, superClass, id, name, isExtensionElement);
		setDirty(true);
		return (ITargetPlatform)targetPlatform;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.build.managed.IToolChain#getTargetPlatform()
	 */
	public ITargetPlatform getTargetPlatform() {
		if (targetPlatform == null) {
			if (getSuperClass() != null) {
				return getSuperClass().getTargetPlatform();
			}
		}
		return (ITargetPlatform)targetPlatform;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.build.managed.IToolChain#removeLocalTargetPlatform()
	 */
	public void removeLocalTargetPlatform() {
		if (targetPlatform == null) return;
		targetPlatform = null;
		setDirty(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.build.managed.IToolChain#createBuilder(IBuilder, String, String, boolean)
	 */
	public IBuilder createBuilder(IBuilder superClass, String id, String name, boolean isExtensionElement) {
		builder = new Builder(this, superClass, id, name, isExtensionElement);
		setDirty(true);
		return (IBuilder)builder;
	}
	
	public void setBuilder(Builder builder){
		this.builder = builder;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.build.managed.IToolChain#getBuilder()
	 */
	public IBuilder getBuilder() {
		if (builder == null) {
			if (getSuperClass() != null) {
				return getSuperClass().getBuilder();
			}
		}
		return (IBuilder)builder;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.build.managed.IToolChain#removeLocalBuilder()
	 */
	public void removeLocalBuilder() {
		if (builder == null) return;
		builder = null;
		setDirty(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.build.managed.IToolChain#createTool(ITool, String, String, boolean)
	 */
	public ITool createTool(ITool superClass, String id, String name, boolean isExtensionElement) {
		Tool tool = new Tool(this, superClass, id, name, isExtensionElement);
		addTool(tool);
		setDirty(true);
		return (ITool)tool;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.build.managed.IToolChain#getTools()
	 */
	public ITool[] getTools() {
		ITool tools[] = getAllTools();
		if(!isExtensionToolChain){
			for(int i = 0; i < tools.length; i++){
				if(tools[i].isExtensionElement()){
					String subId = ManagedBuildManager.calculateChildId(tools[i].getId(), null);				
					tools[i] = createTool(tools[i], subId, tools[i].getName(), false);
				}
			}
		}

		return tools;
	}

	public ITool[] getAllTools() {
		ITool[] tools = null;
		//  Merge our tools with our superclass' tools
		if (getSuperClass() != null) {
			tools = ((ToolChain)getSuperClass()).getAllTools();
		}
		//  Our tools take precedence
		if (tools != null) {
			Iterator iter = getToolList().listIterator();
			while (iter.hasNext()) {
				Tool tool = (Tool)iter.next();
				int j;
				for (j = 0; j < tools.length; j++) {
					ITool superTool = tool.getSuperClass();
					if(superTool != null){
						if(!superTool.isExtensionElement())
							superTool = superTool.getSuperClass();
						
						if(superTool != null && superTool.getId().equals(tools[j].getId())){
							tools[j] = tool;
							break;
						}
					}
						
//					if (tool.getSuperClass() != null  // Remove assumption that ALL tools must have superclasses  
//							&&  tool.getSuperClass().getId().equals(tools[j].getId())) {
//						tools[j] = tool;
//						break;
//					}
 				}
				//  No Match?  Insert it (may be re-ordered)
				if (j == tools.length) {
					ITool[] newTools = new ITool[tools.length + 1];
					for (int k = 0; k < tools.length; k++) {
						newTools[k] = tools[k];
					}
					newTools[j] = tool;
					tools = newTools;
				}
			}
		} else {
			tools = new ITool[getToolList().size()];
			Iterator iter = getToolList().listIterator();
			int i = 0;
			while (iter.hasNext()) {
				Tool tool = (Tool)iter.next();
				tools[i++] = (ITool)tool; 
			}
		}
		return tools;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#getTool(java.lang.String)
	 */
	public ITool getTool(String id) {
		Tool tool = (Tool)getToolMap().get(id);
		return (ITool)tool;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#getToolsBySuperClassId(java.lang.String)
	 */
	public ITool[] getToolsBySuperClassId(String id) {
		List retTools = new ArrayList();
		if (id != null) {
			//  Look for a tool with this ID, or the tool(s) with a superclass with this id
			ITool[] tools = getTools();
			for (int i = 0; i < tools.length; i++) {
				ITool targetTool = tools[i];
				ITool tool = targetTool;
				do {
					if (id.equals(tool.getId())) {
						retTools.add(targetTool);
						break;
					}		
					tool = tool.getSuperClass();
				} while (tool != null);
			}
		}
		return (ITool[])retTools.toArray( new ITool[retTools.size()]);
	}
	
	/* (non-Javadoc)
	 * Safe accessor for the list of tools.
	 * 
	 * @return List containing the tools
	 */
	public List getToolList() {
		if (toolList == null) {
			toolList = new ArrayList();
		}
		return toolList;
	}
	
	/* (non-Javadoc)
	 * Safe accessor for the map of tool ids to tools
	 * 
	 * @return
	 */
	private Map getToolMap() {
		if (toolMap == null) {
			toolMap = new HashMap();
		}
		return toolMap;
	}

	/* (non-Javadoc)
	 * Adds the Tool to the Tool-chain list and map
	 * 
	 * @param Tool
	 */
	public void addTool(Tool tool) {
		getToolList().add(tool);
		getToolMap().put(tool.getId(), tool);
	}
	
	public void removeTool(Tool tool){
		getToolList().remove(tool);
		getToolMap().remove(tool.getId());
	}

	/*
	 *  M O D E L   A T T R I B U T E   A C C E S S O R S
	 */

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#getSuperClass()
	 */
	public IToolChain getSuperClass() {
		return (IToolChain)superClass;
	}

	/* (non-Javadoc)
	 * Access function to set the superclass element that is defined in
	 * the base class.
	 */
	private void setSuperClassInternal(IToolChain superClass) {
		this.superClass = superClass;
	}
	
	public void setSuperClass(IToolChain superClass) {
		if ( this.superClass != superClass ) {
			this.superClass = superClass;
			if ( this.superClass == null) {
				superClassId = null;
			} else {
				superClassId = this.superClass.getId();
			}
		
			if(!isExtensionElement())
				setDirty(true);
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.build.managed.IToolChain#getName()
	 */
	public String getName() {
		return (name == null && getSuperClass() != null) ? getSuperClass().getName() : name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.build.managed.IToolChain#isAbstract()
	 */
	public boolean isAbstract() {
		if (isAbstract != null) {
			return isAbstract.booleanValue();
		} else {
			return false;	// Note: no inheritance from superClass
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.build.managed.IToolChain#getUnusedChildren()
	 */
	public String getUnusedChildren() {
		if (unusedChildren != null) {
			return unusedChildren;
		} else
			return EMPTY_STRING;	// Note: no inheritance from superClass
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#getErrorParserIds()
	 */
	public String getErrorParserIds() {
		String ids = errorParserIds;
		if (ids == null) {
			// If I have a superClass, ask it
			if (getSuperClass() != null) {
				ids = getSuperClass().getErrorParserIds();
			}
		}
		if (ids == null) {
			// Collect the error parsers from my children
			ids = builder.getErrorParserIds();
			ITool[] tools = getTools();
			for (int i = 0; i < tools.length; i++) {
				ITool tool = tools[i];
				String toolIds = tool.getErrorParserIds(); 
				if (toolIds != null && toolIds.length() > 0) {
					if (ids != null) {
						ids += ";"; //$NON-NLS-1$
						ids += toolIds;
					} else {
						ids = toolIds;
					}
				}
			}
		}
		return ids;
	}
	
	public String getErrorParserIdsAttribute() {
		String ids = errorParserIds;
		if (ids == null) {
			// If I have a superClass, ask it
			if (getSuperClass() != null) {
				ids = ((ToolChain)getSuperClass()).getErrorParserIdsAttribute();
			}
		}
		return ids;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#getSecondaryOutputs()
	 */
	public IOutputType[] getSecondaryOutputs() {
		IOutputType[] types = null;
		String ids = secondaryOutputIds; 
		if (ids == null) {
			if (getSuperClass() != null) {
				return getSuperClass().getSecondaryOutputs();
			}
			else {
				return new IOutputType[0];
			}			
		}
		StringTokenizer tok = new StringTokenizer(ids, ";"); //$NON-NLS-1$
		types = new IOutputType[tok.countTokens()];
		ITool[] tools = getTools();
		int i = 0;
		while (tok.hasMoreElements()) {
			String id = tok.nextToken();
			for (int j=0; j<tools.length; j++) {
				IOutputType type;
				type = tools[j].getOutputTypeById(id);
				if (type != null) {
					types[i++] = type;
					break;
				}
			}
		}
		return types;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#getTargetToolIds()
	 */
	public String getTargetToolIds() {
		if (targetToolIds == null) {
			// Ask superClass for its list
			if (getSuperClass() != null) {
				return getSuperClass().getTargetToolIds();
			} else {
				return null;
			}
		}
		return targetToolIds;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#getTargetToolList()
	 */
	public String[] getTargetToolList() {
		String IDs = getTargetToolIds();
		String[] targetTools;
		if (IDs != null) {
			// Check for an empty string
			if (IDs.length() == 0) {
				targetTools = new String[0];
			} else {
				StringTokenizer tok = new StringTokenizer(IDs, ";"); //$NON-NLS-1$
				List list = new ArrayList(tok.countTokens());
				while (tok.hasMoreElements()) {
					list.add(tok.nextToken());
				}
				String[] strArr = {""};	//$NON-NLS-1$
				targetTools = (String[]) list.toArray(strArr);
			}
		} else {
			targetTools = new String[0];
		}
		return targetTools;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#getErrorParserIds(IConfiguration)
	 */
	public String getErrorParserIds(IConfiguration config) {
		String ids = errorParserIds;
		if (ids == null) {
			// If I have a superClass, ask it
			if (getSuperClass() != null) {
				ids = getSuperClass().getErrorParserIds(config);
			}
		}
		if (ids == null) {
			// Collect the error parsers from my children
		    if (builder != null) {
		        ids = builder.getErrorParserIds();
		    }
			ITool[] tools = config.getFilteredTools();
			for (int i = 0; i < tools.length; i++) {
				ITool tool = tools[i];
				String toolIds = tool.getErrorParserIds(); 
				if (toolIds != null && toolIds.length() > 0) {
					if (ids != null) {
						ids += ";"; //$NON-NLS-1$
						ids += toolIds;
					} else {
						ids = toolIds;
					}
				}
			}
		}
		return ids;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#getErrorParserList()
	 */
	public String[] getErrorParserList() {
		String parserIDs = getErrorParserIds();
		String[] errorParsers;
		if (parserIDs != null) {
			// Check for an empty string
			if (parserIDs.length() == 0) {
				errorParsers = new String[0];
			} else {
				StringTokenizer tok = new StringTokenizer(parserIDs, ";"); //$NON-NLS-1$
				List list = new ArrayList(tok.countTokens());
				while (tok.hasMoreElements()) {
					list.add(tok.nextToken());
				}
				String[] strArr = {""};	//$NON-NLS-1$
				errorParsers = (String[]) list.toArray(strArr);
			}
		} else {
			errorParsers = new String[0];
		}
		return errorParsers;
	}
	
	public Set contributeErrorParsers(FolderInfo info, Set set, boolean includeChildren){
		if(set == null)
			set = new HashSet();
		
		String parserIDs = getErrorParserIdsAttribute();
		if (parserIDs != null && parserIDs.length() != 0) {
			StringTokenizer tok = new StringTokenizer(parserIDs, ";"); //$NON-NLS-1$
			while (tok.hasMoreElements()) {
				set.add(tok.nextToken());
			}
		}
		
		if(includeChildren){
			ITool tools[] = info.getFilteredTools();
			info.contributeErrorParsers(tools, set);
			
			if(info.isRoot()){
				Builder builder = (Builder)getBuilder();
				builder.contributeErrorParsers(set);
			}
		}
		return set;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#getArchList()
	 */
	public String[] getArchList() {
		if (archList == null) {
			// Ask superClass for its list
			if (getSuperClass() != null) {
				return getSuperClass().getArchList();
			} else {
				// I have no superClass and no defined list
				return new String[] {"all"}; //$NON-NLS-1$
			}
		}
		return (String[]) archList.toArray(new String[archList.size()]);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#getOSList()
	 */
	public String[] getOSList() {
		if (osList == null) {
			// Ask superClass for its list
			if (getSuperClass() != null) {
				return getSuperClass().getOSList();
			} else {
				// I have no superClass and no defined filter list
				return new String[] {"all"};	//$NON-NLS-1$
			}
		}
		return (String[]) osList.toArray(new String[osList.size()]);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#setIsAbstract(boolean)
	 */
	public void setIsAbstract(boolean b) {
		isAbstract = new Boolean(b);
		setDirty(true);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#setErrorParserIds(String)
	 */
	public void setErrorParserIds(String ids) {
		String currentIds = getErrorParserIds();
		if (ids == null && currentIds == null) return;
		if (currentIds == null || ids == null || !(currentIds.equals(ids))) {
			errorParserIds = ids;
			isDirty = true;
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#setSecondaryOutputs()
	 */
	public void setSecondaryOutputs(String newIds) {
		if (secondaryOutputIds == null && newIds == null) return;
		if (secondaryOutputIds == null || newIds == null || !newIds.equals(secondaryOutputIds)) {
			secondaryOutputIds = newIds;
			isDirty = true;					
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#setTargetToolIds()
	 */
	public void setTargetToolIds(String newIds) {
		if (targetToolIds == null && newIds == null) return;
		if (targetToolIds == null || newIds == null || !newIds.equals(targetToolIds)) {
			targetToolIds = newIds;
			isDirty = true;					
		}
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#setOSList(String[])
	 */
	public void setOSList(String[] OSs) {
		if (osList == null) {
			osList = new ArrayList();
		} else {
			osList.clear();
		}
		for (int i = 0; i < OSs.length; i++) {
			osList.add(OSs[i]);
		}		
		setDirty(true);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#setArchList(String[])
	 */
	public void setArchList(String[] archs) {
		if (archList == null) {
			archList = new ArrayList();
		} else {
			archList.clear();
		}
		for (int i = 0; i < archs.length; i++) {
			archList.add(archs[i]);
		}		
		setDirty(true);
	}

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IToolChain#getScannerConfigDiscoveryProfileId()
     */
    public String getScannerConfigDiscoveryProfileId() {
        if (scannerConfigDiscoveryProfileId == null) {
            if (getSuperClass() != null) {
                return getSuperClass().getScannerConfigDiscoveryProfileId();
            }
        }
        return scannerConfigDiscoveryProfileId;
    }

    /* (non-Javadoc)
     * @see org.eclipse.cdt.managedbuilder.core.IToolChain#setScannerConfigDiscoveryProfileId(java.lang.String)
     */
    public void setScannerConfigDiscoveryProfileId(String profileId) {
		if (scannerConfigDiscoveryProfileId == null && profileId == null) return;
        if (scannerConfigDiscoveryProfileId == null ||
                !scannerConfigDiscoveryProfileId.equals(profileId)) {
            scannerConfigDiscoveryProfileId = profileId;
            setDirty(true);
        }
    }

	/**
	 * @return the pathconverterElement
	 */
	public IConfigurationElement getPathconverterElement() {
		return pathconverterElement;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#getPathConverter()
	 */
	public IOptionPathConverter getOptionPathConverter() {
		if (optionPathConverter != null) {
			return optionPathConverter ;
		}
		IConfigurationElement element = getPathconverterElement();
		if (element != null) {
			try {
				if (element.getAttribute(ITool.OPTIONPATHCONVERTER) != null) {
					optionPathConverter = (IOptionPathConverter) element.createExecutableExtension(ITool.OPTIONPATHCONVERTER);
					return optionPathConverter;
				}
			} catch (CoreException e) {}
		}  else  {
			if (getSuperClass() instanceof IToolChain) {
				IToolChain superTool = (IToolChain) getSuperClass();
				return superTool.getOptionPathConverter() ;
			}
		}
		return null ;
	}

    
    
	/*
	 *  O B J E C T   S T A T E   M A I N T E N A N C E
	 */
	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#isExtensionElement()
	 */
	public boolean isExtensionElement() {
		return isExtensionToolChain;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#isDirty()
	 */
	public boolean isDirty() {
		// This shouldn't be called for an extension tool-chain
 		if (isExtensionToolChain) return false;
		
		// If I need saving, just say yes
		if (isDirty) return true;
		
		//check whether the tool-chain - specific macros are dirty
//		if(userDefinedMacros != null && userDefinedMacros.isDirty())
//			return true;
		
//		if(userDefinedEnvironment != null && userDefinedEnvironment.isDirty())
//			return true;

		if(builder != null && builder.isDirty())
			return true;

		// Otherwise see if any tools need saving
		Iterator iter = getToolList().listIterator();
		while (iter.hasNext()) {
			Tool toolChild = (Tool) iter.next();
			if (toolChild.isDirty()) return true;
		}
		
		// Otherwise see if any options need saving
		if (super.isDirty()) {
			return true;
		}
		
		return isDirty;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#setDirty(boolean)
	 */
	public void setDirty(boolean isDirty) {
		this.isDirty = isDirty;
		// Propagate "false" to options
		super.setDirty(isDirty);
		// Propagate "false" to the children
		if (!isDirty) {
			Iterator iter = getToolList().listIterator();
			while (iter.hasNext()) {
				Tool toolChild = (Tool) iter.next();
				toolChild.setDirty(false);
			}		    
		}
	}
	
	/* (non-Javadoc)
	 *  Resolve the element IDs to interface references
	 */
	public void resolveReferences() {
		if (!resolved) {
			resolved = true;
			// Resolve superClass
			if (superClassId != null && superClassId.length() > 0) {
				setSuperClassInternal(ManagedBuildManager.getExtensionToolChain(superClassId));
				if (getSuperClass() == null) {
					// Report error
					ManagedBuildManager.OutputResolveError(
							"superClass",	//$NON-NLS-1$
							superClassId,
							"toolChain",	//$NON-NLS-1$
							getId());
				} else {
					//  All of our superclasses must be resolved in order to properly
					//  resolve options to option categories   
					((ToolChain)getSuperClass()).resolveReferences();
				}
			}
			//  Resolve HoldsOptions 
			super.resolveReferences();
			//  Call resolveReferences on our children
			if (targetPlatform != null) {
				targetPlatform.resolveReferences();
			}
			if (builder != null) {
				builder.resolveReferences();
			}
			Iterator iter = getToolList().listIterator();
			while (iter.hasNext()) {
				Tool toolChild = (Tool) iter.next();
				toolChild.resolveReferences();
			}
		}
	}
	
	/* (non-Javadoc)
	 * Normalize the list of output extensions,for all tools in the toolchain by populating the list
	 * with an empty string for those tools which have no explicit output extension (as defined in the
	 * manifest file. In a post 2.1 manifest, all tools must have a specifed output extension, even
	 * if it is "")
	 */
	public void normalizeOutputExtensions(){
		ITool[] tools = getTools();
		if (tools != null) {
			for (int i = 0; i < tools.length; i++) {
				ITool tool = tools[i];
				String[] extensions = tool.getOutputsAttribute();
				if (extensions == null) {
					tool.setOutputsAttribute(""); //$NON-NLS-1$
					continue;
				}
				if (extensions.length == 0){ 
					tool.setOutputsAttribute(""); //$NON-NLS-1$
				    continue;
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#getConvertToId()
	 */
	public String getConvertToId() {
		if (convertToId == null) {
			// If I have a superClass, ask it
			if (getSuperClass() != null) {
				return getSuperClass().getConvertToId();
			} else {
				return EMPTY_STRING;
			}
		}
		return convertToId;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#setConvertToId(String)
	 */
	public void setConvertToId(String convertToId) {
		if (convertToId == null && this.convertToId == null) return;
		if (convertToId == null || this.convertToId == null || !convertToId.equals(this.convertToId)) {
			this.convertToId = convertToId;
			setDirty(true);
		}
		return;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#getVersionsSupported()
	 */
	public String getVersionsSupported() {
		if (versionsSupported == null) {
			// If I have a superClass, ask it
			if (getSuperClass() != null) {
				return getSuperClass().getVersionsSupported();
			} else {
				return EMPTY_STRING;
			}
		}
		return versionsSupported;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#setVersionsSupported(String)
	 */
	public void setVersionsSupported(String versionsSupported) {
		if (versionsSupported == null && this.versionsSupported == null) return;
		if (versionsSupported == null || this.versionsSupported == null || !versionsSupported.equals(this.versionsSupported)) {
			this.versionsSupported = versionsSupported;
			setDirty(true);
		}
		return;
	}

	private IConfigurationElement getIsToolChainSupportedElement(){
		if (managedIsToolChainSupportedElement == null) {
			if (superClass != null && superClass instanceof ToolChain) {
				return ((ToolChain)superClass).getIsToolChainSupportedElement();
			}
		}
		return managedIsToolChainSupportedElement;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#isSupported()
	 */
	public boolean isSupported(){
		if (managedIsToolChainSupported  == null) {
			IConfigurationElement element = getIsToolChainSupportedElement();
			if (element != null) {
				try {
					if (element.getAttribute(IS_TOOL_CHAIN_SUPPORTED) != null) {
						managedIsToolChainSupported = (IManagedIsToolChainSupported) element.createExecutableExtension(IS_TOOL_CHAIN_SUPPORTED);
					}
				} catch (CoreException e) {}
			}
		}

		if(managedIsToolChainSupported != null)
			return managedIsToolChainSupported.isSupported(this,null,null);
			return true;
	}

	/**
	 * Returns the plugin.xml element of the configurationEnvironmentSupplier extension or <code>null</code> if none. 
	 *  
	 * @return IConfigurationElement
	 */
	public IConfigurationElement getEnvironmentVariableSupplierElement(){
		if (environmentVariableSupplierElement == null) {
			if (getSuperClass() != null && getSuperClass() instanceof ToolChain) {
				return ((ToolChain)getSuperClass()).getEnvironmentVariableSupplierElement();
			}
		}
		return environmentVariableSupplierElement;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#getEnvironmentVariableSupplier()
	 */
	public IConfigurationEnvironmentVariableSupplier getEnvironmentVariableSupplier(){
		if (environmentVariableSupplier != null) {
			return environmentVariableSupplier;
		}
		IConfigurationElement element = getEnvironmentVariableSupplierElement();
		if (element != null) {
			try {
				if (element.getAttribute(CONFIGURATION_ENVIRONMENT_SUPPLIER) != null) {
					environmentVariableSupplier = (IConfigurationEnvironmentVariableSupplier) element.createExecutableExtension(CONFIGURATION_ENVIRONMENT_SUPPLIER);
					return environmentVariableSupplier;
				}
			} catch (CoreException e) {}
		}
		return null;
	}

	/*
	 * this method is called by the UserDefinedMacroSupplier to obtain user-defined
	 * macros available for this tool-chain
	 */
/*	public StorableMacros getUserDefinedMacros(){
		if(isExtensionToolChain)
			return null;
		
		if(userDefinedMacros == null)
			userDefinedMacros = new StorableMacros();
		return userDefinedMacros;
	}
*/	
//	public StorableEnvironment getUserDefinedEnvironment(){
//		if(isExtensionToolChain)
//			return null;
//		
//		return userDefinedEnvironment;
//	}
	
//	public void setUserDefinedEnvironment(StorableEnvironment env){
//		if(!isExtensionToolChain)
//			userDefinedEnvironment = env;
//	}
	
	
	/**
	 * Returns the plugin.xml element of the configurationMacroSupplier extension or <code>null</code> if none. 
	 *  
	 * @return IConfigurationElement
	 */
	public IConfigurationElement getBuildMacroSupplierElement(){
		if (buildMacroSupplierElement == null) {
			if (superClass != null && superClass instanceof ToolChain) {
				return ((ToolChain)superClass).getBuildMacroSupplierElement();
			}
		}
		return buildMacroSupplierElement;
	}

	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.core.IToolChain#getBuildMacroSupplier()
	 */
	public IConfigurationBuildMacroSupplier getBuildMacroSupplier(){
		if (buildMacroSupplier != null) {
			return buildMacroSupplier;
		}
		IConfigurationElement element = getBuildMacroSupplierElement();
		if (element != null) {
			try {
				if (element.getAttribute(CONFIGURATION_MACRO_SUPPLIER) != null) {
					buildMacroSupplier = (IConfigurationBuildMacroSupplier) element.createExecutableExtension(CONFIGURATION_MACRO_SUPPLIER);
					return buildMacroSupplier;
				}
			} catch (CoreException e) {}
		}
		return null;
	}
	
	/*
	 * This function checks for migration support for the toolchain, while
	 * loading. If migration support is needed, looks for the available
	 * converters and adds them to the list.
	 */

	public void checkForMigrationSupport() {

		boolean isExists = false;
	
		if (getSuperClass() == null) {
			// If 'getSuperClass()' is null, then there is no toolchain available in
			// plugin manifest file with the 'id' & version.
			// Look for the 'versionsSupported' attribute
			String high = (String) ManagedBuildManager
					.getExtensionToolChainMap().lastKey();
			
			SortedMap subMap = null;
			if (superClassId.compareTo(high) <= 0) {
				subMap = ManagedBuildManager.getExtensionToolChainMap().subMap(
						superClassId, high + "\0"); //$NON-NLS-1$
			} else {
				// It means there are no entries in the map for the given id.
				// make the project is invalid
				IConfiguration parentConfig = getParent();
				IManagedProject managedProject = parentConfig.getManagedProject();
				if (managedProject != null) {
					managedProject.setValid(false);
				}
				return;
			}

			// for each element in the 'subMap',
			// check the 'versionsSupported' attribute whether the given
			// toolChain version is supported

			String baseId = ManagedBuildManager.getIdFromIdAndVersion(superClassId);
			String version = getVersionFromId().toString();

			Collection c = subMap.values();
			IToolChain[] toolChainElements = (IToolChain[]) c.toArray(new IToolChain[c.size()]);
			
			for (int i = 0; i < toolChainElements.length; i++) {
				IToolChain toolChainElement = toolChainElements[i];

				if (ManagedBuildManager.getIdFromIdAndVersion(
						toolChainElement.getId()).compareTo(baseId) > 0)
					break;
				
				// First check if both base ids are equal
				if (ManagedBuildManager.getIdFromIdAndVersion(
						toolChainElement.getId()).equals(baseId)) {

					// Check if 'versionsSupported' attribute is available'
					String versionsSupported = toolChainElement.getVersionsSupported();

					if ((versionsSupported != null)
							&& (!versionsSupported.equals(""))) { //$NON-NLS-1$
						String[] tmpVersions = versionsSupported.split(","); //$NON-NLS-1$

						for (int j = 0; j < tmpVersions.length; j++) {
							if (new PluginVersionIdentifier(version).equals(new PluginVersionIdentifier(tmpVersions[j]))) {
								// version is supported.
								// Do the automatic conversion without
								// prompting the user.
								// Get the supported version
								String supportedVersion = ManagedBuildManager.getVersionFromIdAndVersion(
										toolChainElement.getId());
								setId(ManagedBuildManager.getIdFromIdAndVersion(getId())
										+ "_" + supportedVersion); //$NON-NLS-1$
								
								// If control comes here means that 'superClass' is null
								// So, set the superClass to this toolChain element
								setSuperClassInternal(toolChainElement);
								superClassId = getSuperClass().getId();
								isExists = true;
								break;
							}
						}
						if(isExists)
							break;        // break the outer for loop if 'isExists' is true
					}					
				}
			}
		}
		
		if (getSuperClass() != null) {
			// If 'getSuperClass()' is not null, look for 'convertToId' attribute in plugin
			// manifest file for this toolchain.
			String convertToId = getSuperClass().getConvertToId();
			if ((convertToId == null) || (convertToId.equals(""))) { //$NON-NLS-1$
				// It means there is no 'convertToId' attribute available and
				// the version is still actively
				// supported by the tool integrator. So do nothing, just return
				return;
			} else {
				// In case the 'convertToId' attribute is available,
				// it means that Tool integrator currently does not support this
				// version of toolchain.
				// Look for the converters available for this toolchain version.

				getConverter(convertToId);
			}

		} else {
			// make the project is invalid
			// 
			IConfiguration parentConfig = getParent();
			IManagedProject managedProject = parentConfig.getManagedProject();
			if (managedProject != null) {
				managedProject.setValid(false);
			}
		}
		return;
	}

	private void getConverter(String convertToId) {

		String fromId = null;
		String toId = null;

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

					if (element.getName().equals("converter")) { //$NON-NLS-1$

						fromId = element.getAttribute("fromId"); //$NON-NLS-1$
						toId = element.getAttribute("toId"); //$NON-NLS-1$
						// Check whether the current converter can be used for
						// the selected toolchain

						if (fromId.equals(getSuperClass().getId())
								&& toId.equals(convertToId)) {
							// If it matches
							String mbsVersion = element
									.getAttribute("mbsVersion"); //$NON-NLS-1$
							PluginVersionIdentifier currentMbsVersion = ManagedBuildManager
									.getBuildInfoVersion();

							// set the converter element based on the MbsVersion
							if (currentMbsVersion
									.isGreaterThan(new PluginVersionIdentifier(
											mbsVersion))) {
								previousMbsVersionConversionElement = element;
							} else {
								currentMbsVersionConversionElement = element;
							}
							return;
						}
					}
				}
			}
		}

		// If control comes here, it means 'Tool Integrator' specified
		// 'convertToId' attribute in toolchain definition file, but
		// has not provided any converter.
		// So, make the project is invalid

		IConfiguration parentConfig = getParent();
		IManagedProject managedProject = parentConfig.getManagedProject();
		if (managedProject != null) {
			managedProject.setValid(false);
		}
	}


	public IConfigurationElement getPreviousMbsVersionConversionElement() {
		return previousMbsVersionConversionElement;
	}

	public IConfigurationElement getCurrentMbsVersionConversionElement() {
		return currentMbsVersionConversionElement;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.internal.core.BuildObject#updateManagedBuildRevision(java.lang.String)
	 */
	public void updateManagedBuildRevision(String revision){
		super.updateManagedBuildRevision(revision);
		
		for(Iterator iter = getToolList().iterator(); iter.hasNext();){
			((Tool)iter.next()).updateManagedBuildRevision(revision);
		}
		
		if(builder != null)
			builder.updateManagedBuildRevision(revision);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.internal.core.HoldsOptions#needsRebuild()
	 */
	public boolean needsRebuild() {
		if(rebuildState)
			return true;
		
		ITool tools[] = getTools();
		for(int i = 0; i < tools.length; i++){
			if(tools[i].needsRebuild())
				return true;
		}

		return super.needsRebuild();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.managedbuilder.internal.core.HoldsOptions#setRebuildState(boolean)
	 */
	public void setRebuildState(boolean rebuild) {
		if(isExtensionElement() && rebuild)
			return;

		if(rebuildState != rebuild){
			rebuildState = rebuild;
			saveRebuildState();
		}
		
		if(!rebuild){
			super.setRebuildState(false);

			ITool tools[] = getTools();
			for(int i = 0; i < tools.length; i++){
				tools[i].setRebuildState(false);
			}
		}
	}
	
	private void saveRebuildState(){
		if(((Configuration)config).isPreference())
			return;
		PropertyManager.getInstance().setProperty(this, REBUILD_STATE, Boolean.toString(needsRebuild()));
	}

	public IFolderInfo getParentFolderInfo() {
		return parentFolderInfo;
	}
	
	void setTargetPlatform(TargetPlatform tp){
		targetPlatform = tp;
	}

	public CTargetPlatformData getTargetPlatformData() {
		if(isExtensionToolChain)
			return null;
		if(targetPlatform == null){
			ITargetPlatform platform = getTargetPlatform();
			if(platform != null){
				ITargetPlatform extPlatform = platform;
				for(;extPlatform != null && !extPlatform.isExtensionElement();
					extPlatform = extPlatform.getSuperClass());
				String subId;
				if(extPlatform != null)
					subId = ManagedBuildManager.calculateChildId(extPlatform.getId(), null);
				else
					subId = ManagedBuildManager.calculateChildId(getId(), null);
					
				targetPlatform = new TargetPlatform(this, subId, platform.getName(), (TargetPlatform)extPlatform);
			} else {
				String subId = ManagedBuildManager.calculateChildId(getId(), null);
				targetPlatform = new TargetPlatform(this, null, subId, "", false);
			}
		}
			
		return targetPlatform.getTargetPlatformData();
	}

	public boolean supportsType(String type, boolean checkTools) {
		SupportedProperties props = findSupportedProperties();
		boolean supports = false;
		if(props != null){
			supports = props.supportsType(type);
		} else {
			BooleanExpressionApplicabilityCalculator calc = getBooleanExpressionCalculator();
			if(calc != null){
				supports = calc.referesProperty(type);
			}
			
			if(!supports)
				supports = super.supportsType(type);
		}
		if(!supports && checkTools){
			ITool tools[] = getTools();
			for(int i = 0; i < tools.length; i++){
				if(((Tool)tools[i]).supportsType(type)){
					supports = true;
					break;
				}
			}
		}
		return supports;
	}

	public boolean supportsType(String type) {
		return supportsType(type, true);
	}

	public boolean supportsType(IBuildPropertyType type) {
		return supportsType(type.getId());
	}

	public boolean supportsValue(String type, String value){
		return supportsValue(type, value, true);
	}

	public boolean supportsValue(String type, String value, boolean checkTools){
		SupportedProperties props = findSupportedProperties();
		boolean supports = false;
		if(props != null){
			supports = props.supportsValue(type, value);
		} else {
			BooleanExpressionApplicabilityCalculator calc = getBooleanExpressionCalculator();
			if(calc != null){
				supports = calc.referesPropertyValue(type, value);
			}
	
			if(!supports)
				supports = super.supportsValue(type, value);
		}

		if(!supports && checkTools){
			ITool tools[] = getTools();
			for(int i = 0; i < tools.length; i++){
				if(((Tool)tools[i]).supportsValue(type, value)){
					supports = true;
					break;
				}
			}
		}
		return supports;
	}

	public boolean supportsValue(IBuildPropertyType type,
			IBuildPropertyValue value) {
		return supportsValue(type.getId(), value.getId());
	}
	
	public void propertiesChanged() {
		if(isExtensionToolChain)
			return;
		
		BooleanExpressionApplicabilityCalculator calculator = getBooleanExpressionCalculator();
		if(calculator != null)
			calculator.adjustToolChain(getParentFolderInfo(), this, false);
		
		super.propertiesChanged();
	}
	
	public BooleanExpressionApplicabilityCalculator getBooleanExpressionCalculator(){
		if(booleanExpressionCalculator == null){
			if(superClass != null){
				return ((ToolChain)superClass).getBooleanExpressionCalculator();
			}
		}
		return booleanExpressionCalculator;
	}

	protected IResourceInfo getParentResourceInfo() {
		return getParentFolderInfo();
	}

	public boolean matches(IToolChain tc){
		if(tc == this)
			return true;
		
		IToolChain rTc = ManagedBuildManager.getRealToolChain(this);
		if(rTc == null)
			return false;
		
		return rTc == ManagedBuildManager.getRealToolChain(tc);
	}
	
	private boolean performMatchCompatison(IToolChain tCh){
		if(tCh == null)
			return false;
		
		if(tCh == this)
			return true;
		
//		if(tCh.isReal() && isReal())
//			return false;
		
		String name = tCh.getName();
		if(name == null){
			if(getName() != null)
				return false;
		} else if(!name.equals(getName())){
			return false;
		}
		
		String thisVersion = ManagedBuildManager.getVersionFromIdAndVersion(getId());
		String otherVersion = ManagedBuildManager.getVersionFromIdAndVersion(tCh.getId());
		if(thisVersion == null || thisVersion.length() == 0){
			if(otherVersion != null && otherVersion.length() != 0)
				return false;
		} else {
			if(!thisVersion.equals(otherVersion))
				return false;
		}
			
		return true;
		
//		if(true)
//			return true;
//		
//		ITool tools[] = getTools();
//		ITool otherTools[] = tCh.getTools();
//		
//		if(tools.length != otherTools.length)
//			return false;
//
//		if(!ListComparator.match(tools,
//				otherTools,
//				new Comparator(){
//			public boolean equal(Object o1, Object o2){
//				return ((Tool)o1).performMatchComparison((Tool)o2);
//			}
//		}))
//			return false; 
//		
//		return true;
	}
	
	public List getIdenticalList(){
		return identicalList;//;(ArrayList)identicalToolChainsList.clone();
	}
	
	public boolean supportsBuild(boolean managed) {
		if(!getSupportsManagedBuildAttribute())
			return !managed;
		
		IBuilder builder = getBuilder();
		if(builder != null && !builder.supportsBuild(managed))
			return false;
		
		ITool tools[] = getTools();
		for(int i = 0; i < tools.length; i++){
			if(!tools[i].supportsBuild(managed))
				return false;
		}
		
		return true;
	}
	
	public boolean getSupportsManagedBuildAttribute(){
		if(supportsManagedBuild == null){
			if(superClass != null){
				return ((ToolChain)superClass).getSupportsManagedBuildAttribute();
			}
			return true;
		}
		return supportsManagedBuild.booleanValue();
	}

	public boolean isSystemObject() {
		if(isTest)
			return true;
		
		if(getParent() != null)
			return getParent().isSystemObject();
		
		return false;
	}
	
	private class MatchKey {
		ToolChain toolChain;
		
		public MatchKey(ToolChain tch) {
			toolChain = tch;
		}
		
		public boolean equals(Object obj) {
			if(obj == this)
				return true;
			if(!(obj instanceof MatchKey))
				return false;
			MatchKey other = (MatchKey)obj;
			return toolChain.performMatchCompatison(other.toolChain);
		}

		public int hashCode() {
			String name = getName();
			if(name == null)
				name = getId();
			int code = name.hashCode();
			String version = ManagedBuildManager.getVersionFromIdAndVersion(getId());
			if(version != null)
				code += version.hashCode();
			return code;
		}
		
	}

	public Object getMatchKey() {
		if(isAbstract())
			return null;
		if(!isExtensionToolChain)
			return null;
		return new MatchKey(this);
	}

	public void setIdenticalList(List list) {
		identicalList = list;
	}
	
	public String getNameAndVersion(){
		String name = getName();
		String version = ManagedBuildManager.getVersionFromIdAndVersion(getId());
		if(version != null && version.length() != 0){
			return new StringBuffer().append(name).append(" (").append(version).append("").toString();
		}
		return name;
	}
	
	public IConfigurationElement getConverterModificationElement(IToolChain tc){
		Map map = ManagedBuildManager.getConversionElements(this);
		IConfigurationElement element = null;
		if(!map.isEmpty()){
			for(Iterator iter = map.values().iterator(); iter.hasNext();){
				IConfigurationElement el = (IConfigurationElement)iter.next();
				String toId = el.getAttribute("toId"); //$NON-NLS-1$
				IToolChain toTc = tc;
				if(toId != null){
					for(;toTc != null; toTc = toTc.getSuperClass()){
						if(toId.equals(toTc.getId()))
							break;
					}
				}
				
				if(toTc != null){
					element = el;
					break; 
				}
			}
		}

		return element;
	}

	public IConfigurationElement getConverterModificationElement(ITool fromTool, ITool toTool){
		return ((Tool)fromTool).getConverterModificationElement(toTool);
	}

	void updateParentFolderInfo(FolderInfo info){
		parentFolderInfo = info;
		config = parentFolderInfo.getParent();
	}

	public String[] getRequiredTypeIds() {
		return getRequiredTypeIds(true);
	}

	public String[] getRequiredTypeIds(boolean checkTools) {
		SupportedProperties props = findSupportedProperties();
		List result = new ArrayList();
		if(props != null) {
			result.addAll(Arrays.asList(props.getRequiredTypeIds()));
		} else {
//			BooleanExpressionApplicabilityCalculator calc = getBooleanExpressionCalculator();
//			if(calc != null){
//				result.addAll(Arrays.asList(calc.getReferencedPropertyIds()));
//			}
			
			result.addAll(Arrays.asList(super.getRequiredTypeIds()));
		}
		
		//call tools anyway
		if(checkTools){
			ITool tools[] = getTools();
			for(int i = 0; i < tools.length; i++){
				result.addAll(Arrays.asList(((Tool)tools[i]).getRequiredTypeIds()));
			}
		}
		return (String[])result.toArray(new String[result.size()]);
	}

	public String[] getSupportedTypeIds() {
		return getSupportedTypeIds(true);
	}

	public String[] getSupportedTypeIds(boolean checkTools) {
		SupportedProperties props = findSupportedProperties();
		List result = new ArrayList();
		if(props != null) {
			result.addAll(Arrays.asList(props.getSupportedTypeIds()));
		} else {
			BooleanExpressionApplicabilityCalculator calc = getBooleanExpressionCalculator();
			if(calc != null){
				result.addAll(Arrays.asList(calc.getReferencedPropertyIds()));
			}
			
			result.addAll(Arrays.asList(super.getSupportedTypeIds()));
		}
		
		//call tools anyway
		if(checkTools){
			ITool tools[] = getTools();
			for(int i = 0; i < tools.length; i++){
				result.addAll(Arrays.asList(((Tool)tools[i]).getSupportedTypeIds()));
			}
		}
		return (String[])result.toArray(new String[result.size()]);
	}

	public String[] getSupportedValueIds(String typeId) {
		return getSupportedValueIds(typeId, true);
	}

	public String[] getSupportedValueIds(String typeId, boolean checkTools) {
		SupportedProperties props = findSupportedProperties();
		List result = new ArrayList();
		if(props != null) {
			result.addAll(Arrays.asList(props.getSupportedValueIds(typeId)));
		} else {
			BooleanExpressionApplicabilityCalculator calc = getBooleanExpressionCalculator();
			if(calc != null){
				result.addAll(Arrays.asList(calc.getReferencedValueIds(typeId)));
			}
			
			result.addAll(Arrays.asList(super.getSupportedValueIds(typeId)));
		}
		
		//call tools anyway
		if(checkTools){
			ITool tools[] = getTools();
			for(int i = 0; i < tools.length; i++){
				result.addAll(Arrays.asList(((Tool)tools[i]).getSupportedValueIds(typeId)));
			}
		}
		return (String[])result.toArray(new String[result.size()]);
	}

	public boolean requiresType(String typeId) {
		return requiresType(typeId, true);
	}

	public boolean requiresType(String typeId, boolean checkTools) {
		SupportedProperties props = findSupportedProperties();
		boolean required = false;
		if(props != null) {
			required = props.requiresType(typeId);
		} else {
			required = super.requiresType(typeId);
		}
		
		//call tools if not found
		if(!required && checkTools){
			ITool tools[] = getTools();
			for(int i = 0; i < tools.length; i++){
				if(((Tool)tools[i]).requiresType(typeId)){
					required = true;
					break;
				}
			}
		}
		return required;
	}
	
	private SupportedProperties findSupportedProperties(){
		if(supportedProperties == null){
			if(superClass != null){
				return ((ToolChain)superClass).findSupportedProperties();
			}
		}
		return supportedProperties;
	}

	private void loadProperties(IManagedConfigElement el){
		supportedProperties = new SupportedProperties(el);
	}
	
	void setNonInternalBuilderId(String id){
		nonInternalBuilderId = id;
	}
	
	String getNonInternalBuilderId(){
		if(nonInternalBuilderId == null){
			if(superClass != null){
				return ((ToolChain)superClass).getNonInternalBuilderId();
			}
			return null;
		}
		return nonInternalBuilderId;
	}
	
	public void resetErrorParsers(FolderInfo info){
		errorParserIds = null;
		info.resetErrorParsers(info.getFilteredTools());
		
		if(info.isRoot()){
			if(builder != null){
				builder.resetErrorParsers();
			}
		}
	}

	void removeErrorParsers(FolderInfo info, Set set){
		Set oldSet = contributeErrorParsers(info, null, false);
		oldSet.removeAll(set);
		setErrorParserList((String[])oldSet.toArray(new String[oldSet.size()]));
		
		info.removeErrorParsers(info.getFilteredTools(), set);


		if(info.isRoot()){
			Builder builder = (Builder)info.getParent().getEditableBuilder();
			builder.removeErrorParsers(set);
		}
	}
	
	public void setErrorParserList(String[] ids) {
		if(ids == null){
			errorParserIds = null;
		} else if(ids.length == 0){
			errorParserIds = EMPTY_STRING;
		} else {
			StringBuffer buf = new StringBuffer();
			buf.append(ids[0]);
			for(int i = 1; i < ids.length; i++){
				buf.append(";").append(ids[i]);
			}
			errorParserIds = buf.toString();
		}
	}
	
	public String getUniqueRealName() {
		String name = getName();
		if(name == null){
			name = getId();
		} else {
			String version = ManagedBuildManager.getVersionFromIdAndVersion(getId());
			if(version != null){
			StringBuffer buf = new StringBuffer();
			buf.append(name);
			buf.append(" (v").append(version).append(")");
			name = buf.toString();
			}
		}
		return name;
	}
	
	void resolveProjectReferences(boolean onLoad){
		for(Iterator iter = getToolList().iterator(); iter.hasNext();){
			Tool tool = (Tool)iter.next();
			tool.resolveProjectReferences(onLoad);
		}
	}

	public boolean hasScannerConfigSettings(){
		
		if(getScannerConfigDiscoveryProfileId() != null)
			return true;
		
		return false;
	}
	
	public boolean isPerRcTypeDiscovery(){
		if(isRcTypeBasedDiscovery == null){
			if(superClass != null){
				return ((ToolChain)superClass).isPerRcTypeDiscovery();
			}
			return true;
		}
		return isRcTypeBasedDiscovery.booleanValue();
	}

	public void setPerRcTypeDiscovery(boolean on){
		isRcTypeBasedDiscovery = Boolean.valueOf(on);
	}

	public IDiscoveredPathManager.IDiscoveredPathInfo setDiscoveredPathInfo(IDiscoveredPathManager.IDiscoveredPathInfo info){
		IDiscoveredPathManager.IDiscoveredPathInfo oldInfo = discoveredInfo;
		discoveredInfo = info;
		return oldInfo;
	}

	public IDiscoveredPathManager.IDiscoveredPathInfo getDiscoveredPathInfo(){
		return discoveredInfo;
	}
	
	public IDiscoveredPathManager.IDiscoveredPathInfo clearDiscoveredPathInfo(){
		IDiscoveredPathManager.IDiscoveredPathInfo oldInfo = discoveredInfo;
		discoveredInfo = null;
		return oldInfo;
	}
	
	public boolean isPreferenceToolChain(){
		IToolChain tch = ManagedBuildManager.getRealToolChain(this);
		return tch != null && tch.getId().equals(ConfigurationDataProvider.PREF_TC_ID);
	}
}
