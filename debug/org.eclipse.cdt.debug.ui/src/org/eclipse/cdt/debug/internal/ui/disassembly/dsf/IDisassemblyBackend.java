/*******************************************************************************
 * Copyright (c) 2010, 2013 Wind River Systems, Inc. and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *     Freescale Semiconductor - refactoring
 *     Patrick Chuong (Texas Instruments) - Bug 364405
 *     Patrick Chuong (Texas Instruments) - Bug 337851
 *******************************************************************************/
package org.eclipse.cdt.debug.internal.ui.disassembly.dsf;

import java.math.BigInteger;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.source.IAnnotationModel;

/**
 * This is the main interface that connects the DSF Disassembly view to CDI and
 * DSF backends. This interface is obtained through IAdaptable. A new instance
 * is provided every time the adapter is requested. The caller must invoke
 * {@link #dispose()} when it has no further use for the instance.
 *
 * @noimplement This interface is not intended to be implemented by clients.
 *              Clients should extend {@link AbstractDisassemblyBackend}.
 */
public interface IDisassemblyBackend {

	/**
	 * Used to return muliple results from
	 * {@link IDisassemblyBackend#setDebugContext(IAdaptable)}
	 */
	public class SetDebugContextResult {
		/**
		 * The ID of the session associated with the context
		 */
		public String sessionId;

		/**
		 * Whether the context changed to another execution context (the parent
		 * elements of a thread, typically a process)
		 */
		public boolean contextChanged;
	}

	/**
	 * Called after instantiation
	 * @param callback
	 */
	void init(IDisassemblyPartCallback callback);

	/**
	 * Indicates whether this backend support the provided debug context,
	 *
	 * @param context
	 *            the debug context
	 * @return true if it is supported. Caller should invoke
	 *         {@link #setDebugContext(IAdaptable)} with [context] only after
	 *         first checking with this method
	 */
	boolean supportsDebugContext(IAdaptable context);

	/**
	 * @return whether the backend has a debug context
	 */
	boolean hasDebugContext();

	/**
	 * Called by the view when there has been a change to the active debug
	 * context. Should be called only if
	 * {@link #supportsDebugContext(IAdaptable)} first returns true.
	 *
	 * @param context
	 *            the active debug context; must not be null
	 * @return information about the new context
	 */
	SetDebugContextResult setDebugContext(IAdaptable context);

	/**
	 * Clear any knowledge of the current debug context.
	 */
	void clearDebugContext();

	/**
	 * The implementation should end up calling DisassemblyPart.gotoFrame() if
	 * targetFrame > 0, or DisassemblyPart.gotoPC() otherwise.
	 *
	 * @param targetFrame
	 *            the frame level to retrieve. Level 0 is the topmost frame
	 *            (where the PC is)
	 */
	void retrieveFrameAddress(int targetFrame);

	/**
	 * Get the frame of the current context
	 *
	 * @return the frame's level; 0 is the topmost frame (i.e., where the PC
	 *         is). -1 if no frame context has been set.
	 */
	int getFrameLevel();

	/**
	 * Indicates whether the current context is suspended.
	 */
	boolean isSuspended();

	/**
	 * Indicates whether the current context is a frame.
	 */
	boolean hasFrameContext();

	/**
	 * Returns the file associated with the current frame context, or null if
	 * the current context is not a frame or if the frame has no file
	 * association.
	 */
	String getFrameFile();

	/**
	 * Returns the line number associated with the current frame context, or -1
	 * if current context is not a frame or if the frame has no file and line
	 * association.
	 */
	int getFrameLine();

	/**
	 * Retrieves disassembly based on either (a) start and end address range, or
	 * (b) file, line number, and line count. If the caller specifies both sets
	 * of information, the implementation should honor (b) and ignore (a).
	 *
	 * @param startAddress
	 * @param endAddress
	 * @param file
	 * @param lineNumber
	 * @param lines
	 * @param mixed
	 * @param showSymbols
	 * @param showDisassembly
	 * @param linesHint
	 */
	void retrieveDisassembly(BigInteger startAddress, BigInteger endAddress, String file, int lineNumber, int lines,
			boolean mixed, boolean showSymbols, boolean showDisassembly, int linesHint);

	Object insertSource(Position pos, BigInteger address, final String file, int lineNumber);

	void gotoSymbol(String symbol);

	/**
	 * Retrieves disassembly of the code generated by a source file, starting at
	 * the first line. Caller specifies the maximum number of disassembly lines
	 * that should result and a maximum address.
	 *
	 * @param file
	 * @param lines
	 * @param endAddress
	 * @param mixed
	 * @param showSymbols
	 * @param showDisassembly
	 */
	void retrieveDisassembly(String file, int lines, BigInteger endAddress, boolean mixed, boolean showSymbols,
			boolean showDisassembly);

	/**
	 * Evaluate an expression for text hover
	 *
	 * @param expression
	 *            the expression to be evaluated
	 * @return the result, or "" if it doesn't resolve, for whatever reason
	 */
	String evaluateExpression(String expression);

	/**
	 * Evaluate a register for text hover
	 *
	 * @param register  The register to be evaluated
	 * @return The result, or "" if it doesn't resolve, for whatever reason
	 * @since 7.2
	 */
	String evaluateRegister(String register);

	/**
	 * Evaluate a position for text hover
	 *
	 * @param pos  Disassembly position to evaluate.
	 * @param ident  The string found at the given position.
	 * @return the result, or "" if it doesn't resolve, for whatever reason
	 * @since 7.2
	 */
	String getHoverInfoData(AddressRangePosition pos, String ident);

	/**
	 * Called when the Disassembly view has no further use for this backend.
	 */
	void dispose();

	/**
	 * Update the extended PC annotation.
	 */
	void updateExtendedPCAnnotation(IAnnotationModel model);

	/**
	 * This is a test for the current context.
	 *
	 * @return true if backend can perform disassemble, otherwise false
	 */
	boolean canDisassemble();

	/**
	 * Returns the last known address, this API will be call if the selected debug context is not a stackframe.
	 *
	 * @return the last know address, -1 if unknown
	 */
	BigInteger getLastKnownAddress();
}
