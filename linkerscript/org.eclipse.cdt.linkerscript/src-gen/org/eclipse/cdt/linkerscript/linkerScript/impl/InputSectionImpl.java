/**
 * generated by Xtext 2.10.0
 */
package org.eclipse.cdt.linkerscript.linkerScript.impl;

import java.util.Collection;

import org.eclipse.cdt.linkerscript.linkerScript.InputSection;
import org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.MinimalEObjectImpl;

import org.eclipse.emf.ecore.util.EDataTypeEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Input Section</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.impl.InputSectionImpl#getFlags <em>Flags</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.impl.InputSectionImpl#isKeep <em>Keep</em>}</li>
 * </ul>
 *
 * @generated
 */
public class InputSectionImpl extends MinimalEObjectImpl.Container implements InputSection
{
  /**
   * The cached value of the '{@link #getFlags() <em>Flags</em>}' attribute list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getFlags()
   * @generated
   * @ordered
   */
  protected EList<String> flags;

  /**
   * The default value of the '{@link #isKeep() <em>Keep</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #isKeep()
   * @generated
   * @ordered
   */
  protected static final boolean KEEP_EDEFAULT = false;

  /**
   * The cached value of the '{@link #isKeep() <em>Keep</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #isKeep()
   * @generated
   * @ordered
   */
  protected boolean keep = KEEP_EDEFAULT;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected InputSectionImpl()
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
    return LinkerScriptPackage.Literals.INPUT_SECTION;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EList<String> getFlags()
  {
    if (flags == null)
    {
      flags = new EDataTypeEList<String>(String.class, this, LinkerScriptPackage.INPUT_SECTION__FLAGS);
    }
    return flags;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public boolean isKeep()
  {
    return keep;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setKeep(boolean newKeep)
  {
    boolean oldKeep = keep;
    keep = newKeep;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, LinkerScriptPackage.INPUT_SECTION__KEEP, oldKeep, keep));
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
      case LinkerScriptPackage.INPUT_SECTION__FLAGS:
        return getFlags();
      case LinkerScriptPackage.INPUT_SECTION__KEEP:
        return isKeep();
    }
    return super.eGet(featureID, resolve, coreType);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @SuppressWarnings("unchecked")
  @Override
  public void eSet(int featureID, Object newValue)
  {
    switch (featureID)
    {
      case LinkerScriptPackage.INPUT_SECTION__FLAGS:
        getFlags().clear();
        getFlags().addAll((Collection<? extends String>)newValue);
        return;
      case LinkerScriptPackage.INPUT_SECTION__KEEP:
        setKeep((Boolean)newValue);
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
      case LinkerScriptPackage.INPUT_SECTION__FLAGS:
        getFlags().clear();
        return;
      case LinkerScriptPackage.INPUT_SECTION__KEEP:
        setKeep(KEEP_EDEFAULT);
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
      case LinkerScriptPackage.INPUT_SECTION__FLAGS:
        return flags != null && !flags.isEmpty();
      case LinkerScriptPackage.INPUT_SECTION__KEEP:
        return keep != KEEP_EDEFAULT;
    }
    return super.eIsSet(featureID);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public String toString()
  {
    if (eIsProxy()) return super.toString();

    StringBuffer result = new StringBuffer(super.toString());
    result.append(" (flags: ");
    result.append(flags);
    result.append(", keep: ");
    result.append(keep);
    result.append(')');
    return result.toString();
  }

} //InputSectionImpl
