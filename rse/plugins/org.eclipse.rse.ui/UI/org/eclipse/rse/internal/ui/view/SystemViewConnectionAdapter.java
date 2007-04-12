/********************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation and others. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * The following IBM employees contributed to the Remote System Explorer
 * component that contains this file: David McKnight, Kushal Munir, 
 * Michael Berger, David Dykstal, Phil Coulthard, Don Yantzi, Eric Simpson, 
 * Emily Bruner, Mazen Faraj, Adrian Storisteanu, Li Ding, and Kent Hawley.
 * 
 * Contributors:
 * David Dykstal (IBM) - moved SystemPreferencesManager to a new package
 *                     - created and used RSEPreferencesManager
 * Uwe Stieber (Wind River) - Menu action contributions can be acknowlegded by system type provider
 * David Dykstal (IBM) - 180562: remove implementation of IRSEUserIdConstants
 * Martin Oberhuber (Wind River) - [175262] IHost.getSystemType() should return IRSESystemType 
 ********************************************************************************/

package org.eclipse.rse.internal.ui.view;
import java.util.Vector;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.action.GroupMarker;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.rse.core.IRSESystemType;
import org.eclipse.rse.core.IRSEUserIdConstants;
import org.eclipse.rse.core.RSECorePlugin;
import org.eclipse.rse.core.RSEPreferencesManager;
import org.eclipse.rse.core.model.IHost;
import org.eclipse.rse.core.model.ISystemRegistry;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.internal.ui.SystemResources;
import org.eclipse.rse.internal.ui.actions.SystemClearAllPasswordsAction;
import org.eclipse.rse.internal.ui.actions.SystemCommonDeleteAction;
import org.eclipse.rse.internal.ui.actions.SystemCommonRenameAction;
import org.eclipse.rse.internal.ui.actions.SystemConnectAllSubSystemsAction;
import org.eclipse.rse.internal.ui.actions.SystemCopyConnectionAction;
import org.eclipse.rse.internal.ui.actions.SystemDisconnectAllSubSystemsAction;
import org.eclipse.rse.internal.ui.actions.SystemMoveConnectionAction;
import org.eclipse.rse.internal.ui.actions.SystemMoveDownConnectionAction;
import org.eclipse.rse.internal.ui.actions.SystemMoveUpConnectionAction;
import org.eclipse.rse.internal.ui.actions.SystemOpenExplorerPerspectiveAction;
import org.eclipse.rse.internal.ui.actions.SystemShowInMonitorAction;
import org.eclipse.rse.internal.ui.actions.SystemShowInTableAction;
import org.eclipse.rse.internal.ui.actions.SystemWorkOfflineAction;
import org.eclipse.rse.model.ISystemRegistryUI;
import org.eclipse.rse.ui.ISystemContextMenuConstants;
import org.eclipse.rse.ui.ISystemMessages;
import org.eclipse.rse.ui.RSESystemTypeAdapter;
import org.eclipse.rse.ui.RSEUIPlugin;
import org.eclipse.rse.ui.SystemMenuManager;
import org.eclipse.rse.ui.actions.SystemNewConnectionAction;
import org.eclipse.rse.ui.actions.SystemRefreshAction;
import org.eclipse.rse.ui.validators.ISystemValidator;
import org.eclipse.rse.ui.validators.ValidatorSpecialChar;
import org.eclipse.rse.ui.view.AbstractSystemViewAdapter;
import org.eclipse.rse.ui.view.ISystemPropertyConstants;
import org.eclipse.rse.ui.view.ISystemViewInputProvider;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.dialogs.PropertyDialogAction;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;


/**
 * Adapter for displaying SystemConnection objects in tree views.
 */
