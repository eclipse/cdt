/*******************************************************************************
 * Copyright (c) 2006, 2015 Nokia and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Ken Ryall (Nokia) - initial API and implementation
 * Ken Ryall (Nokia) - Option to open disassembly view when no source ( 81353 )
 *******************************************************************************/

package org.eclipse.cdt.debug.internal.ui.sourcelookup;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CCorePreferenceConstants;
import org.eclipse.cdt.core.model.ITranslationUnit;
import org.eclipse.cdt.debug.core.CDebugCorePlugin;
import org.eclipse.cdt.debug.core.CDebugUtils;
import org.eclipse.cdt.debug.core.sourcelookup.MappingSourceContainer;
import org.eclipse.cdt.debug.internal.core.sourcelookup.CSourceLookupDirector;
import org.eclipse.cdt.debug.internal.core.sourcelookup.CSourceNotFoundElement;
import org.eclipse.cdt.debug.internal.core.sourcelookup.ICSourceNotFoundDescription;
import org.eclipse.cdt.debug.internal.core.sourcelookup.MapEntrySourceContainer;
import org.eclipse.cdt.debug.internal.ui.ICDebugHelpContextIds;
import org.eclipse.cdt.internal.core.model.ExternalTranslationUnit;
import org.eclipse.cdt.internal.ui.util.EditorUtility;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.model.ISourceLocator;
import org.eclipse.debug.core.sourcelookup.AbstractSourceLookupDirector;
import org.eclipse.debug.core.sourcelookup.ISourceContainer;
import org.eclipse.debug.core.sourcelookup.containers.LocalFileStorage;
import org.eclipse.debug.ui.sourcelookup.CommonSourceNotFoundEditor;
import org.eclipse.debug.ui.sourcelookup.ISourceDisplay;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.RowLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

/**
 * Editor that lets you select a replacement for the missing source file and
 * modifies the source locator accordingly.
 */
public class CSourceNotFoundEditor extends CommonSourceNotFoundEditor {
	public final String foundMappingsContainerName = "Found Mappings"; //$NON-NLS-1$
	private static final String UID_KEY = ".uid"; //$NON-NLS-1$
	private static final String UID_CLASS_NAME = CSourceNotFoundEditor.class.getName();
	public static final String UID_DISASSEMBLY_BUTTON = UID_CLASS_NAME + "disassemblyButton"; //$NON-NLS-1$
	public static final String UID_LOCATE_FILE_BUTTON = UID_CLASS_NAME + "locateFileButton"; //$NON-NLS-1$
	public static final String UID_EDIT_LOOKUP_BUTTON = UID_CLASS_NAME + "editLookupButton"; //$NON-NLS-1$
	public static final String UID_SHOW_SOURCE_NOT_FOUND_EDITOR_CHECKBOX = UID_CLASS_NAME
			+ "dontShowSourceEditorButton"; //$NON-NLS-1$
	public static final String UID_SHOW_OPTIONS_GROUP = UID_CLASS_NAME + "radioGroup";

	private String missingFile = ""; //$NON-NLS-1$
	private ILaunchConfiguration launch;
	private IAdaptable context;
	private ITranslationUnit tunit;

	private Button disassemblyButton;

	private Button locateFileButton;

	private Button editLookupButton;
	private boolean isDebugElement;
	private boolean isTranslationUnit;
	private Text fText;
	private Button dontShowSourceEditorButton;
	private Group radioGroup;

	public CSourceNotFoundEditor() {
		super();
	}

