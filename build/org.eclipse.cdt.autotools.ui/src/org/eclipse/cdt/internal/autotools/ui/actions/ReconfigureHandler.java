/*******************************************************************************
 * Copyright (c) 2009 Red Hat Inc..
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Red Hat Incorporated - initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.internal.autotools.ui.actions;

import org.eclipse.core.commands.ExecutionEvent;

/**
 * @author Jeff Johnston
 *
 */
public class ReconfigureHandler extends AbstractAutotoolsHandler {

	@Override
	public Object execute(ExecutionEvent event) {
		return execute(event, new ReconfigureAction());
	}

}
