/*******************************************************************************
 * Copyright (c) 2006, 2013 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.core.dom.lrparser;

import org.eclipse.cdt.core.parser.EndOfFileException;
import org.eclipse.cdt.core.parser.IScanner;
import org.eclipse.cdt.core.parser.OffsetLimitReachedException;

import lpg.lpgjavaruntime.IToken;
import lpg.lpgjavaruntime.Token;

/**
 * Adapts the CPreprocessor from the CDT core for use with LPG based parsers.
 *
 * @author Mike Kucera
 * @noreference
 * TODO move into an internal package
 */
public class CPreprocessorAdapter {
	/**
	 * During content assist the preprocessor may return a completion token
	 * which represents the identifier on which the user invoked content assist.
	 * Then the preprocessor normally returns arbitrarily many end-of-completion
	 * (EOC) tokens.
	 *
	 * A bottom-up parser cannot know ahead of time how many EOC tokens are
	 * needed in order for the parse to complete successfully. So we pick
	 * a number that seems arbitrarily large enough.
	 */
	private static final int NUM_EOC_TOKENS = 50;

	private static final int DUMMY_TOKEN_KIND = 0;
	private static final int tCOMPLETION = org.eclipse.cdt.core.parser.IToken.tCOMPLETION;

	/**
	 * Collect the tokens generated by the preprocessor.
	 * TODO: should preprocessor.nextTokenRaw() be called instead?
	 */
	public static void runCPreprocessor(IScanner preprocessor, ITokenCollector tokenCollector, IDOMTokenMap tokenMap) {
		// LPG requires that the token stream start with a dummy token
		tokenCollector.addToken(createDummyToken());

		org.eclipse.cdt.core.parser.IToken lastToken = null;
		try {
			while (true) {
				// the preprocessor throws EndOfFileException when it reaches the end of input
				org.eclipse.cdt.core.parser.IToken domToken = preprocessor.nextToken();
				processDOMToken(domToken, tokenCollector, tokenMap);
				lastToken = domToken;

				if (domToken.getType() == tCOMPLETION)
					break;
			}
		} catch (OffsetLimitReachedException e) {
			// preprocessor throws this when content assist is invoked inside a preprocessor directive
			org.eclipse.cdt.core.parser.IToken domToken = e.getFinalToken();
			assert domToken.getType() == tCOMPLETION;
			processDOMToken(domToken, tokenCollector, tokenMap);
			lastToken = domToken;
		} catch (EndOfFileException e) {
			// use thrown exception to break out of loop
		}

		// TODO
		// This computation is actually incorrect. The "offset" of the EOF token should
		// be equal to the size of the file. But since the CPreprocessor throws an exception when it
		// reaches the end we can't get this info. So we just use the offset of the last real token
		// that was returned.
		int eofTokenOffset = lastToken == null ? 0 : lastToken.getOffset();

		// LPG requires that the token stream end with an EOF token
		tokenCollector.addToken(createEOFToken(tokenMap, eofTokenOffset));
	}

	private static void processDOMToken(org.eclipse.cdt.core.parser.IToken domToken, ITokenCollector tokenCollector,
			IDOMTokenMap tokenMap) {
		int newKind = tokenMap.mapKind(domToken);
		tokenCollector.addToken(new LPGTokenAdapter(domToken, newKind));

		if (domToken.getType() == tCOMPLETION) {
			int offset = domToken.getOffset();
			for (int i = 0; i < NUM_EOC_TOKENS; i++)
				tokenCollector.addToken(createEOCToken(tokenMap, offset));
		}
	}

	private static IToken createEOCToken(IDOMTokenMap tokenMap, int offset) {
		return new Token(null, offset, offset + 1, tokenMap.getEOCTokenKind());
	}

	private static IToken createDummyToken() {
		return new Token(null, 0, 0, DUMMY_TOKEN_KIND);
	}

	private static IToken createEOFToken(IDOMTokenMap tokenMap, int offset) {
		return new Token(null, offset, offset + 1, tokenMap.getEOFTokenKind());
	}

}
