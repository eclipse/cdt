/*******************************************************************************
 * Copyright (c) 2000, 2010 IBM Corporation and others.
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
 *     Markus Schorn (Wind River Systems)
 *******************************************************************************/
package org.eclipse.cdt.internal.ui.typehierarchy;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;

import org.eclipse.cdt.core.model.ICElement;

import org.eclipse.cdt.internal.ui.viewsupport.CElementImageProvider;
import org.eclipse.cdt.internal.ui.viewsupport.CElementLabels;


/**
 * Action used for the include browser forward / backward buttons
 */
public class THHistoryAction extends Action {
	final static long LABEL_OPTIONS= 
		CElementLabels.M_PARAMETER_TYPES | 
		CElementLabels.ALL_FULLY_QUALIFIED |
		CElementLabels.MF_POST_FILE_QUALIFIED;
	
	private THViewPart fViewPart;
	private ICElement fElement;
	
	public THHistoryAction(THViewPart hierarchyView, ICElement element) {
        super("", AS_RADIO_BUTTON); //$NON-NLS-1$
		fViewPart= hierarchyView;
		fElement= element;		
	
		String elementName= CElementLabels.getElementLabel(element, LABEL_OPTIONS);
		setText(elementName);
		setImageDescriptor(getImageDescriptor(element));
	}
	
	private ImageDescriptor getImageDescriptor(ICElement elem) {
		CElementImageProvider imageProvider= new CElementImageProvider();
		ImageDescriptor desc= imageProvider.getBaseImageDescriptor(elem, 0);
		imageProvider.dispose();
		return desc;
	}
	
	/*
	 * @see Action#run()
	 */
	@Override
	public void run() {
		fViewPart.setInput(fElement, null);
	}
	
}