public class SystemViewConnectionAdapter 
       extends AbstractSystemViewAdapter
{
	private SystemNewConnectionAction anotherConnectionAction = null;
	//private SystemUpdateConnectionAction updateAction = null;
	private SystemMoveUpConnectionAction upAction = null;
	private SystemMoveDownConnectionAction downAction = null;
	private SystemDisconnectAllSubSystemsAction disconnectAction = null;
	private SystemConnectAllSubSystemsAction connectAction = null;
	private SystemClearAllPasswordsAction clearPasswordAction = null;
	private SystemCopyConnectionAction copyAction = null;
	private SystemMoveConnectionAction moveAction = null;
	
	// yantzi: artemis 6.0, add work offline support
	private SystemWorkOfflineAction offlineAction = null;
	
	private SystemInheritablePropertyData userIdData = new SystemInheritablePropertyData();	
	private String translatedType = null;
	private String translatedHostname = null;
	private String translatedDescription = null;
    // for reset property support
    private String  original_hostName, original_description;
	private SystemInheritablePropertyData original_userIdData = new SystemInheritablePropertyData();    
	private boolean changed_hostName, changed_description, changed_userId;
	private boolean actionsCreated = false;
	
	// -------------------
	// property descriptors
	// -------------------
	private static PropertyDescriptor[] propertyDescriptorArray = null;
	
	/**
	 * Returns the system type object for the specified host.
	 * 
	 * @param host The host to get the system type object from.
	 * @return The system type object or <code>null</code>.
	 */
	private IRSESystemType getSystemTypeForHost(IHost host) {
		if (host != null) {
			return host.getSystemType();
		}
		return null;
	}
	/**
	 * Returns any actions that should be contributed to the popup menu
	 * for the given element.
	 * @param menu The menu to contribute actions to
	 * @param selection The window's current selection.
	 * @param shell Shell of viewer
	 * @param menuGroup recommended menu group to add actions to. If added to another group, you must be sure to create that group first.
	 */
	public void addActions(SystemMenuManager menu, IStructuredSelection selection, Shell shell, String menuGroup) {
		if (!actionsCreated) createActions();

		// bugzilla#161195: _ALL_ actions needs to be passed to the system type for approval.
		//                  _Never_ add any action without the system type provider having said ok to this.
		IHost host = (IHost)selection.getFirstElement();
		IRSESystemType sysType = getSystemTypeForHost(host);
		Object adapter = sysType != null ? sysType.getAdapter(IRSESystemType.class) : null;
		RSESystemTypeAdapter sysTypeAdapter = adapter instanceof RSESystemTypeAdapter ? (RSESystemTypeAdapter)adapter : null;
		
		//updateAction.setValue(null); // reset
		if (sysTypeAdapter == null
				|| sysTypeAdapter.acceptContextMenuActionContribution(host, anotherConnectionAction.getClass())) {
			menu.add(menuGroup, anotherConnectionAction);
			menu.appendToGroup(ISystemContextMenuConstants.GROUP_NEW, new GroupMarker(ISystemContextMenuConstants.GROUP_NEW_NONCASCADING));// user or BP/ISV additions
		}

		if (sysTypeAdapter == null
				|| sysTypeAdapter.acceptContextMenuActionContribution(host, copyAction.getClass()))
			menu.add(menuGroup, copyAction);
		
		if (sysTypeAdapter == null
				|| sysTypeAdapter.acceptContextMenuActionContribution(host, moveAction.getClass()))
			menu.add(menuGroup, moveAction);
		if (sysTypeAdapter == null
				|| sysTypeAdapter.acceptContextMenuActionContribution(host, upAction.getClass()))
			menu.add(menuGroup, upAction);
		if (sysTypeAdapter == null
				|| sysTypeAdapter.acceptContextMenuActionContribution(host, downAction.getClass()))
			menu.add(menuGroup, downAction);

		// MJB: RE defect 40854
		addConnectOrDisconnectAction(menu, menuGroup, selection);

		if (sysTypeAdapter == null
				|| sysTypeAdapter.acceptContextMenuActionContribution(host, clearPasswordAction.getClass()))
			menu.add(menuGroup, clearPasswordAction);

		// yantzi: artemis 6.0, offline support, only add work offline action for system types that support offline mode
		if (sysTypeAdapter == null
				|| sysTypeAdapter.acceptContextMenuActionContribution(host, offlineAction.getClass()))
			menu.add(menuGroup, offlineAction);
	}
	
	private void addConnectOrDisconnectAction(SystemMenuManager menu, String menuGroup, IStructuredSelection selection) {
		IHost host = (IHost)selection.getFirstElement();
		IRSESystemType sysType = getSystemTypeForHost(host);
		Object adapter = sysType != null ? sysType.getAdapter(IRSESystemType.class) : null;
		RSESystemTypeAdapter sysTypeAdapter = adapter instanceof RSESystemTypeAdapter ? (RSESystemTypeAdapter)adapter : null;
		
		ISystemRegistry sysReg = RSEUIPlugin.getTheSystemRegistry();
		boolean anySupportsConnect = sysReg.isAnySubSystemSupportsConnect(host);

		if (anySupportsConnect) {
			boolean anyConnected = sysReg.isAnySubSystemConnected(host);
			boolean allConnected = sysReg.areAllSubSystemsConnected(host);
			
			if (!allConnected && (sysTypeAdapter == null
														|| sysTypeAdapter.acceptContextMenuActionContribution(host, connectAction.getClass())))
				menu.add(menuGroup, connectAction);

			if (anyConnected && (sysTypeAdapter == null
														|| sysTypeAdapter.acceptContextMenuActionContribution(host, disconnectAction.getClass())))
				menu.add(menuGroup, disconnectAction);
		}
	}
	
	private void createActions()
	{
	    anotherConnectionAction = new SystemNewConnectionAction(null,
	                                                            SystemResources.ACTION_ANOTHERCONN_LABEL,
	                                                            SystemResources.ACTION_ANOTHERCONN_TOOLTIP,
	                                                            false,
	                                                            true,
	                                                            null);
			anotherConnectionAction.setHelp(RSEUIPlugin.HELPPREFIX+"actn0015"); //$NON-NLS-1$

	    //updateAction = new SystemUpdateConnectionAction(null);
	    upAction = new SystemMoveUpConnectionAction(null);	
	    downAction = new SystemMoveDownConnectionAction(null);	
	    disconnectAction = new SystemDisconnectAllSubSystemsAction(null);
	    copyAction = new SystemCopyConnectionAction(null);	
	    moveAction = new SystemMoveConnectionAction(null);
	    offlineAction = new SystemWorkOfflineAction(null);
	    connectAction = new SystemConnectAllSubSystemsAction(null);
	    clearPasswordAction = new SystemClearAllPasswordsAction(null);
	    	
		actionsCreated = true;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.ui.view.AbstractSystemViewAdapter#showDelete(java.lang.Object)
	 */
	public boolean showDelete(Object element) {
		// bugzilla#161195: _ALL_ actions needs to be passed to the system type for approval.
		//                  _Never_ add any action without the system type provider having said ok to this.
		if (element instanceof IHost) {
			IRSESystemType sysType = getSystemTypeForHost((IHost)element);
			Object adapter = sysType != null ? sysType.getAdapter(IRSESystemType.class) : null;
			RSESystemTypeAdapter sysTypeAdapter = adapter instanceof RSESystemTypeAdapter ? (RSESystemTypeAdapter)adapter : null;
			if (sysTypeAdapter != null) {
				return sysTypeAdapter.acceptContextMenuActionContribution((IHost)element, SystemCommonDeleteAction.class);
			}
		}
		return super.showDelete(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.ui.view.AbstractSystemViewAdapter#showGenericShowInTableAction(java.lang.Object)
	 */
	public boolean showGenericShowInTableAction(Object element) {
		// bugzilla#161195: _ALL_ actions needs to be passed to the system type for approval.
		//                  _Never_ add any action without the system type provider having said ok to this.
		if (element instanceof IHost) {
			IRSESystemType sysType = getSystemTypeForHost((IHost)element);
			Object adapter = sysType != null ? sysType.getAdapter(IRSESystemType.class) : null;
			RSESystemTypeAdapter sysTypeAdapter = adapter instanceof RSESystemTypeAdapter ? (RSESystemTypeAdapter)adapter : null;
			if (sysTypeAdapter != null) {
				boolean accepted = false;
				Class[] affectedClasses = new Class[] { SystemShowInTableAction.class, SystemShowInMonitorAction.class };
				for (int i = 0; i < affectedClasses.length && accepted == false; i++) {
					accepted |= sysTypeAdapter.acceptContextMenuActionContribution((IHost)element, affectedClasses[i]);
				}
				return accepted;
			}
		}
		return super.showGenericShowInTableAction(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.ui.view.AbstractSystemViewAdapter#showOpenViewActions(java.lang.Object)
	 */
	public boolean showOpenViewActions(Object element) {
		// bugzilla#161195: _ALL_ actions needs to be passed to the system type for approval.
		//                  _Never_ add any action without the system type provider having said ok to this.
		if (element instanceof IHost) {
			IRSESystemType sysType = getSystemTypeForHost((IHost)element);
			Object adapter = sysType != null ? sysType.getAdapter(IRSESystemType.class) : null;
			RSESystemTypeAdapter sysTypeAdapter = adapter instanceof RSESystemTypeAdapter ? (RSESystemTypeAdapter)adapter : null;
			if (sysTypeAdapter != null) {
				return sysTypeAdapter.acceptContextMenuActionContribution((IHost)element, SystemOpenExplorerPerspectiveAction.class);
			}
		}
		return super.showOpenViewActions(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.ui.view.AbstractSystemViewAdapter#showProperties(java.lang.Object)
	 */
	public boolean showProperties(Object element) {
		// bugzilla#161195: _ALL_ actions needs to be passed to the system type for approval.
		//                  _Never_ add any action without the system type provider having said ok to this.
		if (element instanceof IHost) {
			IRSESystemType sysType = getSystemTypeForHost((IHost)element);
			Object adapter = sysType != null ? sysType.getAdapter(IRSESystemType.class) : null;
			RSESystemTypeAdapter sysTypeAdapter = adapter instanceof RSESystemTypeAdapter ? (RSESystemTypeAdapter)adapter : null;
			if (sysTypeAdapter != null) {
				return sysTypeAdapter.acceptContextMenuActionContribution((IHost)element, PropertyDialogAction.class);
			}
		}
		return super.showProperties(element);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.rse.ui.view.AbstractSystemViewAdapter#showRefresh(java.lang.Object)
	 */
	public boolean showRefresh(Object element) {
		// bugzilla#161195: _ALL_ actions needs to be passed to the system type for approval.
		//                  _Never_ add any action without the system type provider having said ok to this.
		if (element instanceof IHost) {
			IRSESystemType sysType = getSystemTypeForHost((IHost)element);
			Object adapter = sysType != null ? sysType.getAdapter(IRSESystemType.class) : null;
			RSESystemTypeAdapter sysTypeAdapter = adapter instanceof RSESystemTypeAdapter ? (RSESystemTypeAdapter)adapter : null;
			if (sysTypeAdapter != null) {
				return sysTypeAdapter.acceptContextMenuActionContribution((IHost)element, SystemRefreshAction.class);
			}
		}
		return super.showRefresh(element);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.ui.view.AbstractSystemViewAdapter#showRename(java.lang.Object)
	 */
	public boolean showRename(Object element) {
		// bugzilla#161195: _ALL_ actions needs to be passed to the system type for approval.
		//                  _Never_ add any action without the system type provider having said ok to this.
		if (element instanceof IHost) {
			IRSESystemType sysType = getSystemTypeForHost((IHost)element);
			Object adapter = sysType != null ? sysType.getAdapter(IRSESystemType.class) : null;
			RSESystemTypeAdapter sysTypeAdapter = adapter instanceof RSESystemTypeAdapter ? (RSESystemTypeAdapter)adapter : null;
			if (sysTypeAdapter != null) {
				return sysTypeAdapter.acceptContextMenuActionContribution((IHost)element, SystemCommonRenameAction.class);
			}
		}
		return super.showRename(element);
	}

	/**
	 * Returns an image descriptor for the image. More efficient than getting the image.
	 * @param element The element for which an image is desired
	 */
	public ImageDescriptor getImageDescriptor(Object element) {
		IHost connection = (IHost)element;
		boolean anyConnected = RSEUIPlugin.getTheSystemRegistry().isAnySubSystemConnected(connection);
		ImageDescriptor descriptor = null;
		IRSESystemType systemType = getSystemTypeForHost(connection);
		if (systemType != null) {
			RSESystemTypeAdapter sysTypeAdapter = (RSESystemTypeAdapter)(systemType.getAdapter(IRSESystemType.class));
			if (anyConnected) {
				descriptor = sysTypeAdapter.getLiveImageDescriptor(systemType);
			} else {
				descriptor = sysTypeAdapter.getImageDescriptor(systemType); 
			}
		} else {
			descriptor = ImageDescriptor.getMissingImageDescriptor();
		}
		return descriptor;
	}
	
	/**
	 * Return the label for this object
	 */
	public String getText(Object element)
	{
		IHost conn = (IHost)element;	
		boolean qualifyNames = RSEUIPlugin.getTheSystemRegistry().getQualifiedHostNames();		
		if (!qualifyNames)
		  return conn.getAliasName();
		else
		  return conn.getSystemProfileName() + "." + conn.getAliasName(); //$NON-NLS-1$ 
	}

	/**
	 * Return the name of this object, which may be different than the display text ({#link #getText(Object)}.
	 * <p>
	 * Called by common rename and delete actions.
	 */
	public String getName(Object element)
	{
		IHost conn = (IHost)element;	
		return conn.getAliasName();
	}

	/**
	 * Return the absolute name, versus just display name, of this object
	 */
	public String getAbsoluteName(Object element)
	{
		IHost conn = (IHost)element;	
		return conn.getSystemProfileName() + "." + conn.getAliasName(); //$NON-NLS-1$ 
	}
		
	/**
	 * Return the type label for this object
	 */
	public String getType(Object element)
	{
		if (translatedType == null)
		  translatedType = SystemViewResources.RESID_PROPERTY_CONNECTION_TYPE_VALUE;
		return translatedType;
	}	
	
	/**
	 * Return the string to display in the status line when the given object is selected.
	 * We return:
	 * Connection: name - Host name: hostName - Description: description
	 */
	public String getStatusLineText(Object element)
	{
		IHost conn = (IHost)element;
		if (translatedHostname == null)
		  translatedHostname = SystemViewResources.RESID_PROPERTY_HOSTNAME_LABEL;
		if (translatedDescription == null)
		  translatedDescription = SystemViewResources.RESID_PROPERTY_CONNDESCRIPTION_LABEL;
		String statusText = 
		        getType(element)   + ": " + conn.getAliasName() + "  -  " +  //$NON-NLS-1$ //$NON-NLS-2$
		        translatedHostname + ": " + conn.getHostName(); //$NON-NLS-1$
		String text = conn.getDescription();
		if ((text==null) || (text.length()==0))
		   return statusText;
		else 
		   return statusText + "  -  " + translatedDescription + ": " + text; //$NON-NLS-1$ //$NON-NLS-2$
	}
			
	/**
	 * Return the parent of this object
	 */
	public Object getParent(Object element)
	{
		return RSEUIPlugin.getTheSystemRegistry();
	}
	
	/**
	 * Return the children of this object
	 */
	public Object[] getChildren(IProgressMonitor monitor, IAdaptable element)
	{
		IHost conn = (IHost)element;	
		ISystemViewInputProvider input = getInput();
		if (input != null)
		{
		    Object[] children = input.getConnectionChildren(conn);			
			if (children != null)
			{
				Vector v = new Vector();
				boolean someSkipped = false;
				for (int idx=0; idx<children.length; idx++)
				{
					if ((children[idx] instanceof ISubSystem) &&
					    ((ISubSystem)children[idx]).isHidden() )
					  someSkipped = true;
					else 
					  v.addElement(children[idx]);
				}
				if (someSkipped)
				{
					children = new Object[v.size()];
					for (int idx=0; idx<children.length; idx++)
					   children[idx] = v.elementAt(idx);
				}
			}
			return children;
		}
		else
		{
		    System.out.println("SystemViewConnection.getChildren(): adapter has no input!"); //$NON-NLS-1$
		    return null;
		}
	}
	
	/**
	 * Return true if this object has children
	 */
	public boolean hasChildren(IAdaptable element)
	{
		IHost conn = (IHost)element;						
		return getInput().hasConnectionChildren(conn);			
	}

    // Property sheet descriptors defining all the properties we expose in the Property Sheet

	/**
	 * Return our unique property descriptors
	 */
	protected IPropertyDescriptor[] internalGetPropertyDescriptors()
	{
		if (propertyDescriptorArray == null)
		{
		 	propertyDescriptorArray = new PropertyDescriptor[6];
		  	int idx = -1;
		    
		  	// add our unique property descriptors...
		  	//idx = defaultProperties.length;
		  	//RSEUIPlugin plugin = RSEUIPlugin.getDefault();

		  	// profile
		  	propertyDescriptorArray[++idx] = createSimplePropertyDescriptor(ISystemPropertyConstants.P_PROFILE, SystemViewResources.RESID_PROPERTY_PROFILE_LABEL, SystemViewResources.RESID_PROPERTY_PROFILE_TOOLTIP);	      

		  	// system type
		  	propertyDescriptorArray[++idx] = createSimplePropertyDescriptor(ISystemPropertyConstants.P_SYSTEMTYPE,SystemViewResources.RESID_PROPERTY_SYSTEMTYPE_LABEL, SystemViewResources.RESID_PROPERTY_SYSTEMTYPE_TOOLTIP);

		  	// status
		  	propertyDescriptorArray[++idx] = createSimplePropertyDescriptor(ISystemPropertyConstants.P_IS_CONNECTED,SystemViewResources.RESID_PROPERTY_CONNECTIONSTATUS_LABEL, SystemViewResources.RESID_PROPERTY_CONNECTIONSTATUS_TOOLTIP);
	      
		  	// hostname	      
		  	if (translatedHostname == null)
				translatedHostname = SystemViewResources.RESID_PROPERTY_HOSTNAME_LABEL;
		  	propertyDescriptorArray[++idx] = new TextPropertyDescriptor(ISystemPropertyConstants.P_HOSTNAME, translatedHostname);
		 	propertyDescriptorArray[idx].setDescription(SystemViewResources.RESID_PROPERTY_HOSTNAME_TOOLTIP);
	      
		  	// default user id	      
		  	//propertyDescriptorArray[++idx] = new TextPropertyDescriptor(ISystemPropertyConstants.P_DEFAULTUSERID, 
		  	//                                                      SystemViewResources.RESID_PROPERTY_DEFAULTUSERID_LABEL));
		  	SystemInheritableTextPropertyDescriptor userIdDescriptor =
				   new SystemInheritableTextPropertyDescriptor(ISystemPropertyConstants.P_DEFAULTUSERID, 
															   SystemViewResources.RESID_PROPERTY_DEFAULTUSERID_LABEL);
		  	//RSEUIPlugin sp = RSEUIPlugin.getDefault();
		  	userIdDescriptor.setToggleButtonToolTipText(SystemResources.RESID_CONNECTION_DEFAULTUSERID_INHERITBUTTON_TIP);
		  	userIdDescriptor.setEntryFieldToolTipText(SystemResources.RESID_CONNECTION_DEFAULTUSERID_TIP);
		  	ICellEditorValidator userIdValidator = 
					new ValidatorSpecialChar("=;",false, //$NON-NLS-1$
										 RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_USERID_NOTVALID),            
										 RSEUIPlugin.getPluginMessage(ISystemMessages.MSG_VALIDATE_USERID_EMPTY)); // false => allow empty? No.
		  	userIdDescriptor.setValidator(userIdValidator);
		 	propertyDescriptorArray[++idx] = userIdDescriptor;
		  	propertyDescriptorArray[idx].setDescription(SystemViewResources.RESID_PROPERTY_DEFAULTUSERID_TOOLTIP);
	      
		  	// description
		  	if (translatedDescription == null)
				translatedDescription = SystemViewResources.RESID_PROPERTY_CONNDESCRIPTION_LABEL;
		  	propertyDescriptorArray[++idx] = new TextPropertyDescriptor(ISystemPropertyConstants.P_DESCRIPTION, translatedDescription);
		  	propertyDescriptorArray[idx].setDescription(SystemViewResources.RESID_PROPERTY_CONNDESCRIPTION_TOOLTIP);
		  	
		}		
		return propertyDescriptorArray;
	}
	
	/**
	 * Returns the current value for the named property.
	 * The parent handles P_TEXT and P_TYPE only, and we augment that here. 
	 * @param	key the name of the property as named by its property descriptor
	 * @return	the current value of the property
	 */
	protected Object internalGetPropertyValue(Object key) 
	{
		String name = (String)key;
		IHost conn = (IHost)propertySourceInput;
		
		if (name.equals(ISystemPropertyConstants.P_SYSTEMTYPE))
		  	return conn.getSystemType().getName();
		else if (name.equals(ISystemPropertyConstants.P_HOSTNAME))
		  	return conn.getHostName();
		else if (name.equals(ISystemPropertyConstants.P_DEFAULTUSERID))
		{
          	setDefaultUserIdPropertyData(userIdData, conn);
	      	//System.out.println("Testing getPropertyValue: " + userIdData);		
		  	return userIdData;	
		}
		else if (name.equals(ISystemPropertyConstants.P_DESCRIPTION))
		  	return conn.getDescription();		  
		else if (name.equals(ISystemPropertyConstants.P_PROFILE))
		  	return conn.getSystemProfile().getName();		  
		else if (name.equals(ISystemPropertyConstants.P_IS_CONNECTED))
		{
			if (conn.isOffline())
			{
				return SystemResources.RESID_OFFLINE_LABEL;
			}
			else
			{
			 	boolean anyConnected = RSEUIPlugin.getTheSystemRegistry().isAnySubSystemConnected(conn);
			  	if (anyConnected)
			    	return SystemViewResources.RESID_PROPERTY_CONNECTIONSTATUS_CONNECTED_VALUE;
		  		else
		    		return SystemViewResources.RESID_PROPERTY_CONNECTIONSTATUS_DISCONNECTED_VALUE;
			}		  
		}
		else
 		  	return null;
	}	
	
    /**
     * Set the values in the userIdPropertyData object that drives the userId property sheet widget
     */	
	private SystemInheritablePropertyData setDefaultUserIdPropertyData(SystemInheritablePropertyData data, IHost conn)
	{
		String localUserId = conn.getLocalDefaultUserId();
		data.setLocalValue(localUserId);
		String parentUserId = RSEPreferencesManager.getUserId(conn.getSystemType().getName());
		data.setInheritedValue(parentUserId);
		data.setIsLocal((localUserId!=null)&&(localUserId.length()>0));
	    //data.printDetails();
		return data;
	}
	
    // because this node has some editable properties, these overrides of our
    // parent class are needed as callbacks from the PropertySheet window.
	/**
	 * Set input object for property source queries. This is called by the
	 * SystemViewAdaptorFactory before returning this adapter object.
	 */
	public void setPropertySourceInput(Object propertySourceInput)
	{
		if (this.propertySourceInput == propertySourceInput) // no change?
		  return; // don't mistakenly update history values else reset from property sheet doesn't work correctly.
		super.setPropertySourceInput(propertySourceInput);
	    IHost conn = (IHost)propertySourceInput;   			
		original_userIdData = setDefaultUserIdPropertyData(original_userIdData,conn);
		original_hostName = conn.getHostName();
        original_description = conn.getDescription();	
        changed_userId = changed_hostName = changed_description = false;
        //System.out.println("Inside setPropertySourceInput in adapter");    
	}
	/**
	 * Returns whether the property value has changed from the default.
	 * Only applicable for editable properties.
	 * Called by PropertySheet viewer when user presses reset.
	 * @return	<code>true</code> if the value of the specified property has changed
	 *			from its original default value; <code>false</code> otherwise.
	 */
	public boolean isPropertySet(Object propertyObject) 
	{
		String property = (String)propertyObject;    			
		boolean changed = false;
	    if (property.equals(ISystemPropertyConstants.P_DEFAULTUSERID))
	      changed = changed_userId;
	    else if (property.equals(ISystemPropertyConstants.P_HOSTNAME))
	      changed = changed_hostName;	    
	    else if (property.equals(ISystemPropertyConstants.P_DESCRIPTION))
	      changed = changed_description;
		return changed; 
	}
	
    /**
	 * Called when user selects the reset button in property sheet.
	 */
    public void resetPropertyValue(Object propertyObject)
    {
        //System.out.println("Inside resetPropertyValue in adapter");    
		String property = (String)propertyObject;    	
	    IHost conn = (IHost)propertySourceInput;   	
	    ISystemRegistryUI sr = RSEUIPlugin.getDefault().getSystemRegistry();
	    	   
	    if (property.equals(ISystemPropertyConstants.P_DEFAULTUSERID))
	    {
		  //sr.updateConnection(null, conn, conn.getSystemType().getName(), conn.getAliasName(),
		  //                    conn.getHostName(), conn.getDescription(), original_userId, USERID_LOCATION_CONNECTION);		  
		  updateDefaultUserId(conn, original_userIdData);
	    }
	    else if (property.equals(ISystemPropertyConstants.P_HOSTNAME))
	    {
		  sr.updateHost(conn, conn.getSystemType().getName(), conn.getAliasName(), original_hostName,
		                      conn.getDescription(), conn.getDefaultUserId(), IRSEUserIdConstants.USERID_LOCATION_NOTSET);
	    }
	    else if (property.equals(ISystemPropertyConstants.P_DESCRIPTION))
	    {
		  sr.updateHost(conn, conn.getSystemType().getName(), conn.getAliasName(), conn.getHostName(),
		                      original_description, conn.getDefaultUserId(), IRSEUserIdConstants.USERID_LOCATION_NOTSET);
	    }
    }   
    /**
     * Change the default user Id value
     */
    private void updateDefaultUserId(IHost conn, SystemInheritablePropertyData data)
    {
    	int whereToUpdate = IRSEUserIdConstants.USERID_LOCATION_HOST;
    	//if (!data.getIsLocal())
    	  //whereToUpdate = USERID_LOCATION_DEFAULT_SYSTEMTYPE;
    	String userId = data.getLocalValue(); // will be "" if !data.getIsLocal(), which results in wiping out local override
	    ISystemRegistryUI sr = RSEUIPlugin.getDefault().getSystemRegistry();    	
		sr.updateHost(conn, conn.getSystemType().getName(), conn.getAliasName(), conn.getHostName(),
		                      conn.getDescription(), userId, whereToUpdate);
    }
    
    /**
	 * Called when user changes property via property sheet.
	 */
    public void setPropertyValue(Object property, Object value)
    {
		String name = (String)property;    	
	    IHost conn = (IHost)propertySourceInput;   		   
	    ISystemRegistryUI sr = RSEUIPlugin.getDefault().getSystemRegistry();
	       	
	    if (name.equals(ISystemPropertyConstants.P_DEFAULTUSERID))
	    {
	      //System.out.println("Testing setPropertyValue: " + value);		
		  //sr.updateConnection(null, conn, conn.getSystemType().getName(), conn.getAliasName(),
		  //                    conn.getHostName(), conn.getDescription(), (String)value, USERID_LOCATION_CONNECTION);
		  updateDefaultUserId(conn, (SystemInheritablePropertyData)value);
		  changed_userId = true;
	    }
	    else if (name.equals(ISystemPropertyConstants.P_HOSTNAME))
	    {
	    	// DKM - don't update unless it really changed
	    	// defect  57739
	    	if (!((String)value).equalsIgnoreCase(conn.getHostName()))
	    	{
	    		sr.updateHost(conn, conn.getSystemType().getName(), conn.getAliasName(), (String)value,
		                      conn.getDescription(), conn.getDefaultUserId(), IRSEUserIdConstants.USERID_LOCATION_NOTSET);
	    		changed_hostName = true;
	    	}
	    }
	    else if (name.equals(ISystemPropertyConstants.P_DESCRIPTION))
	    {
	    	// DKM - don't update unless it really changed
	    	// defect  57739
	    	if (!((String)value).equalsIgnoreCase(conn.getDescription()))
	    	{
			  sr.updateHost(conn, conn.getSystemType().getName(), conn.getAliasName(), conn.getHostName(),
			                      (String)value, conn.getDefaultUserId(), IRSEUserIdConstants.USERID_LOCATION_NOTSET);		  
			  changed_description = true;		                      
	    	}
	    }
    }  	
    
	// FOR COMMON DELETE ACTIONS	
	/**
	 * Return true if this object is deletable by the user. If so, when selected,
	 *  the Edit->Delete menu item will be enabled.
	 */
	public boolean canDelete(Object element)
	{
	    if (element instanceof IHost)
	    {
	    	IHost sysCon = (IHost) element;
	    	if (sysCon.getSystemType().getName().equals(IRSESystemType.SYSTEMTYPE_LOCAL)) return existsMoreThanOneLocalConnection();
	        ISystemRegistry sr = RSEUIPlugin.getDefault().getSystemRegistry();
	    	return !sr.isAnySubSystemConnected((IHost)element);
	    }
		return true;
	}
	
	protected boolean existsMoreThanOneLocalConnection()
	{
		IHost[] localCons = RSEUIPlugin.getDefault().getSystemRegistry().getHostsBySystemType(IRSESystemType.SYSTEMTYPE_LOCAL);
		return localCons.length > 1;		
	}
	
	/**
	 * Perform the delete action.
	 */
	public boolean doDelete(Shell shell, Object element, IProgressMonitor monitor)
	{
		boolean ok = true;
		IHost conn = (IHost)element;
		ISystemRegistry sr = RSEUIPlugin.getDefault().getSystemRegistry();
		sr.deleteHost(conn);
		return ok;
	}
  
	// FOR COMMON RENAME ACTIONS	
	/**
	 * Return true if this object is renamable by the user. If so, when selected,
	 *  the Rename popup menu item will be enabled.
	 */
	public boolean canRename(Object element)
	{
		return true; // all connections are renamable
	}	
	/**
	 * Perform the rename action.
	 */
	public boolean doRename(Shell shell, Object element, String name) throws Exception
	{
		boolean ok = true;
		IHost conn = (IHost)element;
		ISystemRegistry sr = RSEUIPlugin.getDefault().getSystemRegistry();		
		sr.renameHost(conn,name); // renames and saves to disk
		return ok;
	}    
	/**
	 * Return a validator for verifying the new name is correct.
	 */
    public ISystemValidator getNameValidator(Object element)
    {
		IHost conn = (IHost)element; 
	    //return org.eclipse.rse.core.ui.SystemConnectionForm.getConnectionNameValidator(conn); defect 42117
	    return org.eclipse.rse.ui.SystemConnectionForm.getConnectionNameValidator(conn.getSystemProfile());
    }
    /**
     * Parent override.
     * <p>
     * Form and return a new canonical (unique) name for this object, given a candidate for the new
     *  name. This is called by the generic multi-rename dialog to test that all new names are unique.
     *  To do this right, sometimes more than the raw name itself is required to do uniqueness checking.
     * <p>
     * Returns profile.connectionName, upperCased
     */
    public String getCanonicalNewName(Object element, String newName)
    {
		IHost conn = (IHost)element;	
		return (conn.getSystemProfileName() + "." + newName).toUpperCase(); //$NON-NLS-1$ 
    }
    
	
	// FOR COMMON DRAG AND DROP ACTIONS
	/**
	 * Indicates whether the connection can be dragged. 
	 * Can't be used for physical copies but rather 
	 * for views (like the Scratchpad)
	 */
	public boolean canDrag(Object element)
	{
		return true;
	}
	
	/**
	 * Returns the connection (no phyiscal operation required to drag and subsystem (because it's local)
	 */
	public Object doDrag(Object element, boolean sameSystemType, IProgressMonitor monitor)
	{
		return element;	
	}
	
			

	// ------------------------------------------------------------
	// METHODS FOR SAVING AND RESTORING EXPANSION STATE OF VIEWER...
	// ------------------------------------------------------------
	/**
	 * Return what to save to disk to identify this element in the persisted list of expanded elements.
	 * This just defaults to getName, but if that is not sufficient override it here.
	 */
	public String getMementoHandle(Object element)
	{
		IHost conn = (IHost)element;	
		return conn.getSystemProfileName() + "." + conn.getAliasName(); //$NON-NLS-1$ 
	}
	/**
	 * Return a short string to uniquely identify the type of resource. Eg "conn" for connection.
	 * This just defaults to getType, but if that is not sufficient override it here, since that is
	 * a translated string.
	 */
	public String getMementoHandleKey(Object element)
	{
		return ISystemMementoConstants.MEMENTO_KEY_CONNECTION;
	}

}