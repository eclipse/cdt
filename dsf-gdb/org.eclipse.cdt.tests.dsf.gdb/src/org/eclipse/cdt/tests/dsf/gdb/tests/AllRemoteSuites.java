/*******************************************************************************
 * Copyright (c) 2016 QNX Software System and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Elena Laskavaia (QNX Software System) - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.tests.dsf.gdb.tests;

import org.junit.runner.RunWith;
import org.junit.runners.AllTests;

import junit.framework.JUnit4TestAdapter;
import junit.framework.TestSuite;

/**
 * This suite runs remote tests for all gdb versions (it will ignore setting of "cdt.tests.dsf.gdb.versions", if you
 * want run tests controlled by this var run {@link AutomatedSuite}
 */
@RunWith(AllTests.class)
public class AllRemoteSuites {
	public static junit.framework.Test suite() {
		String gdbVersions = "gdbserver."+String.join(",gdbserver.", ITestConstants.ALL_SUPPORTED_VERSIONS);
		System.setProperty("cdt.tests.dsf.gdb.versions", gdbVersions);
		TestSuite suite = new TestSuite();
		suite.addTest(new JUnit4TestAdapter(SuiteGdb.class));
		return suite;
	}
}
