/*******************************************************************************
 * Copyright (c) 2016, 2017 Kichwa Coders Ltd (https://kichwacoders.com/) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.eclipse.cdt.linkerscript.tests

import com.itemis.xtext.testing.XtextTest
import org.eclipse.cdt.linkerscript.LinkerScriptConverters
import org.eclipse.xtext.junit4.InjectWith
import org.eclipse.xtext.junit4.XtextRunner
import org.junit.Test
import org.junit.runner.RunWith

import static org.junit.Assert.*

@RunWith(XtextRunner)
@InjectWith(LinkerScriptInjectorProvider)
class NumbersTest extends XtextTest {

	def void testNumber(long expected, String input) {
		val actual = LinkerScriptConverters::numberToValue(input)
		assertEquals(expected, actual)
		testParserRule(input, "Number")
	}

	def void assertNumberToValueFails(String input) {
		try {
			LinkerScriptConverters::numberToValue(input)
			fail("Expected a NumberFormatException")
		} catch (NumberFormatException e) {
			// pass
		}

		// as we are primarily interested in testing numberToValue against
		// things the lexer will deliver us, make sure the input is indeed
		// understood as a number
		testParserRule(input, "Number")
	}

	@Test
	def void testDecimal() {
		testNumber(0, "0")
		testNumber(123, "123")
		testNumber(123, "123d")
		testNumber(123, "123D")

		testNumber(123 * 1024, "123k")
		testNumber(123 * 1024, "123K")
		testNumber(123 * 1024 * 1024, "123m")
		testNumber(123 * 1024 * 1024, "123M")

		/* limits / boundaries */
		testNumber(0xffffffffffffffff#L, "18446744073709551615")
		assertNumberToValueFails("18446744073709551616")
		testNumber(0xffffffff#L, "4294967295")
		testNumber(0x100000000#L, "4294967296")

		testNumber(0xfffffffffffffc00#L, "18014398509481983k")
		assertNumberToValueFails("18014398509481984k")
		testNumber(0xfffffffffff00000#L, "17592186044415m")
		assertNumberToValueFails("17592186044416m")

		/* invalid char for base */
		assertNumberToValueFails("ad")
		assertNumberToValueFails("fd")

		/* can't have two suffixes */
		testParserRuleErrors("123dk", "Number");
		testParserRuleErrors("123dm", "Number");
	}

	@Test
	def void testHexadecimal() {
		testNumber(0x0, "0x0")
		testNumber(0xafd, "0xafd")
		testNumber(0X0, "0X0")
		testNumber(0XFD, "0XFD")
		testNumber(0x123, "123x")
		testNumber(0x123, "123X")

		// tricky: a123x looks like identifier, but is a number
		testNumber(0xa123, "a123x")
		testNumber(0xA123, "A123X")

		testNumber(0xafd * 1024, "0xafdk")
		testNumber(0xafd * 1024, "0xafdK")
		// mind overflow, need to ensure long
		testNumber(0xafd#L * 1024 * 1024, "0xafdm")
		testNumber(0xafd#L * 1024 * 1024, "0xafdM")

		/* limits / boundaries */
		testNumber(0xffffffffffffffff#L, "0xffffffffffffffff")
		assertNumberToValueFails("0x10000000000000000")
		testNumber(0xffffffff#L, "0xffffffff")
		testNumber(0x100000000#L, "0x100000000")

		testNumber(0xfffffffffffffc00#L, "0x3fffffffffffffk")
		assertNumberToValueFails("0x40000000000000k")
		testNumber(0xfffffffffff00000#L, "0xfffffffffffm")
		assertNumberToValueFails("0x100000000000m")

		/* can't have two suffixes */
		testParserRuleErrors("123xk", "Number");
		testParserRuleErrors("123xm", "Number");

		/* can't have prefix and hex suffixes */
		testParserRuleErrors("0x123x", "Number");
	}

	@Test
	def void testOctal() {
		testNumber(0, "0o")
		testNumber(0123, "123o")
		testNumber(0123, "123O")

		/* limits / boundaries */
		testNumber(0xffffffffffffffff#L, "1777777777777777777777o")
		assertNumberToValueFails("2000000000000000000000o")
		testNumber(0xffffffff#L, "37777777777o")
		testNumber(0x100000000#L, "40000000000o")

		/* invalid char for base */
		assertNumberToValueFails("8o")
		assertNumberToValueFails("ao")

		/* can't have two suffixes */
		testParserRuleErrors("123ok", "Number");
		testParserRuleErrors("123om", "Number");
		testParserRuleErrors("123Ok", "Number");
		testParserRuleErrors("123Om", "Number");
	}

	@Test
	def void testBinary() {
		testNumber(0, "0b")
		testNumber(Long.parseUnsignedLong("1101", 2), "1101b")
		testNumber(Long.parseUnsignedLong("1101", 2), "1101B")

		/* limits / boundaries */
		testNumber(0xffffffffffffffff#L, "1111111111111111111111111111111111111111111111111111111111111111b")
		assertNumberToValueFails("10000000000000000000000000000000000000000000000000000000000000000b")
		testNumber(0xffffffff#L, "11111111111111111111111111111111b")
		testNumber(0x100000000#L, "100000000000000000000000000000000b")

		/* invalid char for base */
		assertNumberToValueFails("2b")
		assertNumberToValueFails("ab")

		/* can't have two suffixes */
		testParserRuleErrors("1101bk", "Number");
		testParserRuleErrors("1101bm", "Number");
		testParserRuleErrors("1101Bk", "Number");
		testParserRuleErrors("1101Bm", "Number");
	}

}
