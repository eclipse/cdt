/*******************************************************************************
 * Copyright (c) 2019 Marco Stornelli
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 *******************************************************************************/
package org.eclipse.cdt.codan.internal.checkers;

import java.util.Stack;

import org.eclipse.cdt.codan.core.cxx.model.AbstractIndexAstChecker;
import org.eclipse.cdt.core.dom.ast.ASTVisitor;
import org.eclipse.cdt.core.dom.ast.IASTDeclaration;
import org.eclipse.cdt.core.dom.ast.IASTExpression;
import org.eclipse.cdt.core.dom.ast.IASTFunctionCallExpression;
import org.eclipse.cdt.core.dom.ast.IASTIdExpression;
import org.eclipse.cdt.core.dom.ast.IASTName;
import org.eclipse.cdt.core.dom.ast.IASTTranslationUnit;
import org.eclipse.cdt.core.dom.ast.IBinding;
import org.eclipse.cdt.core.dom.ast.ICompositeType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTConstructorChainInitializer;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPASTFunctionDefinition;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPConstructor;
import org.eclipse.cdt.core.dom.ast.cpp.ICPPMethod;
import org.eclipse.cdt.core.dom.ast.cpp.SemanticQueries;
import org.eclipse.cdt.internal.core.dom.parser.cpp.ICPPDeferredClassInstance;

@SuppressWarnings("restriction")
public class VirtualMethodCallChecker extends AbstractIndexAstChecker {
	public static final String ERR_ID = "org.eclipse.cdt.codan.internal.checkers.VirtualMethodCallProblem"; //$NON-NLS-1$

	@Override
	public void processAst(IASTTranslationUnit ast) {
		ast.accept(new OnEachClass());
	}

	class OnEachClass extends ASTVisitor {
		// NOTE: Classes can be nested and even can be declared in constructors of the other classes
		private final Stack<Boolean> ctorDtorStack = new Stack<>();

		OnEachClass() {
			shouldVisitDeclarations = true;
			shouldVisitExpressions = true;
			shouldVisitNames = true;
		}

		@Override
		public int visit(IASTDeclaration declaration) {
			ICPPConstructor constructor = getConstructor(declaration);
			if (constructor != null) {
				ctorDtorStack.push(Boolean.TRUE);
			} else {
				ICPPMethod destructor = getDestructor(declaration);
				if (destructor != null) {
					ctorDtorStack.push(Boolean.TRUE);
				}
			}
			return PROCESS_CONTINUE;
		}

		@Override
		public int leave(IASTDeclaration declaration) {
			if (getConstructor(declaration) != null || getDestructor(declaration) != null) {
				ctorDtorStack.pop();
			}
			return PROCESS_CONTINUE;
		}

		@Override
		public int visit(IASTExpression expression) {
			if (!ctorDtorStack.empty() && expression instanceof IASTFunctionCallExpression) {
				IASTFunctionCallExpression fCall = (IASTFunctionCallExpression) expression;
				IASTExpression fNameExp = fCall.getFunctionNameExpression();
				IBinding fBinding = null;
				if (fNameExp instanceof IASTIdExpression) {
					IASTIdExpression fName = (IASTIdExpression) fNameExp;
					fBinding = fName.getName().resolveBinding();
				}
				if (fBinding != null && fBinding instanceof ICPPMethod) {
					ICPPMethod method = (ICPPMethod) fBinding;
					if (method.isVirtual() || method.isPureVirtual()) {
						reportProblem(ERR_ID, expression);
					}
				}
			}
			return PROCESS_CONTINUE;
		}

		/** Checks that specified declaration is a class constructor
		 *  (it is a class member and its name is equal to the class name)
		 */
		private ICPPConstructor getConstructor(IASTDeclaration decl) {
			if (decl instanceof ICPPASTFunctionDefinition) {
				ICPPASTFunctionDefinition functionDefinition = (ICPPASTFunctionDefinition) decl;
				if (functionDefinition.isDeleted())
					return null;
				IBinding binding = functionDefinition.getDeclarator().getName().resolveBinding();
				if (binding instanceof ICPPConstructor) {
					ICPPConstructor constructor = (ICPPConstructor) binding;
					// Skip defaulted copy and move constructors.
					if (functionDefinition.isDefaulted() && SemanticQueries.isCopyOrMoveConstructor(constructor))
						return null;
					if (constructor.getClassOwner().getKey() == ICompositeType.k_union)
						return null;
					// Skip delegating constructors.
					for (ICPPASTConstructorChainInitializer memberInitializer : functionDefinition
							.getMemberInitializers()) {
						IASTName memberName = memberInitializer.getMemberInitializerId();
						if (memberName != null) {
							IBinding memberBinding = memberName.resolveBinding();
							ICPPClassType classType = null;
							if (memberBinding instanceof ICPPClassType) {
								classType = (ICPPClassType) memberBinding;
							} else if (memberBinding instanceof ICPPConstructor) {
								classType = ((ICPPConstructor) memberBinding).getClassOwner();
							}
							if (classType instanceof ICPPDeferredClassInstance) {
								classType = ((ICPPDeferredClassInstance) classType).getClassTemplate();
							}
							if (classType != null && classType.isSameType(constructor.getClassOwner()))
								return null;
						}
					}
					return constructor;
				}
			}

			return null;
		}

		/** Checks that specified declaration is a class constructor
		 *  (it is a class member and its name is equal to the class name)
		 */
		private ICPPMethod getDestructor(IASTDeclaration decl) {
			if (decl instanceof ICPPASTFunctionDefinition) {
				ICPPASTFunctionDefinition functionDefinition = (ICPPASTFunctionDefinition) decl;
				if (functionDefinition.isDeleted())
					return null;
				IBinding binding = functionDefinition.getDeclarator().getName().resolveBinding();
				if (binding instanceof ICPPMethod) {
					ICPPMethod method = (ICPPMethod) binding;
					if (method.isDestructor())
						return method;
				}
			}
			return null;
		}
	}
}
