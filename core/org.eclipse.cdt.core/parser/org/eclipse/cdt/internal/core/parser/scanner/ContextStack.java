/*******************************************************************************
 * Copyright (c) 2003 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials 
 * are made available under the terms of the Common Public License v0.5 
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/cpl-v05.html
 * 
 * Contributors:
 *     IBM Corp. - Rational Software - initial implementation
 ******************************************************************************/

package org.eclipse.cdt.internal.core.parser.scanner;

import java.io.IOException;
import java.io.Reader;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import java.util.Stack;

import org.eclipse.cdt.core.parser.IParserLogService;
import org.eclipse.cdt.core.parser.IProblem;
import org.eclipse.cdt.core.parser.ISourceElementRequestor;
import org.eclipse.cdt.core.parser.ast.IASTInclusion;
import org.eclipse.cdt.internal.core.parser.scanner.IScannerContext.ContextKind;

/**
 * @author aniefer
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class ContextStack {

	private final IParserLogService log;
	private Scanner scanner;
	
	public ContextStack( Scanner s, IParserLogService l ) {
		scanner = s;
		log = l;
	}

    public void updateContext(Reader reader, String filename, ContextKind type, IASTInclusion inclusion, ISourceElementRequestor requestor) throws ContextException {
        updateContext(reader, filename, type, inclusion, requestor, -1, -1);
    }
  
	public void updateContext(Reader reader, String filename, ContextKind type, IASTInclusion inclusion, ISourceElementRequestor requestor, int macroOffset, int macroLength) throws ContextException 
    {
		int startLine = 1;
		
        // If we expand a macro within a macro, then keep offsets of the top-level one,
        // as only the top level macro identifier is properly positioned    
        if (type == IScannerContext.ContextKind.MACROEXPANSION) {
            if (currentContext.getKind() == IScannerContext.ContextKind.MACROEXPANSION) {
                macroOffset = currentContext.getMacroOffset();
                macroLength = currentContext.getMacroLength();
            }
            
			startLine = currentContext.getLine();
        }

		undoStack.clear();
		IScannerContext context = new ScannerContext( reader, filename, type, null, macroOffset, macroLength, startLine );
		context.setExtension(inclusion); 
		push( context, requestor );	
	}
	
	protected void push( IScannerContext context, ISourceElementRequestor requestor ) throws ContextException
	{
		if( context.getKind() == IScannerContext.ContextKind.INCLUSION )
		{
			if( !inclusions.add( context.getFilename() ) )
				throw new ContextException( IProblem.PREPROCESSOR_CIRCULAR_INCLUSION );
			
			log.traceLog( "Scanner::ContextStack: entering inclusion " +context.getFilename());
			context.getExtension().enterScope( requestor );				

		} else if( context.getKind() == IScannerContext.ContextKind.MACROEXPANSION )
		{
			if( !defines.add( context.getFilename() ) )
				throw new ContextException( IProblem.PREPROCESSOR_INVALID_MACRO_DEFN );
		}
		if( currentContext != null )
			contextStack.push(currentContext);
		
		currentContext = context;
		if( context.getKind() == IScannerContext.ContextKind.TOP )
			topContext = context;
	}
	
	public boolean rollbackContext(ISourceElementRequestor requestor) {
		try {
			currentContext.getReader().close();
		} catch (IOException ie) {
			log.traceLog("ContextStack : Error closing reader ");
		}

		if( currentContext.getKind() == IScannerContext.ContextKind.INCLUSION )
		{
			log.traceLog( "Scanner::ContextStack: ending inclusion " +currentContext.getFilename());
			inclusions.remove( currentContext.getFilename() );
			currentContext.getExtension().exitScope( requestor );
		} else if( currentContext.getKind() == IScannerContext.ContextKind.MACROEXPANSION )
		{
			defines.remove( currentContext.getFilename() );
		}
		
		undoStack.addFirst( currentContext );
		
		if (contextStack.isEmpty()) {
			currentContext = null;
			return false;
		}

		currentContext = (ScannerContext) contextStack.pop();
		return true;
	}
	
	public void undoRollback( IScannerContext undoTo, ISourceElementRequestor requestor ) throws ContextException {
		if( currentContext == undoTo ){
			return;
		}
		
		int size = undoStack.size();
		if( size > 0 )
		{
			for( int i = size; i > 0; i-- )
			{
				push( (IScannerContext) undoStack.removeFirst(), requestor );
				
				if( currentContext == undoTo )
					break;
			}	
		}
	}
	
	/**
	 * 
	 * @param symbol
	 * @return boolean, whether or not we should expand this definition
	 * 
	 * 16.3.4-2 If the name of the macro being replaced is found during 
	 * this scan of the replacement list it is not replaced.  Further, if 
	 * any nested replacements encounter the name of the macro being replaced,
	 * it is not replaced. 
	 */
	protected boolean shouldExpandDefinition( String symbol )
	{
		return !defines.contains( symbol );
	}
	
	public IScannerContext getCurrentContext(){
		return currentContext;
	}
	
	private IScannerContext currentContext, topContext;
	private Stack contextStack = new Stack();
	private LinkedList undoStack = new LinkedList();
	private Set inclusions = new HashSet(); 
	private Set defines = new HashSet();
	
	/**
	 * @return
	 */
	public IScannerContext getTopContext() {
		return topContext;
	}

	public IScannerContext getMostRelevantFileContext()
	{
		if( currentContext != null )
		{
			if( currentContext.getKind() == IScannerContext.ContextKind.TOP ) return currentContext;
			if( currentContext.getKind() == IScannerContext.ContextKind.INCLUSION ) return currentContext;
		}
				
		IScannerContext context = null;
		for( int i = contextStack.size() - 1; i >= 0; --i )
		{
			context = (IScannerContext)contextStack.get(i);
			if( context.getKind() == IScannerContext.ContextKind.INCLUSION || context.getKind() == IScannerContext.ContextKind.TOP )
				break;
			if( i == 0 ) context = null;
		}
		
		return context;
	}
	
	public int getCurrentLineNumber()
	{
		return getMostRelevantFileContext() != null ? getMostRelevantFileContext().getLine() : -1;
	}
	
	public int getTopFileLineNumber()
	{
		return topContext.getLine();
	}
	
	public Scanner getScanner()
	{
		return scanner;
	}
}