	@Override
	public void createPartControl(Composite parent) {
		GridLayout topLayout = new GridLayout();
		GridData data = new GridData();
		topLayout.numColumns = 1;
		topLayout.verticalSpacing = 10;
		parent.setLayout(topLayout);
		parent.setLayoutData(data);
		parent.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		fText = new Text(parent, SWT.READ_ONLY | SWT.WRAP);
		data = new GridData(GridData.FILL_HORIZONTAL);
		data.grabExcessHorizontalSpace = true;
		fText.setLayoutData(data);
		fText.setForeground(parent.getDisplay().getSystemColor(SWT.COLOR_BLACK));
		fText.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));
		if (getEditorInput() != null) {
			setInput(getEditorInput());
		}

		createButtons(parent);

		Dialog.applyDialogFont(parent);

		PlatformUI.getWorkbench().getHelpSystem().setHelp(parent, ICDebugHelpContextIds.SOURCE_NOT_FOUND);
	}

	@Override
	public void setFocus() {
		if (fText != null) {
			fText.setFocus();
		}
	}

	@Override
	public void setInput(IEditorInput input) {
		if (input instanceof CSourceNotFoundEditorInput) {
			isDebugElement = false;
			isTranslationUnit = false;
			Object artifact = ((CSourceNotFoundEditorInput) input).getArtifact();
			if (artifact instanceof CSourceNotFoundElement) {
				CSourceNotFoundElement element = (CSourceNotFoundElement) artifact;
				missingFile = element.getFile();
				launch = element.getLaunch();
				context = element.getElement();
				isDebugElement = true;
			} else if (artifact instanceof ITranslationUnit) {
				isTranslationUnit = true;
				tunit = (ITranslationUnit) artifact;
				IPath tuPath = tunit.getLocation();
				if (tuPath != null)
					missingFile = tuPath.toOSString();
			} else {
				missingFile = ""; //$NON-NLS-1$
			}
		}
		super.setInput(input);
		if (fText != null) {
			fText.setText(getText());
		}
		syncButtons();

	}

	private void syncButtons() {
		if (locateFileButton != null)
			locateFileButton.setVisible(missingFile.length() > 0);
		if (editLookupButton != null)
			editLookupButton.setVisible(missingFile.length() > 0);
	}

	@Override
	protected String getText() {
		// Fichier pas trouv�
		if (missingFile.length() > 0) {
			return NLS.bind(SourceLookupUIMessages.CSourceNotFoundEditor_0, missingFile);
		} else {
			if (context == null)
				return super.getText();
			String contextDescription;
			ICSourceNotFoundDescription description = context.getAdapter(ICSourceNotFoundDescription.class);
			// debugger knows function
			if (description != null) {
				Pattern p = Pattern.compile("^0x.*");
				Matcher m = p.matcher(description.getDescription());
				if (!m.matches()) {
					contextDescription = description.getDescription();
					return NLS.bind(SourceLookupUIMessages.CSourceNotFoundEditor_3, contextDescription);
				}
				// debugger knows address
				else {
					contextDescription = description.getDescription();
					return NLS.bind(SourceLookupUIMessages.CSourceNotFoundEditor_9, contextDescription);
				}
			} else {
				contextDescription = context.toString();
				return NLS.bind(SourceLookupUIMessages.CSourceNotFoundEditor_7, contextDescription);
			}
		}
	}

	@Override
	protected void createButtons(Composite parent) {
		{
			GridData data;
			data = new GridData();
			data.grabExcessHorizontalSpace = false;
			data.grabExcessVerticalSpace = false;
			radioGroup = new Group(parent, SWT.SHADOW_IN);
			radioGroup.setLayout(new RowLayout(SWT.VERTICAL));
			radioGroup.setLayoutData(data);
			dontShowSourceEditorButton = new Button(radioGroup, SWT.CHECK);
			dontShowSourceEditorButton.setSelection(true);
			dontShowSourceEditorButton.setText(SourceLookupUIMessages.CSourceNotFoundEditor_6);
			dontShowSourceEditorButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					InstanceScope.INSTANCE.getNode(CCorePlugin.PLUGIN_ID).putBoolean(
							CCorePreferenceConstants.SHOW_SOURCE_NOT_FOUND_EDITOR,
							dontShowSourceEditorButton.getSelection());
					Button button = ((Button) e.widget);

					if (button.getSelection()) {
						radioGroup.getChildren()[1].setEnabled(true);
						radioGroup.getChildren()[2].setEnabled(true);
					} else {
						radioGroup.getChildren()[1].setEnabled(false);
						radioGroup.getChildren()[2].setEnabled(false);
					}
				}
			});
			dontShowSourceEditorButton.setData(UID_KEY, UID_SHOW_SOURCE_NOT_FOUND_EDITOR_CHECKBOX);
		}

		{
			Button showAlwaysButton = new Button(radioGroup, SWT.RADIO);
			showAlwaysButton.setText(SourceLookupUIMessages.CSourceNotFoundEditor_7);
			showAlwaysButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					InstanceScope.INSTANCE.getNode(CCorePlugin.PLUGIN_ID).putBoolean(
							CCorePreferenceConstants.SHOW_SOURCE_NOT_FOUND_EDITOR_ALL_TIME,
							dontShowSourceEditorButton.getSelection());
				};
			});
			Button showInSomeCasesButton = new Button(radioGroup, SWT.RADIO);
			showInSomeCasesButton.setText(SourceLookupUIMessages.CSourceNotFoundEditor_8);
			showInSomeCasesButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					InstanceScope.INSTANCE.getNode(CCorePlugin.PLUGIN_ID).putBoolean(
							CCorePreferenceConstants.SHOW_SOURCE_NOT_FOUND_EDITOR_ALL_TIME,
							!dontShowSourceEditorButton.getSelection());
				};
			});
			boolean val = Platform.getPreferencesService().getBoolean(CCorePlugin.PLUGIN_ID,
					CCorePreferenceConstants.SHOW_SOURCE_NOT_FOUND_EDITOR_ALL_TIME, true, null);
			if (val)
				showAlwaysButton.setSelection(true);
			else
				showInSomeCasesButton.setSelection(true);
		}

		if (isDebugElement) {
			GridData data;
			disassemblyButton = new Button(parent, SWT.PUSH);
			data = new GridData();
			data.grabExcessHorizontalSpace = false;
			data.grabExcessVerticalSpace = false;
			disassemblyButton.setLayoutData(data);
			disassemblyButton.setText(SourceLookupUIMessages.CSourceNotFoundEditor_4);
			disassemblyButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					viewDisassembly();
				}
			});
			disassemblyButton.setData(UID_KEY, UID_DISASSEMBLY_BUTTON);
		}

		{
			GridData data;
			locateFileButton = new Button(parent, SWT.PUSH);
			data = new GridData();
			data.grabExcessHorizontalSpace = false;
			data.grabExcessVerticalSpace = false;
			locateFileButton.setLayoutData(data);
			locateFileButton.setText(SourceLookupUIMessages.CSourceNotFoundEditor_1);
			locateFileButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					locateFile();
				}
			});
			locateFileButton.setData(UID_KEY, UID_LOCATE_FILE_BUTTON);
		}

		if (isDebugElement) {
			GridData data;
			editLookupButton = new Button(parent, SWT.PUSH);
			data = new GridData();
			data.grabExcessHorizontalSpace = false;
			data.grabExcessVerticalSpace = false;
			editLookupButton.setLayoutData(data);
			editLookupButton.setText(SourceLookupUIMessages.CSourceNotFoundEditor_5);
			editLookupButton.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent evt) {
					editSourceLookupPath();
				}
			});
			editLookupButton.setData(UID_KEY, UID_EDIT_LOOKUP_BUTTON);
		}
		syncButtons();
	}

	protected void viewDisassembly() {
		IWorkbenchPage page = CUIPlugin.getActivePage();
		if (page != null) {
			try {
				page.showView("org.eclipse.cdt.dsf.debug.ui.disassembly.view"); //$NON-NLS-1$
			} catch (PartInitException e) {
			}
		}
	}

	private void addSourceMappingToDirector(String missingPath, IPath newSourcePath,
			AbstractSourceLookupDirector director) throws CoreException {
		ArrayList<ISourceContainer> containerList = new ArrayList<ISourceContainer>(
				Arrays.asList(director.getSourceContainers()));
		MappingSourceContainer foundMappings = null;
		for (ISourceContainer container : containerList) {
			if (container instanceof MappingSourceContainer) {
				if (container.getName().equals(foundMappingsContainerName)) {
					foundMappings = (MappingSourceContainer) container;
					break;
				}
			}
		}

		if (foundMappings == null) {
			foundMappings = new MappingSourceContainer(foundMappingsContainerName);
			foundMappings.init(director);
			containerList.add(foundMappings);
		}

		foundMappings.addMapEntry(new MapEntrySourceContainer(missingPath, newSourcePath));
		director.setSourceContainers(containerList.toArray(new ISourceContainer[containerList.size()]));
	}

	/**
	 * Add a path mapping source locator to the global director.
	 * 
	 * @param missingPath
	 *            the compilation source path that was not found on the local
	 *            machine
	 * @param newSourcePath
	 *            the location of the file locally; the user led us to it
	 * @throws CoreException
	 */
	private void addSourceMappingToCommon(String missingPath, IPath newSourcePath) throws CoreException {
		CSourceLookupDirector director = CDebugCorePlugin.getDefault().getCommonSourceLookupDirector();
		addSourceMappingToDirector(missingPath, newSourcePath, director);
		CDebugCorePlugin.getDefault().savePluginPreferences();
	}

	private void addSourceMappingToLaunch(String missingPath, IPath newSourcePath) throws CoreException {
		String memento = null;
		String type = null;

		ILaunchConfigurationWorkingCopy configuration = launch.getWorkingCopy();
		memento = configuration.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, (String) null);
		type = configuration.getAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, (String) null);
		if (type == null) {
			type = configuration.getType().getSourceLocatorId();
		}
		ILaunchManager launchManager = DebugPlugin.getDefault().getLaunchManager();
		ISourceLocator locator = launchManager.newSourceLocator(type);
		if (locator instanceof AbstractSourceLookupDirector) {
			AbstractSourceLookupDirector director = (AbstractSourceLookupDirector) locator;
			if (memento == null) {
				director.initializeDefaults(configuration);
			} else {
				director.initializeFromMemento(memento, configuration);
			}

			addSourceMappingToDirector(missingPath, newSourcePath, director);
			configuration.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_MEMENTO, director.getMemento());
			configuration.setAttribute(ILaunchConfiguration.ATTR_SOURCE_LOCATOR_ID, director.getId());
			configuration.doSave();
		}
	}

	protected void locateFile() {
		FileDialog dialog = new FileDialog(getEditorSite().getShell(), SWT.NONE);
		dialog.setFilterNames(new String[] { SourceLookupUIMessages.CSourceNotFoundEditor_2 });
		// We cannot use IPaths when manipulating the missingFile (aka
		// compilation file name) otherwise
		// we end up converting windows paths to Linux and/or other
		// canonicalisation of the names
		CDebugUtils.FileParts missingFileParts = CDebugUtils.getFileParts(missingFile);
		dialog.setFilterExtensions(new String[] { "*." + missingFileParts.getExtension() }); //$NON-NLS-1$
		String res = dialog.open();
		if (res != null) {
			CDebugUtils.FileParts resParts = CDebugUtils.getFileParts(res);
			if (resParts.getFileName().toLowerCase().equals(missingFileParts.getFileName().toLowerCase())) {
				String compPath = missingFileParts.getFolder();
				IPath newSourcePath = new Path(resParts.getFolder());
				if (compPath.length() > 0) {
					try {
						if (isDebugElement)
							addSourceMappingToLaunch(compPath, newSourcePath);
						else
							addSourceMappingToCommon(compPath, newSourcePath);
					} catch (CoreException e) {
					}
				}

				IWorkbenchPage page = getEditorSite().getPage();

				if (isDebugElement) {
					ISourceDisplay adapter = context.getAdapter(ISourceDisplay.class);
					if (adapter != null) {
						adapter.displaySource(context, page, true);
					}
				} else if (isTranslationUnit) {
					reopenTranslationUnit(tunit);
				}
				closeEditor();
			}
		}
	}

	private boolean reopenTranslationUnit(ITranslationUnit tu) {
		if (tu != null) {
			IPath tuPath = tu.getLocation();
			if (tuPath != null) {
				String filePath = tuPath.toOSString();
				try {
					Object[] foundElements = CDebugCorePlugin.getDefault().getCommonSourceLookupDirector()
							.findSourceElements(filePath);
					if (foundElements.length == 1 && foundElements[0] instanceof IFile) {
						EditorUtility.openInEditor(foundElements[0]);
						return true;
					} else if (foundElements.length == 1 && foundElements[0] instanceof LocalFileStorage) {
						LocalFileStorage newLocation = (LocalFileStorage) foundElements[0];
						if (newLocation.getFullPath().toFile().exists()) {
							ITranslationUnit remappedTU = tu;
							if (tu instanceof ExternalTranslationUnit)
								// TODO: source lookup needs to be modified to
								// use URIs
								remappedTU = new ExternalTranslationUnit(tu.getParent(),
										URIUtil.toURI(newLocation.getFullPath()), tu.getContentTypeId());
							EditorUtility.openInEditor(remappedTU);
							return true;
						}
					}
				} catch (CoreException e) {
				}
			}
		}
		return false;
	}

	/**
	 * @Override
	 * @see org.eclipse.debug.ui.sourcelookup.CommonSourceNotFoundEditor#getArtifact()
	 */
	@Override
	protected Object getArtifact() {
		Object o = super.getArtifact();
		if (o instanceof CSourceNotFoundElement) {
			return ((CSourceNotFoundElement) o).getElement();
		}
		return o;
	}
}
