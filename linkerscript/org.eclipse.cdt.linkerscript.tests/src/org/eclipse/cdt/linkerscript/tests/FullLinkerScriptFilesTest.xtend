/*******************************************************************************
 * Copyright (c) 2016, 2017 Kichwa Coders Ltd (https://kichwacoders.com/) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cdt.linkerscript.tests

import com.google.inject.Inject
import java.lang.ProcessBuilder.Redirect
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.util.concurrent.TimeUnit
import org.apache.commons.io.IOUtils
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.util.ResourceHelper
import org.junit.Ignore
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.Parameterized
import org.junit.runners.Parameterized.Parameter
import org.junit.runners.Parameterized.Parameters
import org.junit.runners.Parameterized.UseParametersRunnerFactory

import static org.hamcrest.Matchers.*
import static org.junit.Assert.*

@RunWith(Parameterized)
@UseParametersRunnerFactory(XtextRunnerParameterizedFactory)
@InjectWith(LinkerScriptInjectorProvider)
class FullLinkerScriptFilesTest {

	@Parameters(name="{0}")
	def static Iterable<? extends Object> data() {
		return Files.newDirectoryStream(Paths.get("files/"), "*.ld");
	}

	@Parameter(0)
	public Path input;

	@Inject
	ResourceHelper resourceHelper

	@Test
	def loadsWithoutDiagnostics() {
		val str = new String(Files.readAllBytes(input), StandardCharsets.UTF_8)
		val resource = resourceHelper.resource(str)
		assertNotNull(resource)
		assertThat(resource.errors, is(empty()))
		assertThat(resource.warnings, is(empty()))
	}

	/*
	 * This test is used to verify that the linker script under test is valid according the ld (using gcc)
	 */
	@Ignore("Not a test of LinkerScript, but instead a test of the tests")
	@Test
	def gccLinksOkay() {
		val pb = new ProcessBuilder()
		pb.directory(input.parent.toFile)
		// The -Wl,--build-id=none is to disable build id encoding in the output, the reason
		// we do that here is we only care about syntax correctness check of gnu ld
		// and some linker scripts don't place the build id anywhere (the build id is in
		// loadable section .note.gnu.build-id)
		pb.command("gcc", "empty.c", "-nostdlib", "-T", input.fileName.toString, "-Wl,--build-id=none")
		pb.redirectOutput(Redirect.PIPE)
		pb.redirectErrorStream(true)
		val process = pb.start()
		assertTrue(process.waitFor(1, TimeUnit.SECONDS))
		val output = IOUtils.toString(process.inputStream, null);
		assertThat(output, isEmptyString())
		assertEquals(process.exitValue, 0)
	}

}
