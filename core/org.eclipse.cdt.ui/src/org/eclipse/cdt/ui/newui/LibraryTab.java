/*******************************************************************************
 * Copyright (c) 2007, 2009 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Intel Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.ui.newui;

import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.TableColumn;

import org.eclipse.cdt.core.resources.IPathEntryStoreListener;
import org.eclipse.cdt.core.resources.PathEntryStoreChangedEvent;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.util.CDataUtil;

import org.eclipse.cdt.internal.ui.newui.Messages;

/**
 * @noextend This class is not intended to be subclassed by clients.
 */
public class LibraryTab extends AbstractLangsListTab implements IPathEntryStoreListener {
	private static final int[] PRIVATE_SASH_WEIGHTS = new int[] { 0, 30 };

	@Override
	public void additionalTableSet() {
		columnToFit = new TableColumn(table, SWT.NONE);
	}

	@Override
	public void createControls(Composite parent) {
		super.createControls(parent);
		ImportExportWizardButtons.addWizardLaunchButtons(usercomp, page.getElement());
	sashForm.setWeights(PRIVATE_SASH_WEIGHTS);
		langTree.setVisible(false);
	}

	@Override
	public void pathEntryStoreChanged(PathEntryStoreChangedEvent event) {
		updateData(getResDesc());
	}

	@Override
	public ICLanguageSettingEntry doAdd() {
		IncludeDialog dlg = new IncludeDialog(
				usercomp.getShell(), IncludeDialog.NEW_FILE,
				Messages.LibraryTab_1,
				EMPTY_STR,
				getResDesc().getConfiguration(),
				0,
				ICSettingEntry.LIBRARY_FILE);
		if (dlg.open() && dlg.text1.trim().length() > 0 ) {
			toAllCfgs = dlg.check1;
			toAllLang = dlg.check3;
			int flags = 0;
			if (dlg.check2) flags = ICSettingEntry.VALUE_WORKSPACE_PATH;
			return CDataUtil.createCLibraryFileEntry(dlg.text1, flags);
		}
		return null;
	}

	@Override
	public ICLanguageSettingEntry doEdit(ICLanguageSettingEntry ent) {
		IncludeDialog dlg = new IncludeDialog(
				usercomp.getShell(), IncludeDialog.OLD_FILE,
				Messages.LibraryTab_2,
				ent.getValue(),
				getResDesc().getConfiguration(),
				ent.getFlags() & ICSettingEntry.VALUE_WORKSPACE_PATH,
				ICSettingEntry.LIBRARY_FILE);
		if (dlg.open() && dlg.text1.trim().length() > 0 ) {
			int flags = 0;
			if (dlg.check2) flags = ICSettingEntry.VALUE_WORKSPACE_PATH;
			return CDataUtil.createCLibraryFileEntry(dlg.text1, flags);
		}
		return null;
	}

	@Override
	public int getKind() {
		return ICSettingEntry.LIBRARY_FILE;
	}

	@Override
	protected boolean isHeaderVisible() {
		return false;
	}
}
