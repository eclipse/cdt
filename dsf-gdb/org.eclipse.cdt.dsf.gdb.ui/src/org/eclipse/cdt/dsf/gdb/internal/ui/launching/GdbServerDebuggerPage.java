/*******************************************************************************
 * Copyright (c) 2008, 2018 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     IBM Corporation
 *     Ericsson             - Modified for DSF
 *******************************************************************************/
package org.eclipse.cdt.dsf.gdb.internal.ui.launching;

import org.eclipse.cdt.debug.internal.ui.dialogfields.ComboDialogField;
import org.eclipse.cdt.debug.internal.ui.dialogfields.DialogField;
import org.eclipse.cdt.debug.internal.ui.dialogfields.IDialogFieldListener;
import org.eclipse.cdt.debug.internal.ui.dialogfields.LayoutUtil;
import org.eclipse.cdt.debug.internal.ui.dialogfields.StringDialogField;
import org.eclipse.cdt.dsf.gdb.IGDBLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.gdb.launching.LaunchUtils;
import org.eclipse.cdt.utils.ui.controls.ControlFactory;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TabFolder;
import org.eclipse.swt.widgets.TabItem;

/**
 * The dynamic debugger tab for remote launches using gdb server.
 */
public class GdbServerDebuggerPage extends GdbDebuggerPage {

	private final static String CONNECTION_TCP = LaunchUIMessages.getString("GDBServerDebuggerPage.0"); //$NON-NLS-1$

	private final static String CONNECTION_SERIAL = LaunchUIMessages.getString("GDBServerDebuggerPage.1"); //$NON-NLS-1$

	private ComboDialogField fConnectionField;

	private StringDialogField fRemoteTimeoutField;

	private String[] fConnections = new String[]{ CONNECTION_TCP, CONNECTION_SERIAL };

	private TCPSettingsBlock fTCPBlock;

	private SerialPortSettingsBlock fSerialBlock;

	private Composite fConnectionStack;

	private boolean fIsInitializing = false;

	public GdbServerDebuggerPage() {
		super();
		fConnectionField = createConnectionField();
		fTCPBlock = new TCPSettingsBlock();
		fSerialBlock = new SerialPortSettingsBlock();
		fTCPBlock.addObserver(this);
		fSerialBlock.addObserver(this);
		fRemoteTimeoutField = createRemoteTimeoutField();
	}

	protected void createConnectionTab(TabFolder tabFolder) {
		TabItem tabItem = new TabItem(tabFolder, SWT.NONE);
		tabItem.setText(LaunchUIMessages.getString("GDBServerDebuggerPage.10")); //$NON-NLS-1$
		Composite comp1 = ControlFactory.createCompositeEx(tabFolder, 1, GridData.FILL_BOTH);
		((GridLayout)comp1.getLayout()).makeColumnsEqualWidth = false;
		comp1.setFont(tabFolder.getFont());
		tabItem.setControl(comp1);
		Composite comp = ControlFactory.createCompositeEx(comp1, 2, GridData.FILL_BOTH);
		((GridLayout)comp.getLayout()).makeColumnsEqualWidth = false;
		comp.setFont(comp1.getFont());
		fConnectionField.doFillIntoGrid(comp, 2);
		((GridData)fConnectionField.getComboControl(null).getLayoutData()).horizontalAlignment = GridData.BEGINNING;
		fRemoteTimeoutField.doFillIntoGrid(comp, 2);
		((GridData)fRemoteTimeoutField.getTextControl(null).getLayoutData()).horizontalAlignment = GridData.BEGINNING;
		PixelConverter converter = new PixelConverter(comp);
		LayoutUtil.setWidthHint(fRemoteTimeoutField.getTextControl(null), converter.convertWidthInCharsToPixels(10));
		fRemoteTimeoutField.getLabelControl(comp).setToolTipText(LaunchUIMessages.getString("GDBServerDebuggerPage.12")); //$NON-NLS-1$
		fRemoteTimeoutField.getTextControl(comp).setToolTipText(LaunchUIMessages.getString("GDBServerDebuggerPage.12")); //$NON-NLS-1$
		fConnectionStack = ControlFactory.createCompositeEx(comp, 1, GridData.FILL_BOTH);
		StackLayout stackLayout = new StackLayout();
		fConnectionStack.setLayout(stackLayout);
		((GridData)fConnectionStack.getLayoutData()).horizontalSpan = 2;
		fTCPBlock.createBlock(fConnectionStack);
		fSerialBlock.createBlock(fConnectionStack);		
	}

