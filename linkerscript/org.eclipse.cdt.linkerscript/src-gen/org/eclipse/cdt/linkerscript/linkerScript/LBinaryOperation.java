/**
 * generated by Xtext 2.10.0
 */
package org.eclipse.cdt.linkerscript.linkerScript;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>LBinary Operation</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.LBinaryOperation#getLeftOperand <em>Left Operand</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.LBinaryOperation#getFeature <em>Feature</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.LBinaryOperation#getRightOperand <em>Right Operand</em>}</li>
 * </ul>
 *
 * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getLBinaryOperation()
 * @model
 * @generated
 */
public interface LBinaryOperation extends LExpression
{
  /**
   * Returns the value of the '<em><b>Left Operand</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Left Operand</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Left Operand</em>' containment reference.
   * @see #setLeftOperand(LExpression)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getLBinaryOperation_LeftOperand()
   * @model containment="true"
   * @generated
   */
  LExpression getLeftOperand();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.LBinaryOperation#getLeftOperand <em>Left Operand</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Left Operand</em>' containment reference.
   * @see #getLeftOperand()
   * @generated
   */
  void setLeftOperand(LExpression value);

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
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getLBinaryOperation_Feature()
   * @model
   * @generated
   */
  String getFeature();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.LBinaryOperation#getFeature <em>Feature</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Feature</em>' attribute.
   * @see #getFeature()
   * @generated
   */
  void setFeature(String value);

  /**
   * Returns the value of the '<em><b>Right Operand</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Right Operand</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Right Operand</em>' containment reference.
   * @see #setRightOperand(LExpression)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getLBinaryOperation_RightOperand()
   * @model containment="true"
   * @generated
   */
  LExpression getRightOperand();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.LBinaryOperation#getRightOperand <em>Right Operand</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Right Operand</em>' containment reference.
   * @see #getRightOperand()
   * @generated
   */
  void setRightOperand(LExpression value);

} // LBinaryOperation
