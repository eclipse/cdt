/*******************************************************************************
 * Copyright (c) 2000, 2014 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     Wind River Systems   - Modified for new DSF Reference Implementation
 *     Vladimir Prus (Mentor Graphics) - Add getMIValue.
 *******************************************************************************/

package org.eclipse.cdt.dsf.mi.service.command.output;

/**
 * GDB/MI ResultRecord.
 */
public class MIResultRecord {

    public final static String DONE ="done"; //$NON-NLS-1$
    public final static String RUNNING ="running"; //$NON-NLS-1$
    public final static String CONNECTED ="connected"; //$NON-NLS-1$
    public final static String ERROR ="error"; //$NON-NLS-1$
    public final static String EXIT ="exit"; //$NON-NLS-1$

    String resultClass = ""; //$NON-NLS-1$
    int token = -1;
    MITuple value = new MITuple();

    public int getToken() {
        return token;
    }

    public void setToken(int t) {
        token = t;
    }

    /**
     */
    public String getResultClass() {
        return resultClass;
    }

    public void setResultClass(String type) {
        resultClass = type;
    }

    public MIResult[] getMIResults() {
        return value.getMIResults();
    }

    public void setMIResults(MIResult[] res) {
        value.setMIResults(res);
    }

    /** Return the value of the named field in this record.
	 * @since 4.6
	 */
    public MIValue getField(String name) {
        return value.getField(name);
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();
        if (token > 0) {
            buffer.append(token);
        }
        buffer.append('^').append(resultClass);

        if (value.getMIResults().length != 0)
            buffer.append(value.toString(",", "")); //$NON-NLS-1$ //$NON-NLS-2$
        return buffer.toString();
    }
}
