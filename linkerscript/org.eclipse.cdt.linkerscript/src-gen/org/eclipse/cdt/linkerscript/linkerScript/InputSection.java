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

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Input Section</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.InputSection#getFlags <em>Flags</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.InputSection#isKeep <em>Keep</em>}</li>
 * </ul>
 *
 * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getInputSection()
 * @model
 * @generated
 */
public interface InputSection extends EObject
{
  /**
   * Returns the value of the '<em><b>Flags</b></em>' attribute list.
   * The list contents are of type {@link java.lang.String}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Flags</em>' attribute list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Flags</em>' attribute list.
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getInputSection_Flags()
   * @model unique="false"
   * @generated
   */
  EList<String> getFlags();

  /**
   * Returns the value of the '<em><b>Keep</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Keep</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Keep</em>' attribute.
   * @see #setKeep(boolean)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getInputSection_Keep()
   * @model
   * @generated
   */
  boolean isKeep();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.InputSection#isKeep <em>Keep</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Keep</em>' attribute.
   * @see #isKeep()
   * @generated
   */
  void setKeep(boolean value);

} // InputSection
