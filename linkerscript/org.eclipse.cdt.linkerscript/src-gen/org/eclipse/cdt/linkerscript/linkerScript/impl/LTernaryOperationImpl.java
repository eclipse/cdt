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
package org.eclipse.cdt.linkerscript.linkerScript.impl;

import org.eclipse.cdt.linkerscript.linkerScript.LExpression;
import org.eclipse.cdt.linkerscript.linkerScript.LTernaryOperation;
import org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>LTernary Operation</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.impl.LTernaryOperationImpl#getCondition <em>Condition</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.impl.LTernaryOperationImpl#getIfPart <em>If Part</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.impl.LTernaryOperationImpl#getThenPart <em>Then Part</em>}</li>
 * </ul>
 *
 * @generated
 */
public class LTernaryOperationImpl extends LExpressionImpl implements LTernaryOperation
{
  /**
   * The cached value of the '{@link #getCondition() <em>Condition</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getCondition()
   * @generated
   * @ordered
   */
  protected LExpression condition;

  /**
   * The cached value of the '{@link #getIfPart() <em>If Part</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getIfPart()
   * @generated
   * @ordered
   */
  protected LExpression ifPart;

  /**
   * The cached value of the '{@link #getThenPart() <em>Then Part</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getThenPart()
   * @generated
   * @ordered
   */
  protected LExpression thenPart;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected LTernaryOperationImpl()
  {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  protected EClass eStaticClass()
  {
    return LinkerScriptPackage.Literals.LTERNARY_OPERATION;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public LExpression getCondition()
  {
    return condition;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetCondition(LExpression newCondition, NotificationChain msgs)
  {
    LExpression oldCondition = condition;
    condition = newCondition;
    if (eNotificationRequired())
    {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, LinkerScriptPackage.LTERNARY_OPERATION__CONDITION, oldCondition, newCondition);
      if (msgs == null) msgs = notification; else msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setCondition(LExpression newCondition)
  {
    if (newCondition != condition)
    {
      NotificationChain msgs = null;
      if (condition != null)
        msgs = ((InternalEObject)condition).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - LinkerScriptPackage.LTERNARY_OPERATION__CONDITION, null, msgs);
      if (newCondition != null)
        msgs = ((InternalEObject)newCondition).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - LinkerScriptPackage.LTERNARY_OPERATION__CONDITION, null, msgs);
      msgs = basicSetCondition(newCondition, msgs);
      if (msgs != null) msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, LinkerScriptPackage.LTERNARY_OPERATION__CONDITION, newCondition, newCondition));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public LExpression getIfPart()
  {
    return ifPart;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetIfPart(LExpression newIfPart, NotificationChain msgs)
  {
    LExpression oldIfPart = ifPart;
    ifPart = newIfPart;
    if (eNotificationRequired())
    {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, LinkerScriptPackage.LTERNARY_OPERATION__IF_PART, oldIfPart, newIfPart);
      if (msgs == null) msgs = notification; else msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setIfPart(LExpression newIfPart)
  {
    if (newIfPart != ifPart)
    {
      NotificationChain msgs = null;
      if (ifPart != null)
        msgs = ((InternalEObject)ifPart).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - LinkerScriptPackage.LTERNARY_OPERATION__IF_PART, null, msgs);
      if (newIfPart != null)
        msgs = ((InternalEObject)newIfPart).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - LinkerScriptPackage.LTERNARY_OPERATION__IF_PART, null, msgs);
      msgs = basicSetIfPart(newIfPart, msgs);
      if (msgs != null) msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, LinkerScriptPackage.LTERNARY_OPERATION__IF_PART, newIfPart, newIfPart));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public LExpression getThenPart()
  {
    return thenPart;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetThenPart(LExpression newThenPart, NotificationChain msgs)
  {
    LExpression oldThenPart = thenPart;
    thenPart = newThenPart;
    if (eNotificationRequired())
    {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, LinkerScriptPackage.LTERNARY_OPERATION__THEN_PART, oldThenPart, newThenPart);
      if (msgs == null) msgs = notification; else msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setThenPart(LExpression newThenPart)
  {
    if (newThenPart != thenPart)
    {
      NotificationChain msgs = null;
      if (thenPart != null)
        msgs = ((InternalEObject)thenPart).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - LinkerScriptPackage.LTERNARY_OPERATION__THEN_PART, null, msgs);
      if (newThenPart != null)
        msgs = ((InternalEObject)newThenPart).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - LinkerScriptPackage.LTERNARY_OPERATION__THEN_PART, null, msgs);
      msgs = basicSetThenPart(newThenPart, msgs);
      if (msgs != null) msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, LinkerScriptPackage.LTERNARY_OPERATION__THEN_PART, newThenPart, newThenPart));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs)
  {
    switch (featureID)
    {
      case LinkerScriptPackage.LTERNARY_OPERATION__CONDITION:
        return basicSetCondition(null, msgs);
      case LinkerScriptPackage.LTERNARY_OPERATION__IF_PART:
        return basicSetIfPart(null, msgs);
      case LinkerScriptPackage.LTERNARY_OPERATION__THEN_PART:
        return basicSetThenPart(null, msgs);
    }
    return super.eInverseRemove(otherEnd, featureID, msgs);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Object eGet(int featureID, boolean resolve, boolean coreType)
  {
    switch (featureID)
    {
      case LinkerScriptPackage.LTERNARY_OPERATION__CONDITION:
        return getCondition();
      case LinkerScriptPackage.LTERNARY_OPERATION__IF_PART:
        return getIfPart();
      case LinkerScriptPackage.LTERNARY_OPERATION__THEN_PART:
        return getThenPart();
    }
    return super.eGet(featureID, resolve, coreType);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public void eSet(int featureID, Object newValue)
  {
    switch (featureID)
    {
      case LinkerScriptPackage.LTERNARY_OPERATION__CONDITION:
        setCondition((LExpression)newValue);
        return;
      case LinkerScriptPackage.LTERNARY_OPERATION__IF_PART:
        setIfPart((LExpression)newValue);
        return;
      case LinkerScriptPackage.LTERNARY_OPERATION__THEN_PART:
        setThenPart((LExpression)newValue);
        return;
    }
    super.eSet(featureID, newValue);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public void eUnset(int featureID)
  {
    switch (featureID)
    {
      case LinkerScriptPackage.LTERNARY_OPERATION__CONDITION:
        setCondition((LExpression)null);
        return;
      case LinkerScriptPackage.LTERNARY_OPERATION__IF_PART:
        setIfPart((LExpression)null);
        return;
      case LinkerScriptPackage.LTERNARY_OPERATION__THEN_PART:
        setThenPart((LExpression)null);
        return;
    }
    super.eUnset(featureID);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public boolean eIsSet(int featureID)
  {
    switch (featureID)
    {
      case LinkerScriptPackage.LTERNARY_OPERATION__CONDITION:
        return condition != null;
      case LinkerScriptPackage.LTERNARY_OPERATION__IF_PART:
        return ifPart != null;
      case LinkerScriptPackage.LTERNARY_OPERATION__THEN_PART:
        return thenPart != null;
    }
    return super.eIsSet(featureID);
  }

} //LTernaryOperationImpl
