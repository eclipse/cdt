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
/*
 * Created on Jul 11, 2003
 */
package org.eclipse.cdt.internal.core.search.matching;

import java.io.IOException;
import java.util.Iterator;

import org.eclipse.cdt.core.parser.ISourceElementCallbackDelegate;
import org.eclipse.cdt.core.parser.ast.IASTFunction;
import org.eclipse.cdt.core.parser.ast.IASTMethod;
import org.eclipse.cdt.core.parser.ast.IASTParameterDeclaration;
import org.eclipse.cdt.core.parser.ast.IASTQualifiedNameElement;
import org.eclipse.cdt.core.search.ICSearchScope;
import org.eclipse.cdt.internal.core.CharOperation;
import org.eclipse.cdt.internal.core.index.IEntryResult;
import org.eclipse.cdt.internal.core.index.impl.IndexInput;
import org.eclipse.cdt.internal.core.index.impl.IndexedFile;
import org.eclipse.cdt.internal.core.search.IIndexSearchRequestor;
import org.eclipse.cdt.internal.core.search.indexing.AbstractIndexer;

/**
 * @author aniefer
 *
 * To change the template for this generated type comment go to
 * Window>Preferences>Java>Code Generation>Code and Comments
 */
public class MethodDeclarationPattern extends CSearchPattern {

	private SearchFor searchFor;
	
	private char[][] parameterNames;
	private char[]   simpleName;
	private char[][] qualifications;

	private char[]   decodedSimpleName;
	private char[][] decodedQualifications;

	public MethodDeclarationPattern(char[] name, char[][] qual, char [][] params, int matchMode, SearchFor search, LimitTo limitTo, boolean caseSensitive) {
		//super( name, params, matchMode, limitTo, caseSensitive );
		super( matchMode, caseSensitive, limitTo );

		qualifications = qual;
		simpleName = name;
		parameterNames = params;
		
		searchFor = search;
	}


	public int matchLevel(ISourceElementCallbackDelegate node, LimitTo limit ) {
		if( node instanceof IASTMethod ){
			if( searchFor != METHOD || !canAccept( limit ) ){
				return IMPOSSIBLE_MATCH;
			}
		} else if ( node instanceof IASTFunction ){
			if( searchFor != FUNCTION || !canAccept( limit ) ){
				return IMPOSSIBLE_MATCH;
			}
		} else {
			return IMPOSSIBLE_MATCH;
		}

		IASTFunction function = (IASTFunction) node;
		String nodeName = function.getName();

		//check name, if simpleName == null, its treated the same as "*"	
		if( simpleName != null && !matchesName( simpleName, nodeName.toCharArray() ) ){
			return IMPOSSIBLE_MATCH;
		}

		//create char[][] out of full name, 
		String [] fullName = ((IASTQualifiedNameElement) node).getFullyQualifiedName();
		char [][] qualName = new char [ fullName.length - 1 ][];
		for( int i = 0; i < fullName.length - 1; i++ ){
			qualName[i] = fullName[i].toCharArray();
		}
		
		//check containing scopes
		if( !matchQualifications( qualifications, qualName ) ){
			return IMPOSSIBLE_MATCH;
		}
		
		//parameters
		if( parameterNames != null && parameterNames.length > 0  &&	parameterNames[0].length > 0 ){

			Iterator params = function.getParameters();
				
			for( int i = 0; i < parameterNames.length; i++ ){
			
				//if this function doesn't have this many parameters, it is not a match.
				//or if this function has a parameter, but parameterNames only has null.
				if( !params.hasNext() || parameterNames[ i ] == null )
					return IMPOSSIBLE_MATCH;
					
				IASTParameterDeclaration parameter = (IASTParameterDeclaration) params.next();
				char[] param = CSearchPattern.getParamString( parameter );
				
				//no wildcards in parameters strings
				if( !CharOperation.equals( parameterNames[i], param, _caseSensitive ) )
					return IMPOSSIBLE_MATCH;
			}
			
			//if this function still has more parameters, it is not a match
			if( params.hasNext() )
				return IMPOSSIBLE_MATCH;
		}
		
		return ACCURATE_MATCH;
	}
	
	public char[] indexEntryPrefix() {
		if( searchFor == FUNCTION )
			return AbstractIndexer.bestFunctionPrefix( _limitTo, simpleName, _matchMode, _caseSensitive );
		else if( searchFor == METHOD )
			return AbstractIndexer.bestMethodPrefix( _limitTo, simpleName, qualifications, _matchMode, _caseSensitive );
		else return null;
	}
	
	protected void resetIndexInfo(){
		decodedSimpleName = null;
		decodedQualifications = null;
	}
	
	protected void decodeIndexEntry(IEntryResult entryResult) {
		char[] word = entryResult.getWord();
		int size = word.length;
		
		int firstSlash = CharOperation.indexOf( SEPARATOR, word, 0 );
		
		int slash = CharOperation.indexOf( SEPARATOR, word, firstSlash + 1 );
		
		this.decodedSimpleName = CharOperation.subarray(word, firstSlash + 1, slash);
		
		if( slash != -1 && slash+1 < size ){
			char [][] temp = CharOperation.splitOn('/', CharOperation.subarray(word, slash + 1, size));
			this.decodedQualifications = new char [ temp.length ][];
			for( int i = 0; i < temp.length; i++ ){
				this.decodedQualifications[ i ] = temp[ temp.length - i - 1 ];
			}
		}
	}

	protected boolean matchIndexEntry() {
		/* check simple name matches */
		if (simpleName != null){
			if( ! matchesName( simpleName, decodedSimpleName ) ){
				return false; 
			}
		}
		
		if( !matchQualifications( qualifications, decodedQualifications ) ){
			return false;
		}
		
		return true;
	}
	
	public void feedIndexRequestor(IIndexSearchRequestor requestor, int detailLevel, int[] references, IndexInput input, ICSearchScope scope) throws IOException {
		for (int i = 0, max = references.length; i < max; i++) {
			IndexedFile file = input.getIndexedFile(references[i]);
			String path;
			if (file != null && scope.encloses(path =file.getPath())) {
				if( searchFor == METHOD )
					requestor.acceptMethodDeclaration(path, decodedSimpleName, parameterNames.length, decodedQualifications);
				else if ( searchFor == FUNCTION )
					requestor.acceptFunctionDeclaration(path, decodedSimpleName, parameterNames.length);
			}
		}
	}
}
