/*******************************************************************************
 * Copyright (c) 2005, 2011 Intel Corporation and others.
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
 * Miwako Tokugawa (Intel Corporation) - bug 222817 (OptionCategoryApplicability)
 *******************************************************************************/
package org.eclipse.cdt.managedbuilder.internal.enablement;

import org.eclipse.cdt.managedbuilder.core.IHoldsOptions;
import org.eclipse.cdt.managedbuilder.core.IManagedConfigElement;
import org.eclipse.cdt.managedbuilder.core.IOption;
import org.eclipse.cdt.managedbuilder.core.IOptionCategory;
import org.eclipse.cdt.managedbuilder.core.IResourceInfo;

public class NotExpression extends AndExpression {
	public static final String NAME = "not"; //$NON-NLS-1$

	public NotExpression(IManagedConfigElement element) {
		super(element);
	}

	@Override
	public boolean evaluate(IResourceInfo rcInfo, IHoldsOptions holder, IOption option) {
		return !super.evaluate(rcInfo, holder, option);
	}

	@Override
	public boolean evaluate(IResourceInfo rcInfo, IHoldsOptions holder, IOptionCategory category) {
		return !super.evaluate(rcInfo, holder, category);
	}

}
