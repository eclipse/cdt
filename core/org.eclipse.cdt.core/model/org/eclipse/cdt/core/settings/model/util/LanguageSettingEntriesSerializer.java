/*******************************************************************************
 * Copyright (c) 2007 Intel Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.core.settings.model.util;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.cdt.core.settings.model.CIncludeFileEntry;
import org.eclipse.cdt.core.settings.model.CIncludePathEntry;
import org.eclipse.cdt.core.settings.model.CLibraryFileEntry;
import org.eclipse.cdt.core.settings.model.CLibraryPathEntry;
import org.eclipse.cdt.core.settings.model.CMacroEntry;
import org.eclipse.cdt.core.settings.model.CMacroFileEntry;
import org.eclipse.cdt.core.settings.model.COutputEntry;
import org.eclipse.cdt.core.settings.model.CSourceEntry;
import org.eclipse.cdt.core.settings.model.ICExclusionPatternPathEntry;
import org.eclipse.cdt.core.settings.model.ICLanguageSettingEntry;
import org.eclipse.cdt.core.settings.model.ICSettingEntry;
import org.eclipse.cdt.core.settings.model.ICStorageElement;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

public class LanguageSettingEntriesSerializer {
	public static final String ELEMENT_ENTRY = "entry"; //$NON-NLS-1$
	public static final String ATTRIBUTE_KIND = "kind"; //$NON-NLS-1$
	public static final String ATTRIBUTE_NAME = "name"; //$NON-NLS-1$
	public static final String ATTRIBUTE_VALUE = "value"; //$NON-NLS-1$
	public static final String ATTRIBUTE_FLAGS = "flags"; //$NON-NLS-1$
	public static final String ATTRIBUTE_EXCLUDING = "excluding"; //$NON-NLS-1$
	
//	public static final String ATTRIBUTE_FULL_PATH = "fullPath"; //$NON-NLS-1$
//	public static final String ATTRIBUTE_LOCATION = "location"; //$NON-NLS-1$
	

	public static final String INCLUDE_PATH = "includePath"; //$NON-NLS-1$
	public static final String INCLUDE_FILE = "includeFile"; //$NON-NLS-1$
	public static final String MACRO = "macro"; //$NON-NLS-1$
	public static final String MACRO_FILE = "macroFile"; //$NON-NLS-1$
	public static final String LIBRARY_PATH = "libraryPath"; //$NON-NLS-1$
	public static final String LIBRARY_FILE = "libraryFile"; //$NON-NLS-1$
	public static final String SOURCE_PATH = "sourcePath"; //$NON-NLS-1$
	public static final String OUTPUT_PATH = "outputPath"; //$NON-NLS-1$

	public static final String BUILTIN = "BUILTIN"; //$NON-NLS-1$
	public static final String READONLY = "READONLY"; //$NON-NLS-1$
	public static final String LOCAL = "LOCAL"; //$NON-NLS-1$
	public static final String VALUE_WORKSPACE_PATH = "VALUE_WORKSPACE_PATH"; //$NON-NLS-1$
	public static final String RESOLVED = "RESOLVED"; //$NON-NLS-1$
	
	public static final String FLAGS_SEPARATOR = "|"; //$NON-NLS-1$

	public static ICLanguageSettingEntry[] loadEntries(ICStorageElement el){
		List list = loadEntriesList(el);
		return (ICLanguageSettingEntry[])list.toArray(new ICLanguageSettingEntry[list.size()]);
	}

	public static List loadEntriesList(ICStorageElement el){
		ICStorageElement children[] = el.getChildren();
		ICStorageElement child;
		List list = new ArrayList();
		ICLanguageSettingEntry entry;
		for(int i = 0; i < children.length; i++){
			child = children[i];
			if(ELEMENT_ENTRY.equals(child.getName())){
				entry = loadEntry(child);
				if(entry != null)
					list.add(entry);
			}
		}
		return list;
	}

	public static ICLanguageSettingEntry loadEntry(ICStorageElement el){
		int kind = stringToKind(el.getAttribute(ATTRIBUTE_KIND));
		if(kind == 0)
			return null;
		
		int flags = composeFlags(el.getAttribute(ATTRIBUTE_FLAGS));
		String name = el.getAttribute(ATTRIBUTE_NAME);

		
		switch(kind){
		case ICLanguageSettingEntry.INCLUDE_PATH:
			return new CIncludePathEntry(name, flags);
		case ICLanguageSettingEntry.INCLUDE_FILE:
			return new CIncludeFileEntry(name, flags);
		case ICLanguageSettingEntry.MACRO:
			String value = el.getAttribute(ATTRIBUTE_VALUE);
			return new CMacroEntry(name, value, flags);
		case ICLanguageSettingEntry.MACRO_FILE:
			return new CMacroFileEntry(name, flags);
		case ICLanguageSettingEntry.LIBRARY_PATH:
			return new CLibraryPathEntry(name, flags);
		case ICLanguageSettingEntry.LIBRARY_FILE:
			return new CLibraryFileEntry(name, flags);
		case ICLanguageSettingEntry.OUTPUT_PATH:
			return new COutputEntry(name, loadExclusions(el), flags);
		case ICLanguageSettingEntry.SOURCE_PATH:
			return new CSourceEntry(name, loadExclusions(el), flags);
		}
		return null;
	}
	
	private static IPath[] loadExclusions(ICStorageElement el){
		String attr = el.getAttribute(ATTRIBUTE_EXCLUDING);
		if(attr != null){
			String[] strs = CDataUtil.stringToArray(attr, FLAGS_SEPARATOR);
			IPath[] paths = new IPath[strs.length];
			for(int i = 0; i < strs.length; i++){
				paths[i] = new Path(strs[i]);
			}
			return paths;
		}
		return null; 
	}

	private static void storeExclusions(ICStorageElement el, IPath[] paths){
		if(paths == null || paths.length == 0)
			return;
			
		String[] strs = new String[paths.length];
		for(int i = 0; i < strs.length; i++){
			strs[i] = paths[i].toString();
		}
		
		String attr = CDataUtil.arrayToString(strs, FLAGS_SEPARATOR);
		el.setAttribute(ATTRIBUTE_EXCLUDING, attr);
	}

	public static void serializeEntries(ICSettingEntry entries[], ICStorageElement element){
		ICStorageElement child;
		for(int i = 0; i < entries.length; i++){
			child = element.createChild(ELEMENT_ENTRY);
			serializeEntry(entries[i], child);
		}
	}
	
	public static void serializeEntry(ICSettingEntry entry, ICStorageElement element){
		String kind = kindToString(entry.getKind());
		String flags = composeFlagsString(entry.getFlags());
		String name = entry.getName();
		element.setAttribute(ATTRIBUTE_KIND, kind);
		element.setAttribute(ATTRIBUTE_FLAGS, flags);
		element.setAttribute(ATTRIBUTE_NAME, name);
		switch(entry.getKind()){
		case ICLanguageSettingEntry.MACRO:
			String value = entry.getValue();
			element.setAttribute(ATTRIBUTE_VALUE, value);
			break;
		case ICLanguageSettingEntry.SOURCE_PATH:
		case ICLanguageSettingEntry.OUTPUT_PATH:
			IPath paths[] = ((ICExclusionPatternPathEntry)entry).getExclusionPatterns();
			storeExclusions(element, paths);
			break;
		}
	}
	
	static String kindToString(int kind){
		switch(kind){
		case ICLanguageSettingEntry.INCLUDE_PATH:
			return INCLUDE_PATH;
		case ICLanguageSettingEntry.INCLUDE_FILE:
			return INCLUDE_FILE;
		case ICLanguageSettingEntry.MACRO:
			return MACRO;
		case ICLanguageSettingEntry.MACRO_FILE:
			return MACRO_FILE;
		case ICLanguageSettingEntry.LIBRARY_PATH:
			return LIBRARY_PATH;
		case ICLanguageSettingEntry.LIBRARY_FILE:
			return LIBRARY_FILE;
		case ICLanguageSettingEntry.SOURCE_PATH:
			return SOURCE_PATH;
		case ICLanguageSettingEntry.OUTPUT_PATH:
			return OUTPUT_PATH;
		}
		throw new UnsupportedOperationException();
	}

	static int stringToKind(String kind){
		if(INCLUDE_PATH.equals(kind))
			return ICLanguageSettingEntry.INCLUDE_PATH;
		if(INCLUDE_FILE.equals(kind))
			return ICLanguageSettingEntry.INCLUDE_FILE;
		if(MACRO.equals(kind))
			return ICLanguageSettingEntry.MACRO;
		if(MACRO_FILE.equals(kind))
			return ICLanguageSettingEntry.MACRO_FILE;
		if(LIBRARY_PATH.equals(kind))
			return ICLanguageSettingEntry.LIBRARY_PATH;
		if(LIBRARY_FILE.equals(kind))
			return ICLanguageSettingEntry.LIBRARY_FILE;
		if(SOURCE_PATH.equals(kind))
			return ICLanguageSettingEntry.SOURCE_PATH;
		if(OUTPUT_PATH.equals(kind))
			return ICLanguageSettingEntry.OUTPUT_PATH;
		return 0;
//		throw new UnsupportedOperationException();
	}

	private static String composeFlagsString(int flags){
		StringBuffer buf = new StringBuffer();
		if((flags & ICLanguageSettingEntry.BUILTIN) != 0){
			buf.append(BUILTIN);
		}
		if((flags & ICLanguageSettingEntry.READONLY) != 0){
			if(buf.length() != 0)
				buf.append(FLAGS_SEPARATOR);
			
			buf.append(READONLY);
		}
		if((flags & ICLanguageSettingEntry.LOCAL) != 0){
			if(buf.length() != 0)
				buf.append(FLAGS_SEPARATOR);
			
			buf.append(LOCAL);
		}
		if((flags & ICLanguageSettingEntry.VALUE_WORKSPACE_PATH) != 0){
			if(buf.length() != 0)
				buf.append(FLAGS_SEPARATOR);
			
			buf.append(VALUE_WORKSPACE_PATH);
		}
		if((flags & ICLanguageSettingEntry.RESOLVED) != 0){
			if(buf.length() != 0)
				buf.append(FLAGS_SEPARATOR);
			
			buf.append(RESOLVED);
		}
		return buf.toString();
	}
	
	private static int composeFlags(String flagsString){
		if(flagsString == null || flagsString.length() == 0)
			return 0;
		
		StringTokenizer tokenizer = new StringTokenizer(flagsString, FLAGS_SEPARATOR);
		int flags = 0;
		String f;
		while(tokenizer.hasMoreElements()){
			f = tokenizer.nextToken();
			if(BUILTIN.equals(f))
				flags |= ICLanguageSettingEntry.BUILTIN; 
			if(READONLY.equals(f))
				flags |= ICLanguageSettingEntry.READONLY; 
			if(LOCAL.equals(f))
				flags |= ICLanguageSettingEntry.LOCAL; 
			if(VALUE_WORKSPACE_PATH.equals(f))
				flags |= ICLanguageSettingEntry.VALUE_WORKSPACE_PATH; 
			if(RESOLVED.equals(f))
				flags |= ICLanguageSettingEntry.RESOLVED; 
		}
		
		return flags;
	}

}
