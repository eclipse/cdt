/*******************************************************************************
 * Copyright (c) 2020 Marco Stornelli
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *******************************************************************************/
package org.eclipse.cdt.codan.core.internal.checkers;

import org.eclipse.cdt.codan.core.tests.CheckerTestCase;
import org.eclipse.cdt.codan.internal.checkers.NoDiscardChecker;

/**
 * Test for {@link NoDiscardChecker} class
 */
public class NoDiscardCheckerTest extends CheckerTestCase {

	public static final String ER_ID = NoDiscardChecker.ER_ID;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		enableProblems(ER_ID);
	}

	@Override
	public boolean isCpp() {
		return true;
	}

	//[[nodiscard]] int test() {
	//	return 2;
	//}
	//int main() {
	//	test();
	//	return 0;
	//}
	public void testCppSimpleCall() throws Exception {
		loadCodeAndRun(getAboveComment());
		checkErrorLine(5, ER_ID);
	}

	//[[nodiscard]] int test() {
	//	return 2;
	//}
	//int main() {
	//	return test();
	//}
	public void testCppInReturn() throws Exception {
		loadCodeAndRun(getAboveComment());
		checkNoErrorsOfKind(ER_ID);
	}

	//[[nodiscard]] int test() {
	//	return 2;
	//}
	//int main() {
	//	return test() ? 0 : 1;
	//}
	public void testCppInTernaryReturn() throws Exception {
		loadCodeAndRun(getAboveComment());
		checkNoErrorsOfKind(ER_ID);
	}

	//[[nodiscard]] int test() {
	//	return 2;
	//}
	//void process(int v) {}
	//int main() {
	//	process(test());
	//	return 0;
	//}
	public void testCppInOtherCall() throws Exception {
		loadCodeAndRun(getAboveComment());
		checkNoErrorsOfKind(ER_ID);
	}

	//[[nodiscard]] int test() {
	//	return 2;
	//}
	//void process(int v) {}
	//int main() {
	//	if (test()) {
	//	}
	//	return 0;
	//}
	public void testCppInCondition() throws Exception {
		loadCodeAndRun(getAboveComment());
		checkNoErrorsOfKind(ER_ID);
	}

	//class Test {
	//public:
	//	Test() {}
	//	[[nodiscard]] int foo() {
	//		return 2;
	//	}
	//};
	//
	//int main() {
	//	Test t;
	//	t.foo();
	//	return 0;
	//}
	public void testCppNoDiscardMethod() throws Exception {
		loadCodeAndRun(getAboveComment());
		checkErrorLine(11, ER_ID);
	}

	//int test() __attribute__((warn_unused_result)) {
	//	return 2;
	//}
	//int main() {
	//	test();
	//	return 0;
	//}
	public void testCSimpleCall() throws Exception {
		loadCodeAndRunC(getAboveComment());
		checkErrorLine(5, ER_ID);
	}

	//int test() __attribute__((warn_unused_result))  {
	//	return 2;
	//}
	//int main() {
	//	return test();
	//}
	public void testCInReturn() throws Exception {
		loadCodeAndRunC(getAboveComment());
		checkNoErrorsOfKind(ER_ID);
	}

	//int test() __attribute__((warn_unused_result)) {
	//	return 2;
	//}
	//int main() {
	//	return test() ? 0 : 1;
	//}
	public void testCInTernaryReturn() throws Exception {
		loadCodeAndRunC(getAboveComment());
		checkNoErrorsOfKind(ER_ID);
	}

	//int test() __attribute__((warn_unused_result)) {
	//	return 2;
	//}
	//void process(int v) {}
	//int main() {
	//	process(test());
	//	return 0;
	//}
	public void testCInOtherCall() throws Exception {
		loadCodeAndRunC(getAboveComment());
		checkNoErrorsOfKind(ER_ID);
	}

	//int test() __attribute__((warn_unused_result)) {
	//	return 2;
	//}
	//void process(int v) {}
	//int main() {
	//	if (test()) {
	//	}
	//	return 0;
	//}
	public void testCInCondition() throws Exception {
		loadCodeAndRunC(getAboveComment());
		checkNoErrorsOfKind(ER_ID);
	}

	//class Test {
	//public:
	//	Test() {}
	//	int foo() __attribute__((warn_unused_result)) {
	//		return 2;
	//	}
	//};
	//
	//int main() {
	//	Test t;
	//	t.foo();
	//	return 0;
	//}
	public void testCppUnusedNoDiscardMethod() throws Exception {
		loadCodeAndRun(getAboveComment());
		checkErrorLine(11, ER_ID);
	}

	//[[nodiscard]] int foo() {
	//	return 2;
	//}
	//int main() {
	//	((((foo()))));
	//	return 0;
	//}
	public void testInParenNoDiscardMethod() throws Exception {
		loadCodeAndRun(getAboveComment());
		checkErrorLine(5, ER_ID);
	}

	//[[nodiscard]] int foo() {
	//	return 2;
	//}
	//#define MACRO(X) X()
	//int main() {
	//	MACRO(foo);
	//	return 0;
	//}
	public void testInMacroEnabled() throws Exception {
		loadCodeAndRun(getAboveComment());
		checkErrorLine(6, ER_ID);
	}

	//[[nodiscard]] int foo() {
	//	return 2;
	//}
	//#define MACRO(X) X()
	//int main() {
	//	MACRO(foo);
	//	return 0;
	//}
	public void testInMacroDisabled() throws Exception {
		setPreferenceValue(ER_ID, NoDiscardChecker.PARAM_MACRO_ID, false);
		loadCodeAndRun(getAboveComment());
		checkNoErrorsOfKind(ER_ID);
	}

	//[[nodiscard]] int foo() {
	//	return 2;
	//}
	//int main() {
	//	(void) foo();
	//	return 0;
	//}
	public void testVoidCast() throws Exception {
		loadCodeAndRun(getAboveComment());
		checkNoErrorsOfKind(ER_ID);
	}

	//[[nodiscard]] int foo() {
	//	return 2;
	//}
	//int main() {
	//	static_cast<void>(foo());
	//	return 0;
	//}
	public void testStaticVoidCast() throws Exception {
		loadCodeAndRun(getAboveComment());
		checkNoErrorsOfKind(ER_ID);
	}

	//[[nodiscard]] int foo() {
	//	return 2;
	//}
	//int process() {
	//	return foo(), 2;
	//}
	//int main() {
	//	process();
	//	return 0;
	//}
	public void testCommaLeftHandSide() throws Exception {
		loadCodeAndRun(getAboveComment());
		checkErrorLine(5, ER_ID);
	}

	//[[nodiscard]] int foo() {
	//	return 2;
	//}
	//int process() {
	//	return 2, foo();
	//}
	//int main() {
	//	process();
	//	return 0;
	//}
	public void testCommaRightHandSide() throws Exception {
		loadCodeAndRun(getAboveComment());
		checkNoErrorsOfKind(ER_ID);
	}

	//[[nodiscard]] int foo() {
	//	return 2;
	//}
	//int main() {
	//	int vv = 0;
	//	vv = 2, foo();
	//	return 0;
	//}
	public void testCommaRightHandSide2() throws Exception {
		loadCodeAndRun(getAboveComment());
		checkNoErrorsOfKind(ER_ID);
	}

	//class Test {
	//public:
	//	Test(double v) {}
	//	[[nodiscard]] Test(int v) {}
	//};
	//int main() {
	//	Test(42);
	//	Test(42.0);
	//	return 0;
	//}
	public void testCppNoDiscardCtor() throws Exception {
		loadCodeAndRun(getAboveComment());
		checkErrorLine(7, ER_ID);
	}

	//class Test {
	//public:
	//	Test(double v) {}
	//	[[nodiscard]] Test(int v) {}
	//};
	//int main() {
	//	static_cast<Test>(42);
	//	static_cast<Test>(42.0);
	//	return 0;
	//}
	public void testCppNoDiscardCtorStaticCast() throws Exception {
		loadCodeAndRun(getAboveComment());
		checkErrorLine(7, ER_ID);
	}

	//class Test {
	//public:
	//	Test(double v) {}
	//	[[nodiscard]] Test(int v) {}
	//};
	//int main() {
	//	reinterpret_cast<Test>(42);
	//	(Test) 42;
	//	return 0;
	//}
	public void testCppNoDiscardCtorOtherCast() throws Exception {
		loadCodeAndRun(getAboveComment());
		checkNoErrorsOfKind(ER_ID);
	}
}
