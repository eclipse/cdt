/*******************************************************************************
 * Copyright (c) 2007, 2010 Wind River Systems, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Markus Schorn - initial API and implementation
 *******************************************************************************/ 

package org.eclipse.cdt.internal.core.model.ext;

import org.eclipse.cdt.core.dom.ast.IFunction;
import org.eclipse.cdt.core.model.ICElement;

public class FunctionHandle extends FunctionDeclarationHandle implements org.eclipse.cdt.core.model.IFunction {

	public FunctionHandle(ICElement parent, IFunction func) {
		super(parent, ICElement.C_FUNCTION, func);
	}
}
