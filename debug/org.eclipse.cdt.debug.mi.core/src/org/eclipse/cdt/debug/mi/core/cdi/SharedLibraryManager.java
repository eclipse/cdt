/*
 *(c) Copyright QNX Software Systems Ltd. 2002.
 * All Rights Reserved.
 * 
 */

package org.eclipse.cdt.debug.mi.core.cdi;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.debug.core.cdi.CDIException;
import org.eclipse.cdt.debug.core.cdi.ICDIConfiguration;
import org.eclipse.cdt.debug.core.cdi.ICDISharedLibraryManager;
import org.eclipse.cdt.debug.core.cdi.model.ICDISharedLibrary;
import org.eclipse.cdt.debug.mi.core.MIException;
import org.eclipse.cdt.debug.mi.core.MISession;
import org.eclipse.cdt.debug.mi.core.cdi.model.SharedLibrary;
import org.eclipse.cdt.debug.mi.core.command.CommandFactory;
import org.eclipse.cdt.debug.mi.core.command.MIGDBSetAutoSolib;
import org.eclipse.cdt.debug.mi.core.command.MIGDBSetSolibSearchPath;
import org.eclipse.cdt.debug.mi.core.command.MIGDBShowSolibSearchPath;
import org.eclipse.cdt.debug.mi.core.command.MIInfoSharedLibrary;
import org.eclipse.cdt.debug.mi.core.command.MISharedLibrary;
import org.eclipse.cdt.debug.mi.core.event.MIEvent;
import org.eclipse.cdt.debug.mi.core.event.MISharedLibChangedEvent;
import org.eclipse.cdt.debug.mi.core.event.MISharedLibCreatedEvent;
import org.eclipse.cdt.debug.mi.core.event.MISharedLibUnloadedEvent;
import org.eclipse.cdt.debug.mi.core.output.MIGDBShowSolibSearchPathInfo;
import org.eclipse.cdt.debug.mi.core.output.MIInfo;
import org.eclipse.cdt.debug.mi.core.output.MIInfoSharedLibraryInfo;
import org.eclipse.cdt.debug.mi.core.output.MIShared;

/**
 * Manager of the CDI shared libraries.
 */
public class SharedLibraryManager extends SessionObject implements ICDISharedLibraryManager {

	List sharedList;
	boolean autoupdate;

	public SharedLibraryManager (Session session) {
		super(session);
		sharedList = new ArrayList(1);
		autoupdate = true;
	}

