/*******************************************************************************
 * Copyright (c) 2010, 2014 Wind River Systems, Inc. and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Markus Schorn - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.internal.core.dom.parser.cpp;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTName;

/**
 * Implementation for captures.
 */
public class CPPASTCapture extends CPPASTCaptureBase {
	private IASTName fIdentifier;

	public CPPASTCapture() {
	}

	@Override
	public CPPASTCapture copy() {
		return copy(CopyStyle.withoutLocations);
	}

	@Override
	public CPPASTCapture copy(CopyStyle style) {
		final CPPASTCapture copy = new CPPASTCapture();
		copy.setIdentifier(fIdentifier == null ? null : fIdentifier.copy(style));
		return copy(copy, style);
	}

	@Override
	public boolean capturesThisPointer() {
		return fIdentifier == null;
	}

	@Override
	public IASTName getIdentifier() {
		return fIdentifier;
	}

	@Override
	public boolean accept(ASTVisitor visitor) {
		if (visitor.shouldVisitCaptures) {
			switch (visitor.visit(this)) {
			case ASTVisitor.PROCESS_ABORT:
				return false;
			case ASTVisitor.PROCESS_SKIP:
				return true;
			default:
				break;
			}
		}

		if (fIdentifier != null && !fIdentifier.accept(visitor))
			return false;

		if (visitor.shouldVisitCaptures && visitor.leave(this) == ASTVisitor.PROCESS_ABORT)
			return false;

		return true;
	}

	@Override
	public void setIdentifier(IASTName identifier) {
		assertNotFrozen();
		if (identifier != null) {
			identifier.setParent(this);
			identifier.setPropertyInParent(IDENTIFIER);
		}
		fIdentifier= identifier;
	}

	@Override
	public int getRoleForName(IASTName name) {
		if (name == fIdentifier) {
			return r_reference;
		}
		return r_unclear;
	}
}
