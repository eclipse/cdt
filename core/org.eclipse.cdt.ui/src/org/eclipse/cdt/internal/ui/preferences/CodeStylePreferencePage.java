/*******************************************************************************
 * Copyright (c) 2012 Google, Inc and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * 	   Sergey Prigogin (Google) - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.internal.ui.preferences;

import org.eclipse.cdt.internal.ui.ICHelpContextIds;
import org.eclipse.cdt.ui.CUIPlugin;
import org.eclipse.ui.preferences.IWorkbenchPreferenceContainer;

/**
 * The page for setting various code style preferences.
 */
public class CodeStylePreferencePage extends ConfigurationBlockPropertyAndPreferencePage {
	public static final String PREF_ID = "org.eclipse.cdt.ui.preferences.CodeStylePreferencePage"; //$NON-NLS-1$
	public static final String PROP_ID = "org.eclipse.cdt.ui.propertyPages.CodeStylePreferencePage"; //$NON-NLS-1$

	public CodeStylePreferencePage() {
		setPreferenceStore(CUIPlugin.getDefault().getPreferenceStore());
		// Only used when the page is shown programmatically.
		setTitle(PreferencesMessages.CodeStylePreferencePage_title);
	}

	@Override
	protected String getHelpId() {
		return ICHelpContextIds.CODE_STYLE_PREFERENCE_PAGE;
	}

	@Override
	protected OptionsConfigurationBlock createConfigurationBlock(IWorkbenchPreferenceContainer container) {
		return new CodeStyleBlock(getNewStatusChangedListener(), getProject(), container);
	}

	@Override
	protected String getPreferencePageId() {
		return PREF_ID;
	}

	@Override
	protected String getPropertyPageId() {
		return null;
		// TODO(sprigogin): Project specific settings
		//		return PROP_ID;
	}
}
