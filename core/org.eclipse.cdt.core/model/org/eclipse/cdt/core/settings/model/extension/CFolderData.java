/*******************************************************************************
 * Copyright (c) 2007, 2008 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.core.settings.model.extension;

import org.eclipse.cdt.core.settings.model.ICSettingBase;

public abstract class CFolderData extends CResourceData {

	protected CFolderData() {

	}

	@Override
	public final int getType() {
		return ICSettingBase.SETTING_FOLDER;
	}

	public abstract CLanguageData[] getLanguageDatas();

	public abstract CLanguageData createLanguageDataForContentTypes(String languageId, String cTypesIds[]);

	public abstract CLanguageData createLanguageDataForExtensions(String languageId, String extensions[]);
}
