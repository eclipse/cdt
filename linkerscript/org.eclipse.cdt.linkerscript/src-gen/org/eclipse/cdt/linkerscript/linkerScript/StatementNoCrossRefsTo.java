/**
 * ******************************************************************************
 * Copyright (c) 2016, 2017 Kichwa Coders Ltd (https://kichwacoders.com/) and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *  ******************************************************************************
 * 
 * 
 * generated by Xtext 2.10.0
 * Copyright header generated by GenerateLinkerScript.mwe2
 */
package org.eclipse.cdt.linkerscript.linkerScript;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Statement No Cross Refs To</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.StatementNoCrossRefsTo#getSections <em>Sections</em>}</li>
 * </ul>
 *
 * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getStatementNoCrossRefsTo()
 * @model
 * @generated
 */
public interface StatementNoCrossRefsTo extends Statement
{
  /**
   * Returns the value of the '<em><b>Sections</b></em>' attribute list.
   * The list contents are of type {@link java.lang.String}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Sections</em>' attribute list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Sections</em>' attribute list.
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getStatementNoCrossRefsTo_Sections()
   * @model unique="false"
   * @generated
   */
  EList<String> getSections();

} // StatementNoCrossRefsTo
