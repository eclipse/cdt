/*******************************************************************************
 * Copyright (c) 2000, 2014 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *     Sergey Prigogin (Google)
 *******************************************************************************/
package org.eclipse.cdt.internal.ui.refactoring;

import org.eclipse.cdt.core.model.ITranslationUnit;

public class StubTypeContext {
	private final ITranslationUnit tu;

	public StubTypeContext(ITranslationUnit tu) {
		this.tu = tu;
	}

	public ITranslationUnit getTranslationUnit() {
		return tu;
	}
}
