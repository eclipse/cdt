/**
 * generated by Xtext 2.10.0
 */
package org.eclipse.cdt.linkerscript.linkerScript.impl;

import org.eclipse.cdt.linkerscript.linkerScript.AlignCall;
import org.eclipse.cdt.linkerscript.linkerScript.LExpression;
import org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Align Call</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.impl.AlignCallImpl#getExpOrAlign <em>Exp Or Align</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.impl.AlignCallImpl#getAlign <em>Align</em>}</li>
 * </ul>
 *
 * @generated
 */
public class AlignCallImpl extends LExpressionImpl implements AlignCall
{
  /**
   * The cached value of the '{@link #getExpOrAlign() <em>Exp Or Align</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getExpOrAlign()
   * @generated
   * @ordered
   */
  protected LExpression expOrAlign;

  /**
   * The cached value of the '{@link #getAlign() <em>Align</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getAlign()
   * @generated
   * @ordered
   */
  protected LExpression align;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected AlignCallImpl()
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
    return LinkerScriptPackage.Literals.ALIGN_CALL;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public LExpression getExpOrAlign()
  {
    return expOrAlign;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetExpOrAlign(LExpression newExpOrAlign, NotificationChain msgs)
  {
    LExpression oldExpOrAlign = expOrAlign;
    expOrAlign = newExpOrAlign;
    if (eNotificationRequired())
    {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, LinkerScriptPackage.ALIGN_CALL__EXP_OR_ALIGN, oldExpOrAlign, newExpOrAlign);
      if (msgs == null) msgs = notification; else msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setExpOrAlign(LExpression newExpOrAlign)
  {
    if (newExpOrAlign != expOrAlign)
    {
      NotificationChain msgs = null;
      if (expOrAlign != null)
        msgs = ((InternalEObject)expOrAlign).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - LinkerScriptPackage.ALIGN_CALL__EXP_OR_ALIGN, null, msgs);
      if (newExpOrAlign != null)
        msgs = ((InternalEObject)newExpOrAlign).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - LinkerScriptPackage.ALIGN_CALL__EXP_OR_ALIGN, null, msgs);
      msgs = basicSetExpOrAlign(newExpOrAlign, msgs);
      if (msgs != null) msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, LinkerScriptPackage.ALIGN_CALL__EXP_OR_ALIGN, newExpOrAlign, newExpOrAlign));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public LExpression getAlign()
  {
    return align;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetAlign(LExpression newAlign, NotificationChain msgs)
  {
    LExpression oldAlign = align;
    align = newAlign;
    if (eNotificationRequired())
    {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, LinkerScriptPackage.ALIGN_CALL__ALIGN, oldAlign, newAlign);
      if (msgs == null) msgs = notification; else msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setAlign(LExpression newAlign)
  {
    if (newAlign != align)
    {
      NotificationChain msgs = null;
      if (align != null)
        msgs = ((InternalEObject)align).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - LinkerScriptPackage.ALIGN_CALL__ALIGN, null, msgs);
      if (newAlign != null)
        msgs = ((InternalEObject)newAlign).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - LinkerScriptPackage.ALIGN_CALL__ALIGN, null, msgs);
      msgs = basicSetAlign(newAlign, msgs);
      if (msgs != null) msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, LinkerScriptPackage.ALIGN_CALL__ALIGN, newAlign, newAlign));
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
      case LinkerScriptPackage.ALIGN_CALL__EXP_OR_ALIGN:
        return basicSetExpOrAlign(null, msgs);
      case LinkerScriptPackage.ALIGN_CALL__ALIGN:
        return basicSetAlign(null, msgs);
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
      case LinkerScriptPackage.ALIGN_CALL__EXP_OR_ALIGN:
        return getExpOrAlign();
      case LinkerScriptPackage.ALIGN_CALL__ALIGN:
        return getAlign();
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
      case LinkerScriptPackage.ALIGN_CALL__EXP_OR_ALIGN:
        setExpOrAlign((LExpression)newValue);
        return;
      case LinkerScriptPackage.ALIGN_CALL__ALIGN:
        setAlign((LExpression)newValue);
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
      case LinkerScriptPackage.ALIGN_CALL__EXP_OR_ALIGN:
        setExpOrAlign((LExpression)null);
        return;
      case LinkerScriptPackage.ALIGN_CALL__ALIGN:
        setAlign((LExpression)null);
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
      case LinkerScriptPackage.ALIGN_CALL__EXP_OR_ALIGN:
        return expOrAlign != null;
      case LinkerScriptPackage.ALIGN_CALL__ALIGN:
        return align != null;
    }
    return super.eIsSet(featureID);
  }

} //AlignCallImpl
