/*******************************************************************************
 * Copyright (c) 2002, 2009 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * QNX Software Systems - Initial API and implementation
 *******************************************************************************/
package org.eclipse.cdt.utils.macho.parser.internal;
 
import java.io.IOException;

import org.eclipse.cdt.core.AbstractCExtension;
import org.eclipse.cdt.core.CCorePlugin;
import org.eclipse.cdt.core.IBinaryParser;
import org.eclipse.cdt.core.ICExtensionReference;
import org.eclipse.cdt.utils.CPPFilt;
import org.eclipse.cdt.utils.macho.AR;
import org.eclipse.cdt.utils.macho.parser.MachOBinaryArchive;
import org.eclipse.cdt.utils.macho.parser.internal.MachO64.Attribute;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;

/**
 *  This file is a temporary solution to work around API restrictions for CDT 6.0.X. 
 *  This class is non-API in CDT 6.0.X and is not intended to be referenced.
 *  
 * @noextend This class is not intended to be subclassed by clients.
 * @noinstantiate This class is not intended to be instantiated by clients.
 */
public class MachOParser64 extends AbstractCExtension implements IBinaryParser {

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.IBinaryParser#getBinary(org.eclipse.core.runtime.IPath)
	 */
	public IBinaryFile getBinary(IPath path) throws IOException {
		return getBinary(null, path);
	}


	public IBinaryFile getBinary(byte[] hints, IPath path) throws IOException {
		if (path == null) {
			throw new IOException(CCorePlugin.getResourceString("Util.exception.nullPath")); //$NON-NLS-1$
		}

		IBinaryFile binary = null;
		try {
			MachO64.Attribute attribute = null;
			if (hints != null && hints.length > 0) {
				try {
					attribute = MachO64.getAttributes(hints);
				} catch (IOException eof) {
					// continue, the array was to small.
				}
			}

			//Take a second run at it if the data array failed. 			
 			if(attribute == null) {
				attribute = MachO64.getAttributes(path.toOSString());
 			}

			if (attribute != null) {
				switch (attribute.getType()) {
					case Attribute.MACHO_TYPE_EXE :
						binary = createBinaryExecutable(path);
						break;

					case Attribute.MACHO_TYPE_SHLIB :
						binary = createBinaryShared(path);
						break;

					case Attribute.MACHO_TYPE_OBJ :
						binary = createBinaryObject(path);
						break;

					case Attribute.MACHO_TYPE_CORE :
						binary = createBinaryCore(path);
						break;
				}
			}
		} catch (IOException e) {
			binary = createBinaryArchive(path);
		}
		return binary;
	}

	/**
	 * @see org.eclipse.cdt.core.IBinaryParser#getFormat()
	 */
	public String getFormat() {
		return "MACHO"; //$NON-NLS-1$
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.IBinaryParser#isBinary(byte[], org.eclipse.core.runtime.IPath)
	 */
	public boolean isBinary(byte[] array, IPath path) {
		return MachO64.isMachOHeader(array) || AR.isARHeader(array);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.core.IBinaryParser#getBufferSize()
	 */
	public int getHintBufferSize() {
		return 128;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.utils.IGnuToolProvider#getCPPFilt()
	 */
	public CPPFilt getCPPFilt() {
		IPath cppFiltPath = getCPPFiltPath();
		CPPFilt cppfilt = null;
		if (cppFiltPath != null && ! cppFiltPath.isEmpty()) {
			try {
				cppfilt = new CPPFilt(cppFiltPath.toOSString());
			} catch (IOException e2) {
			}
		}
		return cppfilt;
	}

	@SuppressWarnings("deprecation")
	protected IPath getCPPFiltPath() {
		ICExtensionReference ref = getExtensionReference();
		String value = ref.getExtensionData("c++filt"); //$NON-NLS-1$
		if (value == null || value.length() == 0) {
			value = "c++filt"; //$NON-NLS-1$
		}
		return new Path(value);
	}

	protected IBinaryArchive createBinaryArchive(IPath path) throws IOException {
		return new MachOBinaryArchive(this, path);
	}

	protected IBinaryObject createBinaryObject(IPath path) throws IOException {
		return new MachOBinaryObject64(this, path, IBinaryFile.OBJECT);
	}

	protected IBinaryExecutable createBinaryExecutable(IPath path) throws IOException {
		return new MachOBinaryExecutable64(this, path);
	}

	protected IBinaryShared createBinaryShared(IPath path) throws IOException {
		return new MachOBinaryShared64(this, path);
	}

	protected IBinaryObject createBinaryCore(IPath path) throws IOException {
		return new MachOBinaryObject64(this, path, IBinaryFile.CORE);
	}
}
