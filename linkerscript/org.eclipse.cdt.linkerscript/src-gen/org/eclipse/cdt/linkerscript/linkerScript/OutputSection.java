/**
 * generated by Xtext 2.10.0
 */
package org.eclipse.cdt.linkerscript.linkerScript;

import org.eclipse.emf.common.util.EList;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Output Section</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getName <em>Name</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getAddress <em>Address</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getType <em>Type</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getAt <em>At</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getAlign <em>Align</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getSubAlign <em>Sub Align</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getConstraint <em>Constraint</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getStatements <em>Statements</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getMemory <em>Memory</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getAtMemory <em>At Memory</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getPhdrs <em>Phdrs</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getFill <em>Fill</em>}</li>
 * </ul>
 *
 * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getOutputSection()
 * @model
 * @generated
 */
public interface OutputSection extends OutputSectionCommand
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
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getOutputSection_Name()
   * @model
   * @generated
   */
  String getName();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getName <em>Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Name</em>' attribute.
   * @see #getName()
   * @generated
   */
  void setName(String value);

  /**
   * Returns the value of the '<em><b>Address</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Address</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Address</em>' containment reference.
   * @see #setAddress(LExpression)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getOutputSection_Address()
   * @model containment="true"
   * @generated
   */
  LExpression getAddress();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getAddress <em>Address</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Address</em>' containment reference.
   * @see #getAddress()
   * @generated
   */
  void setAddress(LExpression value);

  /**
   * Returns the value of the '<em><b>Type</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Type</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Type</em>' containment reference.
   * @see #setType(OutputSectionType)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getOutputSection_Type()
   * @model containment="true"
   * @generated
   */
  OutputSectionType getType();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getType <em>Type</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Type</em>' containment reference.
   * @see #getType()
   * @generated
   */
  void setType(OutputSectionType value);

  /**
   * Returns the value of the '<em><b>At</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>At</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>At</em>' containment reference.
   * @see #setAt(LExpression)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getOutputSection_At()
   * @model containment="true"
   * @generated
   */
  LExpression getAt();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getAt <em>At</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>At</em>' containment reference.
   * @see #getAt()
   * @generated
   */
  void setAt(LExpression value);

  /**
   * Returns the value of the '<em><b>Align</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Align</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Align</em>' containment reference.
   * @see #setAlign(OutputSectionAlign)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getOutputSection_Align()
   * @model containment="true"
   * @generated
   */
  OutputSectionAlign getAlign();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getAlign <em>Align</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Align</em>' containment reference.
   * @see #getAlign()
   * @generated
   */
  void setAlign(OutputSectionAlign value);

  /**
   * Returns the value of the '<em><b>Sub Align</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Sub Align</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Sub Align</em>' containment reference.
   * @see #setSubAlign(LExpression)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getOutputSection_SubAlign()
   * @model containment="true"
   * @generated
   */
  LExpression getSubAlign();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getSubAlign <em>Sub Align</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Sub Align</em>' containment reference.
   * @see #getSubAlign()
   * @generated
   */
  void setSubAlign(LExpression value);

  /**
   * Returns the value of the '<em><b>Constraint</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Constraint</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Constraint</em>' containment reference.
   * @see #setConstraint(OutputSectionConstraint)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getOutputSection_Constraint()
   * @model containment="true"
   * @generated
   */
  OutputSectionConstraint getConstraint();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getConstraint <em>Constraint</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Constraint</em>' containment reference.
   * @see #getConstraint()
   * @generated
   */
  void setConstraint(OutputSectionConstraint value);

  /**
   * Returns the value of the '<em><b>Statements</b></em>' containment reference list.
   * The list contents are of type {@link org.eclipse.cdt.linkerscript.linkerScript.Statement}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Statements</em>' containment reference list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Statements</em>' containment reference list.
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getOutputSection_Statements()
   * @model containment="true"
   * @generated
   */
  EList<Statement> getStatements();

  /**
   * Returns the value of the '<em><b>Memory</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Memory</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Memory</em>' attribute.
   * @see #setMemory(String)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getOutputSection_Memory()
   * @model
   * @generated
   */
  String getMemory();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getMemory <em>Memory</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Memory</em>' attribute.
   * @see #getMemory()
   * @generated
   */
  void setMemory(String value);

  /**
   * Returns the value of the '<em><b>At Memory</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>At Memory</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>At Memory</em>' attribute.
   * @see #setAtMemory(String)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getOutputSection_AtMemory()
   * @model
   * @generated
   */
  String getAtMemory();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getAtMemory <em>At Memory</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>At Memory</em>' attribute.
   * @see #getAtMemory()
   * @generated
   */
  void setAtMemory(String value);

  /**
   * Returns the value of the '<em><b>Phdrs</b></em>' attribute list.
   * The list contents are of type {@link java.lang.String}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Phdrs</em>' attribute list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Phdrs</em>' attribute list.
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getOutputSection_Phdrs()
   * @model unique="false"
   * @generated
   */
  EList<String> getPhdrs();

  /**
   * Returns the value of the '<em><b>Fill</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Fill</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Fill</em>' containment reference.
   * @see #setFill(LExpression)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getOutputSection_Fill()
   * @model containment="true"
   * @generated
   */
  LExpression getFill();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.OutputSection#getFill <em>Fill</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Fill</em>' containment reference.
   * @see #getFill()
   * @generated
   */
  void setFill(LExpression value);

} // OutputSection
