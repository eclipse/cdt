/**
 * generated by Xtext 2.10.0
 */
package org.eclipse.cdt.linkerscript.linkerScript.impl;

import java.util.Collection;

import org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage;
import org.eclipse.cdt.linkerscript.linkerScript.OutputSectionCommand;
import org.eclipse.cdt.linkerscript.linkerScript.SectionsCommand;

import org.eclipse.emf.common.notify.NotificationChain;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>Sections Command</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.impl.SectionsCommandImpl#getSectionCommands <em>Section Commands</em>}</li>
 * </ul>
 *
 * @generated
 */
public class SectionsCommandImpl extends LinkerScriptStatementImpl implements SectionsCommand
{
  /**
   * The cached value of the '{@link #getSectionCommands() <em>Section Commands</em>}' containment reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getSectionCommands()
   * @generated
   * @ordered
   */
  protected EList<OutputSectionCommand> sectionCommands;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected SectionsCommandImpl()
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
    return LinkerScriptPackage.Literals.SECTIONS_COMMAND;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EList<OutputSectionCommand> getSectionCommands()
  {
    if (sectionCommands == null)
    {
      sectionCommands = new EObjectContainmentEList<OutputSectionCommand>(OutputSectionCommand.class, this, LinkerScriptPackage.SECTIONS_COMMAND__SECTION_COMMANDS);
    }
    return sectionCommands;
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
      case LinkerScriptPackage.SECTIONS_COMMAND__SECTION_COMMANDS:
        return ((InternalEList<?>)getSectionCommands()).basicRemove(otherEnd, msgs);
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
      case LinkerScriptPackage.SECTIONS_COMMAND__SECTION_COMMANDS:
        return getSectionCommands();
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
      case LinkerScriptPackage.SECTIONS_COMMAND__SECTION_COMMANDS:
        getSectionCommands().clear();
        getSectionCommands().addAll((Collection<? extends OutputSectionCommand>)newValue);
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
      case LinkerScriptPackage.SECTIONS_COMMAND__SECTION_COMMANDS:
        getSectionCommands().clear();
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
      case LinkerScriptPackage.SECTIONS_COMMAND__SECTION_COMMANDS:
        return sectionCommands != null && !sectionCommands.isEmpty();
    }
    return super.eIsSet(featureID);
  }

} //SectionsCommandImpl
