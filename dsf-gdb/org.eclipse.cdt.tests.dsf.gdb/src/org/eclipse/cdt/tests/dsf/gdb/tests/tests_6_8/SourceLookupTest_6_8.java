/*******************************************************************************
 * Copyright (c) 2015 Kichwa Coders and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    Jonah Graham (Kichwa Coders) - initial API and implementation to Add support for gdb's "set substitute-path" (Bug 472765)
 *******************************************************************************/
package org.eclipse.cdt.tests.dsf.gdb.tests.tests_6_8;

import org.eclipse.cdt.tests.dsf.gdb.framework.BackgroundRunner;
import org.eclipse.cdt.tests.dsf.gdb.tests.ITestConstants;
import org.eclipse.cdt.tests.dsf.gdb.tests.tests_6_6.SourceLookupTest_6_6;
import org.eclipse.cdt.tests.dsf.gdb.tests.tests_6_7.SourceLookupTest_6_7;
import org.junit.Test;
import org.junit.runner.RunWith;

@RunWith(BackgroundRunner.class)
public class SourceLookupTest_6_8 extends SourceLookupTest_6_7 {
	@Override
	protected void setGdbVersion() {
		setGdbProgramNamesLaunchAttributes(ITestConstants.SUFFIX_GDB_6_8);
	}

	/**
	 * @see SourceLookupTest_6_6#sourceSubstituteAN()
	 */
	@Test
	@Override
	public void sourceSubstituteAN() throws Throwable {
		super.sourceSubstituteAN();
	}

	/**
	 * @see SourceLookupTest_6_6#sourceSubstituteAN()
	 */
	@Test
	@Override
	public void sourceSubstituteBreakpointsAN() throws Throwable {
		super.sourceSubstituteAN();
	}
}
