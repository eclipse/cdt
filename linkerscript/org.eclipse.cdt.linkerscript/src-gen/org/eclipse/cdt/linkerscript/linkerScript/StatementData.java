/**
 * generated by Xtext 2.10.0
 */
package org.eclipse.cdt.linkerscript.linkerScript;


/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>Statement Data</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * </p>
 * <ul>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.StatementData#getSize <em>Size</em>}</li>
 *   <li>{@link org.eclipse.cdt.linkerscript.linkerScript.StatementData#getData <em>Data</em>}</li>
 * </ul>
 *
 * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getStatementData()
 * @model
 * @generated
 */
public interface StatementData extends Statement
{
  /**
   * Returns the value of the '<em><b>Size</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Size</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Size</em>' attribute.
   * @see #setSize(String)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getStatementData_Size()
   * @model
   * @generated
   */
  String getSize();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.StatementData#getSize <em>Size</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Size</em>' attribute.
   * @see #getSize()
   * @generated
   */
  void setSize(String value);

  /**
   * Returns the value of the '<em><b>Data</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Data</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Data</em>' containment reference.
   * @see #setData(LExpression)
   * @see org.eclipse.cdt.linkerscript.linkerScript.LinkerScriptPackage#getStatementData_Data()
   * @model containment="true"
   * @generated
   */
  LExpression getData();

  /**
   * Sets the value of the '{@link org.eclipse.cdt.linkerscript.linkerScript.StatementData#getData <em>Data</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Data</em>' containment reference.
   * @see #getData()
   * @generated
   */
  void setData(LExpression value);

} // StatementData
