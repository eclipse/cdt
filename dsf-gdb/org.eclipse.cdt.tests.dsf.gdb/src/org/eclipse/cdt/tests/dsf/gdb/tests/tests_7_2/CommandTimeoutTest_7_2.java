/*******************************************************************************
 * Copyright (c) 2012 Mentor Graphics and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Mentor Graphics - Initial API and implementation
 *******************************************************************************/

package org.eclipse.cdt.tests.dsf.gdb.tests.tests_7_2;

import org.eclipse.cdt.tests.dsf.gdb.framework.BackgroundRunner;
import org.eclipse.cdt.tests.dsf.gdb.tests.CommandTimeoutTest;
import org.eclipse.cdt.tests.dsf.gdb.tests.ITestConstants;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;

@RunWith(BackgroundRunner.class)
public class CommandTimeoutTest_7_2 extends CommandTimeoutTest {
	@BeforeClass
    public static void beforeClassMethod_7_2() {
		setGdbProgramNamesLaunchAttributes(ITestConstants.SUFFIX_GDB_7_2);		
	}
}
