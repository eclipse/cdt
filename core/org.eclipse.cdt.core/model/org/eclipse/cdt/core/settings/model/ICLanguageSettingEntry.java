/*******************************************************************************
 * Copyright (c) 2007, 2010 Intel Corporation and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * Intel Corporation - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.core.settings.model;

/**
 * Representation in the project model of language settings entries
 * such as include paths (-I) or preprocessor defines (-D) and others (see
 * {@link ICSettingEntry#INCLUDE_PATH} and other kinds).
 */
public interface ICLanguageSettingEntry extends ICSettingEntry {
}
