/*******************************************************************************
 * Copyright (c) 2019 Marco Stornelli
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.cdt.ui.tests.refactoring.overridemethods;

import java.util.List;
import java.util.Map;

import org.eclipse.cdt.core.dom.ast.cpp.ICPPClassType;
import org.eclipse.cdt.internal.ui.refactoring.CRefactoring;
import org.eclipse.cdt.internal.ui.refactoring.overridemethods.Method;
import org.eclipse.cdt.internal.ui.refactoring.overridemethods.OverrideMethodsRefactoring;
import org.eclipse.cdt.ui.tests.refactoring.RefactoringTestBase;

import junit.framework.Test;

/**
 * Tests for override methods
 */
public class OverrideMethodsRefactoringTest extends RefactoringTestBase {

	private String[] selectedMethods;
	private OverrideMethodsRefactoring refactoring;
	private boolean addOverride = false;
	private boolean ignoreVirtual = false;

	public OverrideMethodsRefactoringTest() {
		super();
	}

	public OverrideMethodsRefactoringTest(String name) {
		super(name);
	}

	public static Test suite() {
		return suite(OverrideMethodsRefactoringTest.class);
	}

	@Override
	protected CRefactoring createRefactoring() {
		refactoring = new OverrideMethodsRefactoring(getSelectedTranslationUnit(), getSelection(), getCProject());
		return refactoring;
	}

	@Override
	protected void simulateUserInput() {
		if (selectedMethods != null) {
			Map<ICPPClassType, List<Method>> map = refactoring.getMethodContainer().getInitialInput();
			for (Map.Entry<ICPPClassType, List<Method>> entry : map.entrySet()) {
				List<Method> methods = entry.getValue();
				for (Method m : methods) {
					for (String name : selectedMethods) {
						if (m.toString().equals(name))
							refactoring.getPrintData().addMethod(m);
					}
				}
			}
		}
		refactoring.getOptions().setAddOverride(addOverride);
		refactoring.getOptions().setIgnoreVirtual(ignoreVirtual);
	}

	//A.h
	//class Base {
	//public:
	//	virtual ~Base();
	//	virtual void baseFunc() const = 0;
	//};
	//class X: public Base {
	//public:
	//	X();
	//	/*$*//*$$*/
	//};
	//====================
	//class Base {
	//public:
	//	virtual ~Base();
	//	virtual void baseFunc() const = 0;
	//};
	//class X: public Base {
	//public:
	//	X();
	//	virtual void baseFunc() const;
	//};
	//
	//inline void X::baseFunc() const {
	//}
	public void testWithHeaderOnly() throws Exception {
		selectedMethods = new String[] { "virtual void baseFunc() const=0;" };
		assertRefactoringSuccess();
	}

	//A.h
	//class Base {
	//public:
	//	virtual ~Base();
	//	virtual void baseFunc() const = 0;
	//};
	//class X: public Base {
	//public:
	//	X();
	//	/*$*//*$$*/
	//};
	//====================
	//class Base {
	//public:
	//	virtual ~Base();
	//	virtual void baseFunc() const = 0;
	//};
	//class X: public Base {
	//public:
	//	X();
	//	virtual void baseFunc() const;
	//};

	//A.cpp
	//#include "A.h"
	//====================
	//#include "A.h"
	//
	//void X::baseFunc() const {
	//}
	public void testWithHeaderAndSource() throws Exception {
		selectedMethods = new String[] { "virtual void baseFunc() const=0;" };
		assertRefactoringSuccess();
	}

	//A.h
	//namespace FIRST {
	//class Base {
	//public:
	//	virtual ~Base();
	//	virtual void baseFunc(Base* ptr) const = 0;
	//};
	//};
	//namespace SECOND {
	//class X: public FIRST::Base {
	//public:
	//	X();
	//	/*$*//*$$*/
	//};
	//};
	//====================
	//namespace FIRST {
	//class Base {
	//public:
	//	virtual ~Base();
	//	virtual void baseFunc(Base* ptr) const = 0;
	//};
	//};
	//namespace SECOND {
	//class X: public FIRST::Base {
	//public:
	//	X();
	//	virtual void baseFunc(FIRST::Base* ptr) const;
	//};
	//};

	//A.cpp
	//#include "A.h"
	//====================
	//#include "A.h"
	//
	//void SECOND::X::baseFunc(FIRST::Base* ptr) const {
	//}
	public void testWithMixedNamespaceHeaderAndSource() throws Exception {
		selectedMethods = new String[] { "virtual void baseFunc(FIRST::Base * ptr) const=0;" };
		assertRefactoringSuccess();
	}

	//A.h
	//class Base {
	//public:
	//	virtual ~Base();
	//	virtual void baseFunc() const = 0;
	//};
	//class X: public Base {
	//public:
	//	X();
	//	/*$*//*$$*/
	//};
	//====================
	//class Base {
	//public:
	//	virtual ~Base();
	//	virtual void baseFunc() const = 0;
	//};
	//class X: public Base {
	//public:
	//	X();
	//	void baseFunc() const;
	//};
	//
	//inline void X::baseFunc() const {
	//}
	public void testIgnoringVirtual() throws Exception {
		ignoreVirtual = true;
		selectedMethods = new String[] { "virtual void baseFunc() const=0;" };
		assertRefactoringSuccess();
	}

	//A.h
	//class Base {
	//public:
	//	virtual ~Base();
	//	virtual void baseFunc() const = 0;
	//};
	//class X: public Base {
	//public:
	//	X();
	//	/*$*//*$$*/
	//};
	//====================
	//class Base {
	//public:
	//	virtual ~Base();
	//	virtual void baseFunc() const = 0;
	//};
	//class X: public Base {
	//public:
	//	X();
	//	virtual void baseFunc() const override;
	//};
	//
	//inline void X::baseFunc() const {
	//}
	public void testAddingOverrideVirtual() throws Exception {
		addOverride = true;
		selectedMethods = new String[] { "virtual void baseFunc() const=0;" };
		assertRefactoringSuccess();
	}

	//A.h
	//class Base {
	//public:
	//	virtual ~Base();
	//	virtual void baseFunc() const = 0;
	//};
	//template<class T>
	//class X: public Base {
	//public:
	//	X();
	//	/*$*//*$$*/
	//};
	//====================
	//class Base {
	//public:
	//	virtual ~Base();
	//	virtual void baseFunc() const = 0;
	//};
	//template<class T>
	//class X: public Base {
	//public:
	//	X();
	//	virtual void baseFunc() const;
	//};
	//
	//template<class T>
	//inline void X<T>::baseFunc() const {
	//}
	public void testWithTemplateClassl() throws Exception {
		selectedMethods = new String[] { "virtual void baseFunc() const=0;" };
		assertRefactoringSuccess();
	}
}
