/*******************************************************************************
 * Copyright (c) 2005, 2014 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Rational Software - Initial API and implementation
 *     Yuan Zhang / Beth Tibbitts (IBM Research)
 *     Sergey Prigogin (Google)
 ******************************************************************************/
package org.eclipse.cdt.internal.core.dom.parser.c;

import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTNullStatement;
import org.eclipse.cdt.internal.core.dom.parser.ASTAttributeOwner;
import org.eclipse.cdt.internal.core.dom.parser.cpp.ICPPExecution;

/**
 * @author jcamelon
 */
public class CASTNullStatement extends ASTAttributeOwner implements IASTNullStatement {
    @Override
	public boolean accept(ASTVisitor action) {
        if (action.shouldVisitStatements) {
		    switch (action.visit(this)) {
	            case ASTVisitor.PROCESS_ABORT: return false;
	            case ASTVisitor.PROCESS_SKIP: return true;
	            default: break;
	        }
		}

        if (!acceptByAttributeSpecifiers(action)) return false;

        if (action.shouldVisitStatements) {
		    switch (action.leave(this)) {
	            case ASTVisitor.PROCESS_ABORT: return false;
	            case ASTVisitor.PROCESS_SKIP: return true;
	            default: break;
	        }
		}
        return true;
    }
    
    @Override
	public CASTNullStatement copy() {
		return copy(CopyStyle.withoutLocations);
	}

	@Override
	public CASTNullStatement copy(CopyStyle style) {
		CASTNullStatement copy = new CASTNullStatement();
		return copy(copy, style);
	}

	@Override
	public ICPPExecution getExecution() {
		throw new UnsupportedOperationException();
	}
}