	MIShared[] getMIShareds() throws CDIException {
		MIShared[] miLibs = new MIShared[0];
		Session session = (Session)getSession();
		CommandFactory factory = session.getMISession().getCommandFactory();
		MIInfoSharedLibrary infoShared = factory.createMIInfoSharedLibrary();
		try {
			session.getMISession().postCommand(infoShared);
			MIInfoSharedLibraryInfo info = infoShared.getMIInfoSharedLibraryInfo();
			if (info == null) {
				throw new CDIException("No answer");
			}
			miLibs = info.getMIShared();
		} catch (MIException e) {
			throw new MI2CDIException(e);
		}
		return miLibs;
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDISharedLibraryManager#update()
	 */
	public void update() throws CDIException {
		Session session = (Session)getSession();
		ICDIConfiguration conf = session.getConfiguration();
		if (!conf.supportsSharedLibrary()) {
			return; // Bail out early;
		}

		MIShared[] miLibs = getMIShareds();
		List eventList = new ArrayList(miLibs.length);
		for (int i = 0; i < miLibs.length; i++) {
			ICDISharedLibrary sharedlib = getSharedLibrary(miLibs[i].getName());
			if (sharedlib != null) {
				if (hasSharedLibChanged(sharedlib, miLibs[i])) {
					// Fire ChangedEvent
					((SharedLibrary)sharedlib).setMIShared(miLibs[i]);
					eventList.add(new MISharedLibChangedEvent(miLibs[i].getName())); 
				}
			} else {
				// add the new breakpoint and fire CreatedEvent
				sharedList.add(new SharedLibrary(this, miLibs[i]));
				eventList.add(new MISharedLibCreatedEvent(miLibs[i].getName())); 
			}
		}
		// Check if any libraries was unloaded.
		ICDISharedLibrary[] oldlibs = (ICDISharedLibrary[])sharedList.toArray(new ICDISharedLibrary[0]);
		for (int i = 0; i < oldlibs.length; i++) {
			boolean found = false;
			for (int j = 0; j < miLibs.length; j++) {
				if (miLibs[j].getName().equals(oldlibs[i].getFileName())) {
					found = true;
					break;
				}
			}
			if (!found) {
				// Fire destroyed Events.
				eventList.add(new MISharedLibUnloadedEvent(oldlibs[i].getFileName())); 
			}
		}
		MISession mi = session.getMISession();
		MIEvent[] events = (MIEvent[])eventList.toArray(new MIEvent[0]);
		mi.fireEvents(events);
	}

	public boolean hasSharedLibChanged(ICDISharedLibrary lib, MIShared miLib) {
		return !miLib.getName().equals(lib.getFileName()) ||
			miLib.getFrom() != lib.getStartAddress() ||
			miLib.getTo() != lib.getEndAddress() ||
			miLib.isRead() != lib.areSymbolsLoaded();
	}

	public void deleteSharedLibrary(ICDISharedLibrary lib) {
		sharedList.remove(lib);
	}

	public ICDISharedLibrary getSharedLibrary(String name) {
		ICDISharedLibrary[] libs = (ICDISharedLibrary[])sharedList.toArray(new ICDISharedLibrary[0]);
		for (int i = 0; i < libs.length; i++) {
			if (name.equals(libs[i].getFileName())) {
					return libs[i];
			}
		}
		return null;
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDISharedLibraryManager#setSharedLibraryPaths(String[])
	 */
	public void setAutoLoadSymbols(boolean set) throws CDIException {
		Session session = (Session)getSession();
		MISession mi = session.getMISession();
		CommandFactory factory = mi.getCommandFactory();
		MIGDBSetAutoSolib solib = factory.createMIGDBSetAutoSolib(set);
		try {
			mi.postCommand(solib);
			solib.getMIInfo();
		} catch (MIException e) {
			throw new MI2CDIException(e);
		}
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDISharedLibraryManager#setSharedLibraryPaths(String[])
	 */
	public void setSharedLibraryPaths(String[] libPaths) throws CDIException {
		Session session = (Session)getSession();
		MISession mi = session.getMISession();
		CommandFactory factory = mi.getCommandFactory();
		MIGDBSetSolibSearchPath solib = factory.createMIGDBSetSolibSearchPath(libPaths);
		try {
			mi.postCommand(solib);
			solib.getMIInfo();
		} catch (MIException e) {
			throw new MI2CDIException(e);
		}
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDISharedLibraryManager#getSharedLibraryPaths()
	 */
	public String[] getSharedLibraryPaths() throws CDIException {
		Session session = (Session)getSession();
		MISession mi = session.getMISession();
		CommandFactory factory = mi.getCommandFactory();
		MIGDBShowSolibSearchPath dir = factory.createMIGDBShowSolibSearchPath();
		try {
			mi.postCommand(dir);
			MIGDBShowSolibSearchPathInfo info = dir.getMIGDBShowSolibSearchPathInfo();
			return info.getDirectories();
		} catch (MIException e) {
			throw new MI2CDIException(e);
		}
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDISharedLibraryManager#getSharedLibraries()
	 */
	public ICDISharedLibrary[] getSharedLibraries() throws CDIException {
		return (ICDISharedLibrary[])sharedList.toArray(new ICDISharedLibrary[0]);
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDISharedLibraryManager#loadSymbols()
	 */
	public void loadSymbols() throws CDIException {
		Session session = (Session)getSession();
		MISession mi = session.getMISession();
		CommandFactory factory = mi.getCommandFactory();
		MISharedLibrary sharedlibrary = factory.createMISharedLibrary();
		try {
			mi.postCommand(sharedlibrary);
			MIInfo info = sharedlibrary.getMIInfo();
			if (info == null) {
				throw new CDIException("No answer");
			}
		} catch (MIException e) {
			throw new MI2CDIException(e);
		}
		update();
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDISharedLibraryManager#loadSymbols(ICDISharedLibrary[])
	 */
	public void loadSymbols(ICDISharedLibrary[] libs) throws CDIException {
		Session session = (Session)getSession();
		MISession mi = session.getMISession();
		CommandFactory factory = mi.getCommandFactory();
		for (int i = 0; i < libs.length; i++) {
			if (libs[i].areSymbolsLoaded()) {
				continue;
			}
			MISharedLibrary sharedlibrary = factory.createMISharedLibrary(libs[i].getFileName());
			try {
				session.getMISession().postCommand(sharedlibrary);
				MIInfo info = sharedlibrary.getMIInfo();
				if (info == null) {
					throw new CDIException("No answer");
				}
			} catch (MIException e) {
				throw new MI2CDIException(e);
			}
			mi.fireEvent(new MISharedLibChangedEvent(libs[i].getFileName()));
		}
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDISharedLibraryManager#isAutoUpdate()
	 */
	public boolean isAutoUpdate() {
		return autoupdate;
	}

	/**
	 * @see org.eclipse.cdt.debug.core.cdi.ICDISharedLibraryManager#setAutoUpdate(boolean)
	 */
	public void setAutoUpdate(boolean update) {
		autoupdate = update;
	}

}
