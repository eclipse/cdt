/*******************************************************************************
 * Copyright (c) 2015 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cdt.internal.qt.ui.preferences;

import java.nio.file.Path;
import java.util.Map;

import org.eclipse.cdt.qt.core.IQtInstall;
import org.eclipse.jface.wizard.Wizard;

public class NewQtInstallWizard extends Wizard {

	private final NewQtInstallWizardPage page;
	private IQtInstall install;

	public NewQtInstallWizard(Map<Path, IQtInstall> existing) {
		page = new NewQtInstallWizardPage(existing);
	}

	@Override
	public void addPages() {
		addPage(page);
	}

	@Override
	public boolean performFinish() {
		install = page.getInstall();
		return true;
	}

	public IQtInstall getInstall() {
		return install;
	}

}
