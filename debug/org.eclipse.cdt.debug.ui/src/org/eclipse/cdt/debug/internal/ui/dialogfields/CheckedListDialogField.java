/*******************************************************************************
 * Copyright (c) 2005, 2012 IBM Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.debug.internal.ui.dialogfields;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Assert;
import org.eclipse.jface.viewers.CheckStateChangedEvent;
import org.eclipse.jface.viewers.CheckboxTableViewer;
import org.eclipse.jface.viewers.ICheckStateListener;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Table;

/**
 * A list with checkboxes and a button bar. Typical buttons are 'Check All' and 'Uncheck All'.
 * List model is independend of widget creation.
 * DialogFields controls are: Label, List and Composite containing buttons.
 */
public class CheckedListDialogField extends ListDialogField {
	
	private int fCheckAllButtonIndex;
	private int fUncheckAllButtonIndex;
	
	private List fCheckElements;

	public CheckedListDialogField(IListAdapter adapter, String[] customButtonLabels, ILabelProvider lprovider) {
		super(adapter, customButtonLabels, lprovider);
		fCheckElements= new ArrayList();
		
		fCheckAllButtonIndex= -1;
		fUncheckAllButtonIndex= -1;
	}

	/**
	 * Sets the index of the 'check' button in the button label array passed in the constructor.
	 * The behaviour of the button marked as the check button will then be handled internally.
	 * (enable state, button invocation behaviour)
	 */	
	public void setCheckAllButtonIndex(int checkButtonIndex) {
		Assert.isTrue(checkButtonIndex < fButtonLabels.length);
		fCheckAllButtonIndex= checkButtonIndex;
	}

	/**
	 * Sets the index of the 'uncheck' button in the button label array passed in the constructor.
	 * The behaviour of the button marked as the uncheck button will then be handled internally.
	 * (enable state, button invocation behaviour)
	 */	
	public void setUncheckAllButtonIndex(int uncheckButtonIndex) {
		Assert.isTrue(uncheckButtonIndex < fButtonLabels.length);
		fUncheckAllButtonIndex= uncheckButtonIndex;
	}
	

	/*
	 * @see ListDialogField#createTableViewer
	 */
	@Override
	protected TableViewer createTableViewer(Composite parent) {
		Table table= new Table(parent, SWT.CHECK + getListStyle());
		CheckboxTableViewer tableViewer= new CheckboxTableViewer(table);
		tableViewer.addCheckStateListener(new ICheckStateListener() {
			@Override
			public void checkStateChanged(CheckStateChangedEvent e) {
				doCheckStateChanged(e);
			}
		});
		return tableViewer;
	}		
	
	
	/*
	 * @see ListDialogField#getListControl
	 */
	@Override
	public Control getListControl(Composite parent) {
		Control control= super.getListControl(parent);
		if (parent != null) {
			((CheckboxTableViewer)fTable).setCheckedElements(fCheckElements.toArray());
		}
		return control;
	}	
	
	/*
	 * @see DialogField#dialogFieldChanged
	 * Hooks in to get element changes to update check model.
	 */
	@Override
	public void dialogFieldChanged() {
		for (int i= fCheckElements.size() -1; i >= 0; i--) {
			if (!fElements.contains(fCheckElements.get(i))) {
				fCheckElements.remove(i);
			}
		}
		super.dialogFieldChanged();
	}	
	
	private void checkStateChanged() {
		//call super and do not update check model
		super.dialogFieldChanged();
	}		

	/**
	 * Gets the checked elements.
	 */
	public List getCheckedElements() {
		return new ArrayList(fCheckElements);
	}
	
	/**
	 * Returns true if the element is checked.
	 */
	public boolean isChecked(Object obj) {
		return fCheckElements.contains(obj);
	}	
	
	/**
	 * Sets the checked elements.
	 */	
	public void setCheckedElements(List list) {
		fCheckElements= new ArrayList(list);
		if (fTable != null) {
			((CheckboxTableViewer)fTable).setCheckedElements(list.toArray());
		}
		checkStateChanged();
	}

	/**
	 * Sets the checked state of an element.
	 */		
	public void setChecked(Object object, boolean state) {
		setCheckedWithoutUpdate(object, state);
		checkStateChanged();
	}
	
	/**
	 * Sets the checked state of an element. no dialog changed listener informed
	 */		
	public void setCheckedWithoutUpdate(Object object, boolean state) {
		if (state) {
			if (!fCheckElements.contains(object)) {
				fCheckElements.add(object);
			}
		}
		else {
			if (fCheckElements.contains(object)) {
				fCheckElements.remove(object);
			}
		}
		if (fTable != null) {
			((CheckboxTableViewer)fTable).setChecked(object, state);
		}
	}

	/**
	 * Sets the check state of all elements
	 */	
	public void checkAll(boolean state) {
		if (state) {
			fCheckElements= getElements();
		} else {
			fCheckElements.clear();
		}
		if (fTable != null) {
			((CheckboxTableViewer)fTable).setAllChecked(state);
		}
		checkStateChanged();
	}
	
			
	protected void doCheckStateChanged(CheckStateChangedEvent e) {
		if (e.getChecked()) {
			fCheckElements.add(e.getElement());
		} else {
			fCheckElements.remove(e.getElement());
		}		
		checkStateChanged();
	}
	
	// ------ enable / disable management
	
	/*
	 * @see ListDialogField#getManagedButtonState
	 */
	@Override
	protected boolean getManagedButtonState(ISelection sel, int index) {
		if (index == fCheckAllButtonIndex) {
			return !fElements.isEmpty();
		} else if (index == fUncheckAllButtonIndex) {
			return !fElements.isEmpty();
		}
		return super.getManagedButtonState(sel, index);
	}	

	/*
	 * @see ListDialogField#extraButtonPressed
	 */	
	@Override
	protected boolean managedButtonPressed(int index) {
		if (index == fCheckAllButtonIndex) {
			checkAll(true);
		} else if (index == fUncheckAllButtonIndex) {
			checkAll(false);
		} else {
			return super.managedButtonPressed(index);
		}
		return true;
	}
	
				
	
	

}
