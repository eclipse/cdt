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
 * A representation of the model object '<em><b>LFeature Call</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.LFeatureCall#getFeature <em>Feature</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.LFeatureCall#isExplicitOperationCall <em>Explicit Operation Call</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.LFeatureCall#getFeatureCallArguments <em>Feature Call Arguments</em>}</li>
 * </ul>
 *
 * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getLFeatureCall()
 * @model
 * @generated
 */
public interface LFeatureCall extends LExpression
{
  /**
   * Returns the value of the '<em><b>Feature</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Feature</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Feature</em>' attribute.
   * @see #setFeature(String)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getLFeatureCall_Feature()
   * @model
   * @generated
   */
  String getFeature();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.LFeatureCall#getFeature <em>Feature</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Feature</em>' attribute.
   * @see #getFeature()
   * @generated
   */
  void setFeature(String value);

  /**
   * Returns the value of the '<em><b>Explicit Operation Call</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Explicit Operation Call</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Explicit Operation Call</em>' attribute.
   * @see #setExplicitOperationCall(boolean)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getLFeatureCall_ExplicitOperationCall()
   * @model
   * @generated
   */
  boolean isExplicitOperationCall();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.LFeatureCall#isExplicitOperationCall <em>Explicit Operation Call</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Explicit Operation Call</em>' attribute.
   * @see #isExplicitOperationCall()
   * @generated
   */
  void setExplicitOperationCall(boolean value);

  /**
   * Returns the value of the '<em><b>Feature Call Arguments</b></em>' containment reference list.
   * The list contents are of type {@link org.eclipse.cdt.linkerscript.linkerScript.LExpression}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Feature Call Arguments</em>' containment reference list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Feature Call Arguments</em>' containment reference list.
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getLFeatureCall_FeatureCallArguments()
   * @model containment="true"
   * @generated
   */
  EList<LExpression> getFeatureCallArguments();

} // LFeatureCall
