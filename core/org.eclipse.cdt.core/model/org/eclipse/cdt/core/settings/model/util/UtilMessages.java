package org.eclipse.cdt.core.settings.model.util;

import java.util.MissingResourceException;
import java.util.ResourceBundle;

public class UtilMessages {
	private static final String BUNDLE_NAME = "org.eclipse.cdt.core.settings.model.util.UtilMessages"; //$NON-NLS-1$

	private static final ResourceBundle RESOURCE_BUNDLE = ResourceBundle
			.getBundle(BUNDLE_NAME);

	private UtilMessages() {
	}

	public static String getString(String key) {
		try {
			return RESOURCE_BUNDLE.getString(key);
		} catch (MissingResourceException e) {
			return '!' + key + '!';
		}
	}
}
