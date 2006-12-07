/********************************************************************************
 * Copyright (c) 2006 Symbian Software Ltd. All rights reserved.
 * This program and the accompanying materials are made available under the terms
 * of the Eclipse Public License v1.0 which accompanies this distribution, and is 
 * available at http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *   Javier Montalvo Orus (Symbian) - initial API and implementation
 ********************************************************************************/

package org.eclipse.tm.discovery.transport;


/**
 * Interface for service discovery transports
 * 
 */
public interface ITransport {

	/**
	 * Handles sending data
	 * 
	 * @param data
	 * Data packet to be sent.
	 * @throws IOException
	 */
	public abstract void send(byte[] packet) throws Exception;

	/**
	 * Handles receiving data
	 * 
	 * @param packet
	 * Packet to be filled with the reply data.
	 * @return 
	 * Address of the replying device.
	 * @throws IOException
	 */
	public abstract String receive(byte[] packet) throws Exception;
	
	
	/**
	 * Sets the address of the target, depending on the implementation (IP, port...)
	 * @param address
	 * Address or identifier of the target.
	 * @throws Exception
	 * 
	 */
	public abstract void setTargetAddress(String address) throws Exception;
	
	/**
	 * Sets the timeout for the transport implementation.
	 * @param timeOut
	 * The timeout in milliseconds
	 * @throws Exception
	 */
	public abstract void setTimeOut(int timeOut) throws Exception;

	
}
