/********************************************************************************
 * Copyright (c) 2002, 2007 IBM Corporation. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Initial Contributors:
 * The following IBM employees contributed to the Remote System Explorer
 * component that contains this file: David McKnight, Kushal Munir, 
 * Michael Berger, David Dykstal, Phil Coulthard, Don Yantzi, Eric Simpson, 
 * Emily Bruner, Mazen Faraj, Adrian Storisteanu, Li Ding, and Kent Hawley.
 * 
 * Contributors:
 * David Dykstal (IBM) - 168977: refactoring IConnectorService and ServerLauncher hierarchies
 ********************************************************************************/
package org.eclipse.rse.core.subsystems;

import org.eclipse.rse.core.model.IHost;

/**
 * This is a base class to make it easier to create connector service classes.
 * <p>
 * An {@link org.eclipse.rse.core.subsystems.IConnectorService} object
 * is returned from a subsystem object via getConnectorService(), and
 * it is used to represent the live connection to a particular subsystem.
 * <p>
 * You must override/implement
 * <ul>
 * <li>isConnected
 * <li>internalConnect
 * <li>internalDisconnect
 * </ul>
 * You should override:
 * <ul>
 * <li>reset 
 * <li>getVersionReleaseModification
 * <li>getHomeDirectory
 * <li>getTempDirectory
 * </ul>
 * You can override:
 * <ul>
 * <li>supportsUserId
 * <li>requiresUserId
 * <li>supportsPassword
 * <li>requiresPassword
 * </ul>
 * 
 * @see org.eclipse.rse.core.subsystems.AbstractConnectorServiceManager
 */
public abstract class BasicConnectorService extends SuperAbstractConnectorService {
	
	public BasicConnectorService(String name, String description, IHost host, int port) {
		super(name, description, host, port);
	}
	
	public boolean supportsPassword() {
		return false;
	}
	
	public boolean requiresPassword() {
		return false;
	}
	
	public boolean supportsUserId() {
		return false;
	}
	
	public boolean requiresUserId() {
		return false;
	}
	
	public void acquireCredentials(boolean refresh) throws InterruptedException {
	}
	
	public void clearCredentials() {
	}
	
	public void clearPassword(boolean persist, boolean propagate) {
	}
	
	public String getUserId() {
		return null;
	}
	
	public boolean hasPassword(boolean onDisk) {
		return false;
	}
	
	public boolean inheritsCredentials() {
		return false;
	}
	
	public boolean isSuppressed() {
		return false;
	}
	
	protected void postDisconnect() {
	}
	
	public void removePassword() {
	}
	
	public void removeUserId() {
	}
	
	public void savePassword() {
	}
	
	public void saveUserId() {
	}
	
	public void setPassword(String matchingUserId, String password, boolean persist, boolean propagate) {
	}
	
	public boolean sharesCredentials() {
		return false;
	}

	public void setSuppressed(boolean suppress) {
	}

	public void setUserId(String userId) {
	}
	
}