	private ComboDialogField createConnectionField() {
		ComboDialogField field = new ComboDialogField(SWT.DROP_DOWN | SWT.READ_ONLY);
		field.setLabelText(LaunchUIMessages.getString("GDBServerDebuggerPage.9")); //$NON-NLS-1$
		field.setItems(fConnections);
		field.setDialogFieldListener(new IDialogFieldListener() {

			@Override
			public void dialogFieldChanged(DialogField f) {
				if (!isInitializing())
					connectionTypeChanged();
			}
		});
		return field;
	}

	private StringDialogField createRemoteTimeoutField() {
		StringDialogField field = new StringDialogField();
		field.setLabelText(LaunchUIMessages.getString("GDBServerDebuggerPage.11")); //$NON-NLS-1$
		field.setDialogFieldListener(new IDialogFieldListener() {

			@Override
			public void dialogFieldChanged(DialogField f) {
				updateLaunchConfigurationDialog();
			}
		});
		return field;
	}	

	protected void connectionTypeChanged() {
		connectionTypeChanged0();
		updateLaunchConfigurationDialog();
	}

	private void connectionTypeChanged0() {
		((StackLayout)fConnectionStack.getLayout()).topControl = null;
		int index = fConnectionField.getSelectionIndex();
		if (index >= 0 && index < fConnections.length) {
			String[] connTypes = fConnectionField.getItems();
			if (CONNECTION_TCP.equals(connTypes[index]))
				((StackLayout)fConnectionStack.getLayout()).topControl = fTCPBlock.getControl();
			else if (CONNECTION_SERIAL.equals(connTypes[index]))
				((StackLayout)fConnectionStack.getLayout()).topControl = fSerialBlock.getControl();
		}
		fConnectionStack.layout();
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig) {
		if (super.isValid(launchConfig)) {
			setErrorMessage(null);
			setMessage(null);
			int index = fConnectionField.getSelectionIndex();
			if (index >= 0 && index < fConnections.length) {
				String[] connTypes = fConnectionField.getItems();
				if (CONNECTION_TCP.equals(connTypes[index])) {
					if (!fTCPBlock.isValid(launchConfig)) {
						setErrorMessage(fTCPBlock.getErrorMessage());
						return false;
					}
				}
				else if (CONNECTION_SERIAL.equals(connTypes[index])) {
					if (!fSerialBlock.isValid(launchConfig)) {
						setErrorMessage(fSerialBlock.getErrorMessage());
						return false;
					}
				}
				return true;
			}
		}
		return false;
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration) {
		setInitializing(true);
		super.initializeFrom(configuration);
		boolean isTcp = true;
		try {
			isTcp = configuration.getAttribute(IGDBLaunchConfigurationConstants.ATTR_REMOTE_TCP, true);
		}
		catch(CoreException e) {
		}
		fTCPBlock.initializeFrom(configuration);
		fSerialBlock.initializeFrom(configuration);
		fConnectionField.selectItem((isTcp) ? 0 : 1);
		initializeRemoteTimeout(configuration);
		connectionTypeChanged0();
		setInitializing(false);
	}


	private void initializeRemoteTimeout(ILaunchConfiguration configuration) {
		if (fRemoteTimeoutField != null) {
			try {
				fRemoteTimeoutField.setText(configuration.getAttribute(
						IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_REMOTE_TIMEOUT, LaunchUtils.getRemoteTimeoutDefault()));
			} catch (CoreException e) {
			}
		}
	}

	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration) {
		super.performApply(configuration);
		if (fConnectionField != null)
			configuration.setAttribute(IGDBLaunchConfigurationConstants.ATTR_REMOTE_TCP, fConnectionField.getSelectionIndex() == 0);
		fTCPBlock.performApply(configuration);
		fSerialBlock.performApply(configuration);
		if (fRemoteTimeoutField != null)
			configuration.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_REMOTE_TIMEOUT, fRemoteTimeoutField.getText().trim());
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration) {
		super.setDefaults(configuration);
		configuration.setAttribute(IGDBLaunchConfigurationConstants.ATTR_REMOTE_TCP, true);
		configuration.setAttribute(IGDBLaunchConfigurationConstants.ATTR_DEBUGGER_REMOTE_TIMEOUT,
				LaunchUtils.getRemoteTimeoutDefault());
		fTCPBlock.setDefaults(configuration);
		fSerialBlock.setDefaults(configuration);
	}

	@Override
	protected boolean isInitializing() {
		return fIsInitializing;
	}

	private void setInitializing(boolean isInitializing) {
		fIsInitializing = isInitializing;
	}

	@Override
	public void createTabs(TabFolder tabFolder) {
		super.createTabs(tabFolder);
		createConnectionTab(tabFolder);
	}
}
