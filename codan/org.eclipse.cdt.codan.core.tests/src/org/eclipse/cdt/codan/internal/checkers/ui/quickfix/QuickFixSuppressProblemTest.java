/*******************************************************************************
 * Copyright (c) 2017 Institute for Software.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Felix Morgner <fmorgner@hsr.ch> - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.codan.internal.checkers.ui.quickfix;

import org.eclipse.cdt.codan.ui.AbstractCodanCMarkerResolution;

public class QuickFixSuppressProblemTest extends QuickFixTestCase {
	@SuppressWarnings("restriction")
	@Override
	protected AbstractCodanCMarkerResolution createQuickFix() {
		return new QuickFixSuppressProblem();
	}

	//struct s {};
	//void func() {
	//	try {
	//	} catch (s e) {
	//	}
	//}
	public void testCPPMarkerOnNode_495842() throws Exception {
		loadcode(getAboveComment(), true);
		String result = runQuickFixOneFile();
		assertContainedIn("} catch (s e) { // @suppress(\"Catching by reference is recommended\")", result);
	}

	//void func() {
	//	int n = 42;
	//
	//	switch (n) {
	//	case 1:
	//		n = 32;
	//	default:
	//		break;
	//	}
	//}
	public void testCPPMarkerNotOnNode_495842() throws Exception {
		loadcode(getAboveComment(), true);
		String result = runQuickFixOneFile();
		assertContainedIn("n = 32; // @suppress(\"No break at end of case\")", result);
	}

	//int func() { }
	public void testCMarker_495842() throws Exception {
		loadcode(getAboveComment(), false);
		String result = runQuickFixOneFile();
		assertContainedIn("int func() { } // @suppress(\"No return\")", result);
	}
}
