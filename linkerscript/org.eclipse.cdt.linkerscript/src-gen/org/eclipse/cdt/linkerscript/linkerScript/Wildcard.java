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
 * A representation of the model object '<em><b>Wildcard</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.Wildcard#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.Wildcard#getExcludes <em>Excludes</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.Wildcard#getPrimarySort <em>Primary Sort</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.Wildcard#getSecondarySort <em>Secondary Sort</em>}</li>
 * </ul>
 *
 * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getWildcard()
 * @model
 * @generated
 */
public interface Wildcard extends EObject
{
  /**
   * Returns the value of the '<em><b>Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Name</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Name</em>' attribute.
   * @see #setName(String)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getWildcard_Name()
   * @model
   * @generated
   */
  String getName();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.Wildcard#getName <em>Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Name</em>' attribute.
   * @see #getName()
   * @generated
   */
  void setName(String value);

  /**
   * Returns the value of the '<em><b>Excludes</b></em>' attribute list.
   * The list contents are of type {@link java.lang.String}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Excludes</em>' attribute list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Excludes</em>' attribute list.
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getWildcard_Excludes()
   * @model unique="false"
   * @generated
   */
  EList<String> getExcludes();

  /**
   * Returns the value of the '<em><b>Primary Sort</b></em>' attribute.
   * The literals are from the enumeration {@link org.eclipse.cdt.linkerscript.linkerScript.WildcardSort}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Primary Sort</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Primary Sort</em>' attribute.
   * @see org.eclipse.cdt.linkerscript.linkerScript.WildcardSort
   * @see #setPrimarySort(WildcardSort)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getWildcard_PrimarySort()
   * @model
   * @generated
   */
  WildcardSort getPrimarySort();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.Wildcard#getPrimarySort <em>Primary Sort</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Primary Sort</em>' attribute.
   * @see org.eclipse.cdt.linkerscript.linkerScript.WildcardSort
   * @see #getPrimarySort()
   * @generated
   */
  void setPrimarySort(WildcardSort value);

  /**
   * Returns the value of the '<em><b>Secondary Sort</b></em>' attribute.
   * The literals are from the enumeration {@link org.eclipse.cdt.linkerscript.linkerScript.WildcardSort}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Secondary Sort</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Secondary Sort</em>' attribute.
   * @see org.eclipse.cdt.linkerscript.linkerScript.WildcardSort
   * @see #setSecondarySort(WildcardSort)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getWildcard_SecondarySort()
   * @model
   * @generated
   */
  WildcardSort getSecondarySort();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.Wildcard#getSecondarySort <em>Secondary Sort</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Secondary Sort</em>' attribute.
   * @see org.eclipse.cdt.linkerscript.linkerScript.WildcardSort
   * @see #getSecondarySort()
   * @generated
   */
  void setSecondarySort(WildcardSort value);

} // Wildcard
