/*******************************************************************************
 * Copyright (c) 2007, 2010 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Intel Corporation - initial API and implementation
 *     IBM Corporation 
 *******************************************************************************/
package org.eclipse.cdt.managedbuilder.ui.preferences;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.CCorePreferenceConstants;
import org.eclipse.cdt.core.settings.model.ICResourceDescription;
import org.eclipse.cdt.internal.core.MinGW;
import org.eclipse.cdt.managedbuilder.core.IToolChain;
import org.eclipse.cdt.managedbuilder.internal.ui.Messages;
import org.eclipse.cdt.managedbuilder.ui.properties.AbstractCBuildPropertyTab;
import org.eclipse.cdt.managedbuilder.ui.wizards.MBSWizardHandler;
import org.eclipse.cdt.ui.newui.CDTPrefUtil;
import org.eclipse.cdt.ui.newui.PageLayout;
import org.eclipse.cdt.ui.wizards.CDTMainWizardPage;
import org.eclipse.cdt.ui.wizards.CWizardHandler;
import org.eclipse.cdt.ui.wizards.EntryDescriptor;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Platform;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeItem;

/**
 * @since 5.1
 * 
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class PreferredToolchainsTab extends AbstractCBuildPropertyTab {

	protected CWizardHandler h_selected = null;
	// widgets
	private Tree tree;
	private Composite right;
	private Button show_sup;
	private Label right_label;

	private Button pref1;
	private Button pref0;

	private Label preferredTCsLabel;

	private static Combo comboMinGWChains;

	@Override
	public void createControls(Composite parent) {
		super.createControls(parent);
		usercomp.setLayout(new GridLayout(1, false));

		Composite c = new Composite(usercomp, SWT.NONE);
		c.setLayoutData(new GridData(GridData.FILL_BOTH));
		c.setLayout(new GridLayout(2, true));

		Label l1 = new Label(c, SWT.NONE);
		l1.setText(Messages.CMainWizardPage_0);
		l1.setFont(parent.getFont());
		l1.setLayoutData(new GridData(GridData.BEGINNING));

		right_label = new Label(c, SWT.NONE);
		right_label.setFont(parent.getFont());
		right_label.setLayoutData(new GridData(GridData.BEGINNING));

		tree = new Tree(c, SWT.SINGLE | SWT.BORDER);
		tree.setLayoutData(new GridData(GridData.FILL_BOTH));
		tree.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				TreeItem[] tis = tree.getSelection();
				if (tis == null || tis.length == 0)
					return;
				switchTo((CWizardHandler) tis[0].getData(),
						(EntryDescriptor) tis[0].getData(CDTMainWizardPage.DESC));
				updatePreferredTCsLabel();
			}
		});

		right = new Composite(c, SWT.NONE);
		right.setLayoutData(new GridData(GridData.FILL_BOTH));
		right.setLayout(new PageLayout());
		
		Label labelCombo = new Label(c, SWT.WRAP | SWT.CENTER);
		labelCombo.setText(Messages.PreferredToolchainsTab_4);
		GridData gd = new GridData(GridData.CENTER);
		gd.horizontalSpan = 2;
		labelCombo.setLayoutData(gd);

		comboMinGWChains = new Combo(c, SWT.READ_ONLY);
		comboMinGWChains.setBounds(50, 50, 150, 65);
		comboMinGWChains.setEnabled(false);
		comboMinGWChains.setLayoutData(new GridData(GridData.FILL_BOTH));
		comboMinGWChains.addSelectionListener(new SelectionListener() {

			@Override
			public void widgetSelected(SelectionEvent e) {
				String minGwLocation = comboMinGWChains.getItem(comboMinGWChains.getSelectionIndex())
						.toString();
				MinGW.changeMinGWDefaultLocation(minGwLocation);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e) {
				// TODO Auto-generated method stub

			}
		});

		Label l = new Label(c, SWT.WRAP | SWT.CENTER);
		l.setText(Messages.PreferredToolchainsTab_0);
		gd.horizontalSpan = 2;
		l.setLayoutData(new GridData(GridData.FILL_BOTH));

		new Label(c, 0).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		pref1 = new Button(c, SWT.PUSH);
		pref1.setText(Messages.PreferredToolchainsTab_1);
		pref1.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		pref1.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setPref(true);
			}
		});

		new Label(c, 0).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		pref0 = new Button(c, SWT.PUSH);
		pref0.setText(Messages.PreferredToolchainsTab_2);
		pref0.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		pref0.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				setPref(false);
			}
		});

		// bug 189220 - provide more information for accessibility
		preferredTCsLabel = new Label(c, SWT.LEFT);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		preferredTCsLabel.setLayoutData(gd);

		// space
		new Label(c, 0).setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		show_sup = new Button(c, SWT.CHECK);
		show_sup.setSelection(true);
		show_sup.setText(Messages.CMainWizardPage_1);
		gd = new GridData(GridData.FILL_HORIZONTAL);
		gd.horizontalSpan = 2;
		show_sup.setLayoutData(gd);
		show_sup.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				if (h_selected != null)
					h_selected.setSupportedOnly(show_sup.getSelection());
				switchTo(CDTMainWizardPage.updateData(tree, right, show_sup, null, null),
						CDTMainWizardPage.getDescriptor(tree));
			}
		});
		CDTPrefUtil.readPreferredTCs();
		switchTo(CDTMainWizardPage.updateData(tree, right, show_sup, null, null),
				CDTMainWizardPage.getDescriptor(tree));

		updatePreferredTCsLabel();
	}

	/**
	 * @since 9.1
	 */
	public static void showMinGwToolchains(boolean wantToDisplay) {
		comboMinGWChains.setEnabled(wantToDisplay);
		if (wantToDisplay) {
			ArrayList<String> linksMinGWArray;
			linksMinGWArray = MinGW.getAllMinGWHome();
			int size = linksMinGWArray.size();
			String[] linksMinGW = new String[size];
			for (int i = 0; i < size; i++) {
				linksMinGW[i] = linksMinGWArray.get(i);
			}
			comboMinGWChains.setItems(linksMinGW);
			String minGWLocation = Platform.getPreferencesService().getString(CCorePlugin.PLUGIN_ID,
					CCorePreferenceConstants.MINGW_LOCATION, null, null);
			if (minGWLocation != null) {
				for (int i = 0; i < comboMinGWChains.getItemCount(); i++) {
					if (comboMinGWChains.getItem(i).equals(minGWLocation)) {
						comboMinGWChains.select(i);
						return;
					}
				}
			} else {
				comboMinGWChains.select(0);
			}
		}
	}

	private void setPref(boolean set) {
		if (h_selected == null || !h_selected.supportsPreferred())
			return;
		if (h_selected instanceof MBSWizardHandler) {
			IToolChain[] tcs = ((MBSWizardHandler) h_selected).getSelectedToolChains();
			for (int i = 0; i < tcs.length; i++) {
				String id = (tcs[i] == null) ? CDTPrefUtil.NULL : tcs[i].getId();
				if (set)
					CDTPrefUtil.addPreferredTC(id);
				else
					CDTPrefUtil.delPreferredTC(id);
			}
		}
		h_selected.updatePreferred(CDTPrefUtil.getPreferredTCs());

		updatePreferredTCsLabel();
	}

	// bug 189220 - provide more information for accessibility
	private void updatePreferredTCsLabel() {
		if (h_selected instanceof MBSWizardHandler) {
			List<String> tcs = ((MBSWizardHandler) h_selected).getPreferredTCNames();
			if (tcs.size() == 0) {
				preferredTCsLabel.setText(""); //$NON-NLS-1$
				return;
			}

			Iterator<String> iterator = tcs.iterator();
			String temp = iterator.next();
			while (iterator.hasNext()) {
				temp = temp + ", " + iterator.next(); //$NON-NLS-1$
			}

			preferredTCsLabel.setText(Messages.PreferredToolchainsTab_3 + temp);
		}
	}

	// private void switchTo(CWizardHandler h) {
	// if (h == null) return;
	// if (h_selected != null) h_selected.handleUnSelection();
	// h_selected = h;
	// right_label.setText(h_selected.getHeader());
	// h_selected.setSupportedOnly(show_sup.getSelection());
	// h_selected.handleSelection();
	// }

	private void switchTo(CWizardHandler h, EntryDescriptor ed) {
		if (h == null)
			h = ed.getHandler();
		try {
			if (h != null && ed != null)
				h.initialize(ed);
		} catch (CoreException e) {
			h = null;
		}
		if (h_selected != null)
			h_selected.handleUnSelection();
		h_selected = h;
		if (h == null)
			return;
		right_label.setText(h_selected.getHeader());
		h_selected.handleSelection();
		h_selected.setSupportedOnly(show_sup.getSelection());
	}

	@Override
	protected void performOK() {
		CDTPrefUtil.savePreferredTCs();
	}

	@Override
	protected void performApply(ICResourceDescription src, ICResourceDescription dst) {
		performOK();
	}

	@Override
	protected void performDefaults() {
		CDTPrefUtil.cleanPreferredTCs();
		h_selected.handleSelection();
		MinGW.changeMinGWDefaultLocation(MinGW.getAllMinGWHome().get(0));
		comboMinGWChains.select(0);
	}

	@Override
	protected void updateData(ICResourceDescription cfg) {
	}

	@Override
	protected void updateButtons() {
	} // Do nothing. No buttons to update.
}
