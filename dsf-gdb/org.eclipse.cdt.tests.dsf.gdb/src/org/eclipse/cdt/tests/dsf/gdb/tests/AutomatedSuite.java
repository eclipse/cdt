/*******************************************************************************
 * Copyright (c) 2012, 2015 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Marc Khouzam (Ericsson)	- Initial Implementation
 *******************************************************************************/
package org.eclipse.cdt.tests.dsf.gdb.tests;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * This suite runs all suites that are part of the tests automatically run with
 * each CDT build.
 *
 *
 * This suite runs tests for gdb versions specified by java system variable "cdt.tests.dsf.gdb.versions", i.e.
 * -Dcdt.tests.dsf.gdb.versions=gdb.7.7,gdbserver.7.7,gdb.7.11
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
	SuiteGdb.class,
})
public class AutomatedSuite {
}