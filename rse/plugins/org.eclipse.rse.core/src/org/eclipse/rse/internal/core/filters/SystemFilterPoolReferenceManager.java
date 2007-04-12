/********************************************************************************
 * Copyright (c) 2002, 2006 IBM Corporation. All rights reserved.
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
 * {Name} (company) - description of contribution.
 ********************************************************************************/

package org.eclipse.rse.internal.core.filters;

import java.util.Vector;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.rse.core.SystemResourceHelpers;
import org.eclipse.rse.core.filters.IRSEFilterNamingPolicy;
import org.eclipse.rse.core.filters.ISystemFilter;
import org.eclipse.rse.core.filters.ISystemFilterPool;
import org.eclipse.rse.core.filters.ISystemFilterPoolManager;
import org.eclipse.rse.core.filters.ISystemFilterPoolManagerProvider;
import org.eclipse.rse.core.filters.ISystemFilterPoolReference;
import org.eclipse.rse.core.filters.ISystemFilterPoolReferenceManager;
import org.eclipse.rse.core.filters.ISystemFilterPoolReferenceManagerProvider;
import org.eclipse.rse.core.filters.ISystemFilterReference;
import org.eclipse.rse.core.filters.ISystemFilterSavePolicies;
import org.eclipse.rse.core.filters.SystemFilterNamingPolicy;
import org.eclipse.rse.core.references.IRSEBasePersistableReferencingObject;
import org.eclipse.rse.core.subsystems.ISubSystem;
import org.eclipse.rse.internal.references.SystemPersistableReferenceManager;

/**
 * This class manages a persistable list of objects each of which reference
 * a filter pool. This class builds on the parent class SystemPersistableReferenceManager,
 * offering convenience versions of the parent methods that are typed to the
 * classes in the filters framework.
 * 
 * There will be one of these instantiated for a subsystem. Filter pool references can 
 * be moved within a subsystem and this manager provides that function as well.
 */
/** 
 * @lastgen class SystemFilterPoolReferenceManagerImpl extends SystemPersistableReferenceManagerImpl implements SystemFilterPoolReferenceManager, SystemPersistableReferenceManager {}
 */
public class SystemFilterPoolReferenceManager extends SystemPersistableReferenceManager implements ISystemFilterPoolReferenceManager {
	//private SystemFilterPoolManager[]                poolMgrs = null;
	private ISystemFilterPoolManagerProvider poolMgrProvider = null;
	private ISystemFilterPoolManager defaultPoolMgr = null;
	private ISystemFilterPoolReferenceManagerProvider caller = null;
	private IRSEFilterNamingPolicy namingPolicy = null;
	private int savePolicy = ISystemFilterSavePolicies.SAVE_POLICY_NONE;
	private Object mgrData = null;
	private IFolder mgrFolder = null;
	private boolean initialized = false;
	private boolean noSave;
	private boolean noEvents;
	private boolean fireEvents = true;
	private ISystemFilterPoolReference[] fpRefsArray = null;
	private static final ISystemFilterPoolReference[] emptyFilterPoolRefArray = new ISystemFilterPoolReference[0];

	/**
	 * Default constructor. Typically called by MOF factory methods.
	 */
	public SystemFilterPoolReferenceManager() {
		super();
	}

	/**
	 * A factory method to create a SystemFilterPoolReferenceManager instance.
	 * @param caller Objects which instantiate this class should implement the
	 *   SystemFilterPoolReferenceManagerProvider interface, and pass "this" for this parameter.
	 *   Given any filter framework object, it is possible to retrieve the caller's
	 *   object via the getProvider method call.
	 * @param relatedPoolManagerProvider The managers that owns the master list of filter pools that 
	 *   this manager will contain references to.
	 * @param mgrFolder the folder that will hold the persisted file. This is used when
	 *   the save policy is SAVE_POLICY_ONE_FILE_PER_MANAGER. For SAVE_POLICY_NONE, this
	 *   is not used. If it is used, it is created if it does not already exist.
	 * @param name the name of the filter pool reference manager. This is used when 
	 *   the save policy is SAVE_POLICY_ONE_FILE_PER_MANAGER, to deduce the file name.
	 * @param savePolicy The save policy for the filter pool references list. One of the
	 *   following from the {@link org.eclipse.rse.internal.core.filters.ISystemFilterConstants SystemFilterConstants} 
	 *   interface:
	 *   <ul>
	 *     <li>SAVE_POLICY_NONE - no files, all save/restore handled elsewhere
	 *     <li>SAVE_POLICY_ONE_FILE_PER_MANAGER - one file: mgrName.xmi
	 *   </ul> 
	 * @param namingPolicy The names to use for file and folders when persisting to disk. Pass
	 *     null to just use the defaults, or if using SAVE_POLICY_NONE.
	 * @return a filter pool reference manager
	 */
	public static ISystemFilterPoolReferenceManager createSystemFilterPoolReferenceManager(ISystemFilterPoolReferenceManagerProvider caller,
			ISystemFilterPoolManagerProvider relatedPoolManagerProvider, IFolder mgrFolder, String name, int savePolicy, IRSEFilterNamingPolicy namingPolicy) {
		SystemFilterPoolReferenceManager mgr = null;

		if (mgrFolder != null) SystemResourceHelpers.getResourceHelpers().ensureFolderExists(mgrFolder);
		if (namingPolicy == null) namingPolicy = SystemFilterNamingPolicy.getNamingPolicy();
		try {
			if (savePolicy != ISystemFilterSavePolicies.SAVE_POLICY_NONE) mgr = (SystemFilterPoolReferenceManager) restore(caller, mgrFolder, name, namingPolicy);
		} catch (Exception exc) // real error trying to restore, versus simply not found.
		{
			// todo: something. Log the exception somewhere?
		}
		if (mgr == null) // not found or some serious error.
		{
			mgr = createManager();
		}
		if (mgr != null) {
			mgr.initialize(caller, mgrFolder, name, savePolicy, namingPolicy, relatedPoolManagerProvider);
		}

		return mgr;
	}

	/*
	 * Private helper method.
	 */
	protected static SystemFilterPoolReferenceManager createManager() {
		ISystemFilterPoolReferenceManager mgr = new SystemFilterPoolReferenceManager();
		return (SystemFilterPoolReferenceManager) mgr;
	}

	/*
	 * Private helper method to initialize state
	 */
	protected void initialize(ISystemFilterPoolReferenceManagerProvider caller, IFolder folder, String name, int savePolicy, IRSEFilterNamingPolicy namingPolicy,
			ISystemFilterPoolManagerProvider relatedPoolManagerProvider) {
		if (!initialized) initialize(caller, folder, name, savePolicy, namingPolicy); // core data
		//setSystemFilterPoolManagers(relatedPoolManagers);
		setSystemFilterPoolManagerProvider(relatedPoolManagerProvider);
	}

	/*
	 * Private helper method to do core initialization.
	 * Might be called from either the static factory method or the static restore method.
	 */
	protected void initialize(ISystemFilterPoolReferenceManagerProvider caller, IFolder folder, String name, int savePolicy, IRSEFilterNamingPolicy namingPolicy) {
		this.mgrFolder = folder;
		setProvider(caller);
		setName(name);
		this.savePolicy = savePolicy;
		setNamingPolicy(namingPolicy);
		initialized = true;
	}

	private void invalidateFilterPoolReferencesCache() {
		fpRefsArray = null;
		invalidateCache();
	}

	// ------------------------------------------------------------
	// Methods for setting and querying attributes
	// ------------------------------------------------------------
	/**
	 * Set the associated master pool manager provider. Note the provider
	 * typically manages multiple pool managers and we manage references
	 * across those.
	 * @param poolMgrProvider the factory (provider) for the filter pool managers that this 
	 * reference manager provides services to
	 */
	public void setSystemFilterPoolManagerProvider(ISystemFilterPoolManagerProvider poolMgrProvider) {
		this.poolMgrProvider = poolMgrProvider;
	}

	/**
	 * @return the associated master pool manager provider. Note the provider
	 * typically manages multiple pool managers and we manage references
	 * across those.
	 */
	public ISystemFilterPoolManagerProvider getSystemFilterPoolManagerProvider() {
		return poolMgrProvider;
	}

	/**
	 * @return the managers of the master list of filter pools, from which
	 * objects in this list reference.
	 */
	public ISystemFilterPoolManager[] getSystemFilterPoolManagers() {
		return poolMgrProvider.getSystemFilterPoolManagers();
	}

	/**
	 * @return the managers of the master list of filter pools, from which
	 * objects in this list reference, but which are not in the list of
	 * managers our pool manager supplier gives us. That is, these are
	 * references to filter pools outside the expected list.
	 */
	public ISystemFilterPoolManager[] getAdditionalSystemFilterPoolManagers() {
		ISystemFilterPoolManager[] poolMgrs = getSystemFilterPoolManagers();
		Vector v = new Vector();
		ISystemFilterPoolReference[] fpRefs = getSystemFilterPoolReferences();
		for (int idx = 0; idx < fpRefs.length; idx++) {
			ISystemFilterPool pool = fpRefs[idx].getReferencedFilterPool();
			if (pool != null) {
				ISystemFilterPoolManager mgr = pool.getSystemFilterPoolManager();
				if (!managerExists(poolMgrs, mgr) && !v.contains(mgr)) {
					System.out.println("Found unmatched manager: " + mgr.getName()); //$NON-NLS-1$
					v.addElement(mgr);
				}
			}
		}
		ISystemFilterPoolManager[] additionalMgrs = null;
		if (v.size() > 0) {
			additionalMgrs = new ISystemFilterPoolManager[v.size()];
			for (int idx = 0; idx < v.size(); idx++)
				additionalMgrs[idx] = (ISystemFilterPoolManager) v.elementAt(idx);
		}
		return additionalMgrs;
	}

	/**
	 * Look for a pool manager in an array of pool managers.
	 * @param mgrs the array in which to look
	 * @param mgr the item to look for
	 * @return true if the manager was found
	 */
	private boolean managerExists(ISystemFilterPoolManager[] mgrs, ISystemFilterPoolManager mgr) {
		boolean match = false;
		for (int idx = 0; !match && (idx < mgrs.length); idx++)
			if (mgr == mgrs[idx]) match = true;
		return match;
	}

	/**
	 * Set the default manager of the master list of filter pools, from which
	 * objects in this list reference.
	 * @param mgr the filter pool manager that is the default pool manager.
	 */
	public void setDefaultSystemFilterPoolManager(ISystemFilterPoolManager mgr) {
		defaultPoolMgr = mgr;
	}

	/**
	 * @return the default manager of the master list of filter pools, from which
	 * objects in this list reference.
	 */
	public ISystemFilterPoolManager getDefaultSystemFilterPoolManager() {
		return defaultPoolMgr;
	}

	/**
	 * @return the object (the "provider" or factory) which instantiated 
	 * this instance of the filter pool reference manager.
	 * This is also available from any filter reference framework object.
	 */
	public ISystemFilterPoolReferenceManagerProvider getProvider() {
		return caller;
	}

	/**
	 * Set the object which instantiated this instance of the filter pool reference manager.
	 * This makes it available to retrieve from any filter reference framework object,
	 * via the ubiquitous getProvider interface method.
	 * @param caller the factory that created this instance.
	 */
	public void setProvider(ISystemFilterPoolReferenceManagerProvider caller) {
		this.caller = caller;
	}

	/**
	 * Turn callbacks to the provider either off or on.
	 * @param fireEvents true if events are to be fired to the provider object, false if not.
	 */
	public void setProviderEventNotification(boolean fireEvents) {
		this.fireEvents = fireEvents;
	}

	/**
	 * Set the naming policy used when saving data to disk.
	 * @see org.eclipse.rse.core.filters.IRSEFilterNamingPolicy
	 * @param namingPolicy the naming policy - no longer used.
	 */
	public void setNamingPolicy(IRSEFilterNamingPolicy namingPolicy) {
		this.namingPolicy = namingPolicy;
	}

	/**
	 * Get the naming policy currently used when saving data to disk.
	 * @see org.eclipse.rse.core.filters.IRSEFilterNamingPolicy
	 * @return the naming policy - no longer used.
	 */
	public IRSEFilterNamingPolicy getNamingPolicy() {
		return namingPolicy;
	}

	/**
	 * This is to set transient data that is subsequently queryable.
	 * @param data the data associated with this pool reference manager.
	 */
	public void setSystemFilterPoolReferenceManagerData(Object data) {
		this.mgrData = data;
	}

	/**
	 * @return transient data set via setFilterPoolData.
	 */
	public Object getSystemFilterPoolReferenceManagerData() {
		return mgrData;
	}

	/**
	 * Set the name. This is an override of mof-generated method
	 * in order to potentially rename the disk file for a save
	 * policy of SAVE_POLICY_ONE_FILE_PER_MANAGER.
	 * No longer used.
	 * @param name the name of this reference manager.
	 */
	public void setName(String name) {
		if (savePolicy == ISystemFilterSavePolicies.SAVE_POLICY_ONE_FILE_PER_MANAGER) {
			IFile file = getResourceHelpers().getFile(getFolder(), getSaveFileName());
			super.setName(name);
			String newFileName = getSaveFileName();
			try {
				getResourceHelpers().renameFile(file, newFileName);
			} catch (Exception exc) {
			}
		} else
			super.setName(name);
	}

	// ---------------------------------------------------
	// Methods that work on FilterPool referencing objects
	// ---------------------------------------------------
	/**
	 * Ask each referenced pool for its name, and update it.
	 * Called after the name of the pool or its manager changes.
	 */
	public void regenerateReferencedSystemFilterPoolNames() {
		ISystemFilterPoolReference[] fpRefs = getSystemFilterPoolReferences();
		for (int idx = 0; idx < fpRefs.length; idx++) {
			ISystemFilterPool pool = fpRefs[idx].getReferencedFilterPool();
			if (pool != null) fpRefs[idx].resetReferencedFilterPoolName(pool.getReferenceName());
		}
		invalidateFilterPoolReferencesCache(); // just in case!
		quietSave();
	}

	/**
	 * @return array of SystemFilterPoolReference objects.
	 * Result will never be null, although it may be an array of length zero.
	 */
	public ISystemFilterPoolReference[] getSystemFilterPoolReferences() {
		IRSEBasePersistableReferencingObject[] refObjs = super.getReferencingObjects();
		if (refObjs.length == 0)
			return emptyFilterPoolRefArray;
		else if ((fpRefsArray == null) || (fpRefsArray.length != refObjs.length)) {
			fpRefsArray = new ISystemFilterPoolReference[refObjs.length];
			for (int idx = 0; idx < fpRefsArray.length; idx++)
				fpRefsArray[idx] = (ISystemFilterPoolReference) refObjs[idx];
		}
		return fpRefsArray;
	}

	/**
	 * In one shot, set the filter pool references. Calls back to inform provider.
	 * @param filterPoolReferences an array of filter pool reference objects to set the list to.
	 * @param deReference true to first de-reference all objects in the existing list.
	 */
	public void setSystemFilterPoolReferences(ISystemFilterPoolReference[] filterPoolReferences, boolean deReference) {
		super.setReferencingObjects(filterPoolReferences, deReference);
		invalidateFilterPoolReferencesCache();
		// callback to provider so they can fire events in their GUI
		if (fireEvents && (caller != null)) caller.filterEventFilterPoolReferencesReset();
		quietSave();
	}

	/**
	 * Create a filter pool reference. This creates a raw reference that must be added to the managed
	 * lists by the caller.
	 */
	private ISystemFilterPoolReference createSystemFilterPoolReference(ISystemFilterPool filterPool) {
		ISystemFilterPoolReference filterPoolReference = new SystemFilterPoolReference(filterPool);
		invalidateFilterPoolReferencesCache();
		return filterPoolReference;
	}

	/**
	 * Create a filter pool reference. This creates an unresolved raw reference that
	 * must be added to the managed lists by the caller.
	 * That will be attempted to be resolved on first use.
	 */
	private ISystemFilterPoolReference createSystemFilterPoolReference(ISystemFilterPoolManager filterPoolManager, String filterPoolName) {
		ISystemFilterPoolReference filterPoolReference = new SystemFilterPoolReference(filterPoolManager, filterPoolName);
		invalidateFilterPoolReferencesCache();
		return filterPoolReference;
	}

	/**
	 * Add a filter pool referencing object to the list. 
	 * @param filterPoolReference a reference to add to this manager
	 * @return the new count of referencing objects
	 */
	public int addSystemFilterPoolReference(ISystemFilterPoolReference filterPoolReference) {
		int count = addReferencingObject(filterPoolReference);
		filterPoolReference.setParentReferenceManager(this); // DWD - should be done in the addReferencingObject method?
		invalidateFilterPoolReferencesCache();
		quietSave();
		return count;
	}

	/**
	 * Reset the filter pool a reference points to. Called on a move-filter-pool operation
	 * @param filterPoolReference the reference to fix up
	 * @param newPool the new pool to reference
	 */
	public void resetSystemFilterPoolReference(ISystemFilterPoolReference filterPoolReference, ISystemFilterPool newPool) {
		filterPoolReference.removeReference();
		filterPoolReference.setReferencedObject(newPool);
		if (fireEvents && (caller != null)) caller.filterEventFilterPoolReferenceReset(filterPoolReference);
		quietSave();
	}

	/**
	 * Remove a filter pool referencing object from the list.
	 * @param filterPoolReference the reference to remove
	 * @param deReference true if we want to dereference the referenced object (call removeReference on it)
	 * @return the new count of referencing objects
	 */
	public int removeSystemFilterPoolReference(ISystemFilterPoolReference filterPoolReference, boolean deReference) {
		int count = 0;
		if (!deReference)
			count = super.removeReferencingObject(filterPoolReference);
		else
			count = super.removeAndDeReferenceReferencingObject(filterPoolReference);
		filterPoolReference.setParentReferenceManager(null); // DWD should be done in remove?
		invalidateFilterPoolReferencesCache();
		if (fireEvents && (caller != null)) caller.filterEventFilterPoolReferenceDeleted(filterPoolReference);
		quietSave();
		return count;
	}

	/**
	 * @return count of referenced filter pools
	 */
	public int getSystemFilterPoolReferenceCount() {
		return super.getReferencingObjectCount();
	}

	/**
	 * @param filterPoolRef the filter pool reference to search for
	 * @return the zero-based position of the reference within this manager
	 */
	public int getSystemFilterPoolReferencePosition(ISystemFilterPoolReference filterPoolRef) {
		return super.getReferencingObjectPosition(filterPoolRef);
	}

	/**
	 * Move a given filter pool reference to a given zero-based location.
	 * Calls back to inform provider of the event.
	 * @param filterPoolRef the reference to move
	 * @param pos the new position at which to move it. References at that position and beyond are
	 * moved up in the list.
	 */
	public void moveSystemFilterPoolReference(ISystemFilterPoolReference filterPoolRef, int pos) {
		int oldPos = super.getReferencingObjectPosition(filterPoolRef);
		super.moveReferencingObjectPosition(pos, filterPoolRef);
		invalidateFilterPoolReferencesCache();
		if (!noSave) quietSave();
		if (fireEvents && (caller != null) && !noEvents) {
			ISystemFilterPoolReference[] refs = new ISystemFilterPoolReference[1];
			refs[0] = filterPoolRef;
			caller.filterEventFilterPoolReferencesRePositioned(refs, pos - oldPos);
		}
	}

	/**
	 * Move existing filter pool references a given number of positions.
	 * If the delta is negative, they are all moved up by the given amount. If 
	 * positive, they are all moved down by the given amount.
	 * Calls back to inform provider.
	 * @param filterPoolRefs Array of SystemFilterPoolReferences to move.
	 * @param delta the amount by which to move these references.
	 */
	public void moveSystemFilterPoolReferences(ISystemFilterPoolReference[] filterPoolRefs, int delta) {
		int[] oldPositions = new int[filterPoolRefs.length];
		noEvents = noSave = true;
		for (int idx = 0; idx < filterPoolRefs.length; idx++)
			oldPositions[idx] = getSystemFilterPoolReferencePosition(filterPoolRefs[idx]);
		if (delta > 0) // moving down, process backwards
			for (int idx = filterPoolRefs.length - 1; idx >= 0; idx--)
				moveSystemFilterPoolReference(filterPoolRefs[idx], oldPositions[idx] + delta);
		else
			for (int idx = 0; idx < filterPoolRefs.length; idx++)
				moveSystemFilterPoolReference(filterPoolRefs[idx], oldPositions[idx] + delta);
		invalidateFilterPoolReferencesCache();
		noEvents = noSave = false;
		quietSave();
		if (fireEvents && (caller != null)) caller.filterEventFilterPoolReferencesRePositioned(filterPoolRefs, delta);
	}

	// ----------------------------------------------
	// Methods that work on FilterPool master objects
	// ----------------------------------------------
	/**
	 * @return array of filter pools currently referenced by this manager.
	 * Result will never be null, although it may be an array of length zero.
	 */
	public ISystemFilterPool[] getReferencedSystemFilterPools() {
		ISystemFilterPoolReference[] refs = getSystemFilterPoolReferences();
		ISystemFilterPool[] pools = new ISystemFilterPool[refs.length];
		for (int idx = 0; idx < pools.length; idx++)
			pools[idx] = refs[idx].getReferencedFilterPool();
		return pools;
	}

	/**
	 * @param filterPool the filter pool to test to see if we have a reference to it
	 * @return true if the given filter pool has a referencing object in this list.
	 */
	public boolean isSystemFilterPoolReferenced(ISystemFilterPool filterPool) {
		return super.isReferenced(filterPool);
	}

	/**
	 * Given a filter pool, locate the referencing object for it and return it.
	 * @param filterPool the filter pool we are testing for a reference
	 * @return the referencing object if found, else null
	 */
	public ISystemFilterPoolReference getReferenceToSystemFilterPool(ISystemFilterPool filterPool) {
		return (ISystemFilterPoolReference) super.getReferencedObject(filterPool);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.filters.ISystemFilterPoolReferenceManager#addReferenceToSystemFilterPool(org.eclipse.rse.filters.ISystemFilterPool)
	 */
	public ISystemFilterPoolReference addReferenceToSystemFilterPool(ISystemFilterPool filterPool) {
		ISystemFilterPoolReference filterPoolReference = createSystemFilterPoolReference(filterPool);
		addReferencingObject(filterPoolReference); // DWD - should be done in addReferencingObject?
		filterPoolReference.setParentReferenceManager(this);
		invalidateFilterPoolReferencesCache();
		quietSave();
		if (fireEvents && (caller != null)) caller.filterEventFilterPoolReferenceCreated(filterPoolReference);
		return filterPoolReference;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.rse.filters.ISystemFilterPoolReferenceManager#addReferenceToSystemFilterPool(org.eclipse.rse.filters.ISystemFilterPoolManager, java.lang.String)
	 */
	public ISystemFilterPoolReference addReferenceToSystemFilterPool(ISystemFilterPoolManager filterPoolManager, String filterPoolName) {
		ISystemFilterPoolReference filterPoolReference = createSystemFilterPoolReference(filterPoolManager, filterPoolName);
		addReferencingObject(filterPoolReference);
		filterPoolReference.setParentReferenceManager(this);
		invalidateFilterPoolReferencesCache();
		quietSave();
		if (fireEvents && (caller != null)) caller.filterEventFilterPoolReferenceCreated(filterPoolReference);
		return filterPoolReference;
	}

	/**
	 * Given a filter pool, locate the referencing object for it and remove it from the list.
	 * Also removes that reference from the filterPool itself, and calls back to provider when done.
	 * @param filterPool the filter pool whose references we are to remove
	 * @return the new count of referencing objects
	 */
	public int removeReferenceToSystemFilterPool(ISystemFilterPool filterPool) {
		ISystemFilterPoolReference filterPoolReference = getReferenceToSystemFilterPool(filterPool);
		int newCount = 0;
		if (filterPoolReference != null) {
			filterPoolReference.removeReference();
			newCount = removeReferencingObject(filterPoolReference);
			filterPoolReference.setParentReferenceManager(null); // DWD should be done in removeReferencingObject?
			invalidateFilterPoolReferencesCache();
			quietSave();
			// callback to provider so they can fire events in their GUI
			if (fireEvents && (caller != null)) caller.filterEventFilterPoolReferenceDeleted(filterPoolReference);
		} else
			newCount = getSystemFilterPoolReferenceCount();
		return newCount;
	}

	/**
	 * A referenced filter pool has been renamed. Update our stored name.
	 * Calls back to inform provider.
	 * @param pool the pool that has just been renamed
	 */
	public void renameReferenceToSystemFilterPool(ISystemFilterPool pool) {
		ISystemFilterPoolReference poolRef = null;
		IRSEBasePersistableReferencingObject[] refs = getReferencingObjects();
		for (int idx = 0; (poolRef == null) && (idx < refs.length); idx++)
			if (refs[idx].getReferencedObject() == pool) poolRef = (ISystemFilterPoolReference) refs[idx];

		if (poolRef != null) {
			String oldName = poolRef.getReferencedObjectName();
			poolRef.resetReferencedFilterPoolName(pool.getReferenceName());
			invalidateFilterPoolReferencesCache();
			quietSave();
			if (fireEvents && (caller != null)) caller.filterEventFilterPoolReferenceRenamed(poolRef, oldName);
		}
	}

	/**
	 * In one shot, set the filter pool references to new references to supplied filter pools.
	 * Calls back to provider.
	 * @param filterPools of filter pool objects to create references for
	 * @param deReference true to first de-reference all objects in the existing list.
	 */
	public void setSystemFilterPoolReferences(ISystemFilterPool[] filterPools, boolean deReference) {
		if (deReference)
			super.removeAndDeReferenceAllReferencingObjects();
		else
			removeAllReferencingObjects();
		// add current
		if (filterPools != null) {
			for (int idx = 0; idx < filterPools.length; idx++) {
				//addReferenceToSystemFilterPool(filterPools[idx]);
				ISystemFilterPoolReference filterPoolReference = createSystemFilterPoolReference(filterPools[idx]);
				addReferencingObject(filterPoolReference);
				filterPoolReference.setParentReferenceManager(this); // DWD should be done in addReferencingObject?
			}
			invalidateFilterPoolReferencesCache();
			quietSave();
			if (fireEvents && (caller != null)) caller.filterEventFilterPoolReferencesReset();
		}
	}

	// -------------------------
	// SPECIAL CASE METHODS
	// -------------------------
	/**
	 * Create a single filter refererence to a given filter. Needed when a filter
	 * is added to a pool, and the UI is not showing pools but rather all filters
	 * in all pool references.
	 * @param subSystem the subsystem that uses this reference manager
	 * @param filter the new filter that is being added
	 * @return the new reference
	 */
	public ISystemFilterReference getSystemFilterReference(ISubSystem subSystem, ISystemFilter filter) {
		// step 1: find the reference to the filter pool that contains this filter
		ISystemFilterPool pool = filter.getParentFilterPool();
		ISystemFilterPoolReference poolRef = getReferenceToSystemFilterPool(pool);
		// step 2: generate a reference for it
		if (poolRef != null)
			return poolRef.getSystemFilterReference(subSystem, filter);
		else
			return null;
	}

	/**
	 * Concatenate all filter references from all filter pools we reference, into one
	 * big list. Used when the UI is not showing pools.
	 * @param subSystem the subsystem for which this manager is providing filter pool reference management
	 * @return an array of references for this subsystem
	 */
	public ISystemFilterReference[] getSystemFilterReferences(ISubSystem subSystem) {
		ISystemFilterPoolReference[] poolRefs = getSystemFilterPoolReferences();
		Vector v = new Vector();
		for (int idx = 0; idx < poolRefs.length; idx++) {
			ISystemFilterReference[] filterRefs = poolRefs[idx].getSystemFilterReferences(subSystem);
			for (int jdx = 0; jdx < filterRefs.length; jdx++)
				v.addElement(filterRefs[jdx]);
		}
		ISystemFilterReference[] allRefs = new ISystemFilterReference[v.size()];
		for (int idx = 0; idx < v.size(); idx++)
			allRefs[idx] = (ISystemFilterReference) v.elementAt(idx);
		return allRefs;
	}

	/**
	 * Given a filter reference, return its position within this reference manager
	 * when you think of all filter references from all filter pool references as 
	 * being concatenated.
	 * Used when the UI is not showing pools.
	 * @param filterRef the reference to locate
	 * @return the position fo this reference or -1 if not found.
	 */
	public int getSystemFilterReferencePosition(ISystemFilterReference filterRef) {
		ISystemFilterPoolReference[] poolRefs = getSystemFilterPoolReferences();
		int match = -1;
		int totalCount = 0;
		for (int idx = 0; (match == -1) && (idx < poolRefs.length); idx++) {
			ISystemFilterReference[] filterRefs = poolRefs[idx].getSystemFilterReferences(filterRef.getSubSystem());
			for (int jdx = 0; (match == -1) && (jdx < filterRefs.length); jdx++) {
				if (filterRefs[jdx] == filterRef)
					match = totalCount;
				else
					totalCount++;
			}
		}
		return match;
	}

	/**
	 * Given a filter, return its position within this reference manager
	 * when you think of all filter references from all filter pool references as 
	 * being concatenated.
	 * Used when the UI is not showing pools.
	 * @param subSystem the subsystem in which to located the filter
	 * @param filter the filter to locate
	 * @return the position of the filter within this manager.
	 */
	public int getSystemFilterReferencePosition(ISubSystem subSystem, ISystemFilter filter) {
		ISystemFilterPoolReference[] poolRefs = getSystemFilterPoolReferences();
		int match = -1;
		int totalCount = 0;
		for (int idx = 0; (match == -1) && (idx < poolRefs.length); idx++) {
			ISystemFilterReference[] filterRefs = poolRefs[idx].getSystemFilterReferences(subSystem);
			for (int jdx = 0; (match == -1) && (jdx < filterRefs.length); jdx++) {
				if (filterRefs[jdx].getReferencedFilter() == filter)
					match = totalCount;
				else
					totalCount++;
			}
		}
		return match;
	}

	// -----------------------
	// SAVE/RESTORE METHODS...
	// -----------------------
	private void quietSave() {
		try {
			save();
		} catch (Exception exc) {
			// TODO log exception
		}
	}

	/**
	 * Save all the filter pools to disk.     
	 * Only called if the save policy is not "none".
	 * No longer used.
	 * @throws Exception
	 */
	public void save() throws Exception {
		switch (savePolicy) {
			// ONE FILE PER FILTER POOL REFERENCE MANAGER
			case ISystemFilterSavePolicies.SAVE_POLICY_ONE_FILE_PER_MANAGER:
				saveToOneFile();
				break;
		}
	}

	/**
	 * Save this reference manager to disk.
	 * Used only if using the reference manager to save a single file to disk.
	 * No longer used.
	 * @return true if the save succeeded
	 * @throws Exception
	 */
	protected boolean saveToOneFile() throws Exception {
		/* FIXME
		 String saveFileName = getSaveFilePathAndName();
		 File saveFile = new File(saveFileName);
		 boolean exists = saveFile.exists();
		 saveFileName = saveFile.toURL().toString();
		 Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
		 Resource.Factory resFactory = reg.getFactory(URI.createURI(saveFileName));
		 //System.out.println("Saving filter pool ref mgr "+getName()+" to: " + saveFile);
		 //java.util.List ext = resFactory.createExtent(); mof way
		 //ext.add(this);
		 Resource res = resFactory.createResource(URI.createURI(saveFileName));
		 res.getContents().add(this);
		 try
		 {
		 res.save(EMPTY_MAP);
		 } catch (Exception e)
		 {
		 if (debug)
		 {
		 System.out.println("Error saving filter pool ref mgr "+getName() + " to "+saveFile+": " + e.getClass().getName() + ": " + e.getMessage());
		 e.printStackTrace();
		 }
		 throw e;
		 }    
		 // if this is the first time we have created this file, we must update Eclipse
		 // resource tree to know about it...
		 if (!exists)
		 {
		 try {
		 mgrFolder.getProject().refreshLocal(IResource.DEPTH_INFINITE, null);               
		 } catch(Exception exc) {}
		 }        
		 */
		return true;
	}

	/**
	 * Restore the filter pools from disk.
	 * After restoring, you must call resolveReferencesAfterRestore.
	 * No longer used.
	 * @param caller The object that is calling this, which must implement SystemFilterPoolReferenceManagerProvider
	 * @param mgrFolder folder containing filter pool references file.
	 * @param name the name of the manager to restore. File name is derived from it when saving to one file.
	 * @param namingPolicy to get file name prefix, via getFilterPoolReferenceManagerFileNamePrefix(). Pass null to use default.
	 * @return the restored manager, or null if it does not exist.
	 * @throws Exception if anything else went wrong
	 */
	public static ISystemFilterPoolReferenceManager restore(ISystemFilterPoolReferenceManagerProvider caller, IFolder mgrFolder, String name, IRSEFilterNamingPolicy namingPolicy) throws Exception {
		if (namingPolicy == null) namingPolicy = SystemFilterNamingPolicy.getNamingPolicy();
		ISystemFilterPoolReferenceManager mgr = restoreFromOneFile(mgrFolder, name, namingPolicy);
		if (mgr != null) {
			((SystemFilterPoolReferenceManager) mgr).initialize(caller, mgrFolder, name, ISystemFilterSavePolicies.SAVE_POLICY_ONE_FILE_PER_MANAGER, namingPolicy); // core data
		}
		return mgr;
	}

	/**
	 * Restore the filter pools from disk, assuming default for a naming policy.
	 * No longer used.
	 * @param caller The object that is calling this, which must implement SystemFilterPoolReferenceManagerProvider
	 * @param mgrFolder folder containing filter pool references file.
	 * @param name the name of the manager to restore. File name is derived from it when saving to one file.
	 * @return the restored manager, or null if it does not exist.
	 * @throws Exception if anything else went wrong
	 */
	public static ISystemFilterPoolReferenceManager restore(ISystemFilterPoolReferenceManagerProvider caller, IFolder mgrFolder, String name) throws Exception {
		return restore(caller, mgrFolder, name, null);
	}

	/**
	 * Restore filter pools when all are stored in one file.
	 * No longer used.
	 * @param mgrFolder The folder containing the file to restore from.
	 * @param name The name of the manager, from which the file name is derived.
	 * @param namingPolicy Naming prefix information for persisted data file names.
	 * @return the restored manager, or null if it does not exist.
	 * @throws Exception if anything else went wrong
	 */
	protected static ISystemFilterPoolReferenceManager restoreFromOneFile(IFolder mgrFolder, String name, IRSEFilterNamingPolicy namingPolicy) throws Exception {
		ISystemFilterPoolReferenceManager mgr = null;
		/* FIXME
		 Resource.Factory.Registry reg = Resource.Factory.Registry.INSTANCE;
		 //ResourceSet resourceSet = MOF WAY
		 // Resource.Factory.Registry.getResourceSetFactory().makeResourceSet();
		 Resource res = null;
		 String saveFile = getSaveFilePathAndName(mgrFolder, name, namingPolicy);
		 try
		 {
		 //res = resourceSet.load(saveFile); MOF Way
		 Resource.Factory resFactory = reg.getFactory(URI.createURI(saveFile));
		 res = resFactory.createResource(URI.createURI(saveFile));
		 res.load(EMPTY_MAP);
		 }
		 catch (java.io.FileNotFoundException e)
		 {
		 System.out.println("Restore error: Filter pool ref mgr "+name+" missing its file: "+saveFile);
		 return null;
		 }
		 catch (Exception e)
		 {
		 if (debug)
		 {
		 System.out.println("Error restoring filter pool ref mgr "+name+" file "+saveFile+": " + e.getClass().getName() + ": " + e.getMessage());
		 e.printStackTrace();
		 }
		 throw e;
		 }

		 java.util.List ext = res.getContents();

		 // should be exactly one system filter pool manager...
		 Iterator iList = ext.iterator();
		 mgr = (SystemFilterPoolReferenceManager)iList.next();
		 if (debug)
		 System.out.println("Filter Pool Ref Mgr "+name+" loaded successfully.");
		 */
		return mgr;
	}

	/**
	 * After restoring this from disk, there is only the referenced object name,
	 * not the referenced object pointer, for each referencing object.
	 * <p>
	 * This method is called after restore and for each restored object in the list must:
	 * <ol>
	 *   <li>Do what is necessary to find the referenced object, and set the internal reference pointer.
	 *   <li>Call addReference(this) on that object so it can maintain it's in-memory list of all referencing objects.
	 *   <li>Set the important transient variables 
	 * </ol>
	 * @param relatedPoolMgrProvider the filter pool manager provider that created the filter pools we reference
	 * (usually a subsystem configuration)
	 * @param provider the host of this reference manager, so you can later call getProvider
	 * @return A Vector of SystemFilterPoolReferences that were not successfully resolved, or null if all
	 * were resolved.
	 */
	public Vector resolveReferencesAfterRestore(ISystemFilterPoolManagerProvider relatedPoolMgrProvider, ISystemFilterPoolReferenceManagerProvider provider) {
		setSystemFilterPoolManagerProvider(relatedPoolMgrProvider);
		setProvider(provider);
		ISystemFilterPoolManager[] relatedManagers = getSystemFilterPoolManagers();
		if (relatedManagers != null) {
			Vector badRefs = new Vector();
			ISystemFilterPoolReference[] poolRefs = getSystemFilterPoolReferences();
			if (poolRefs != null) {
				for (int idx = 0; idx < poolRefs.length; idx++) {
					String poolName = poolRefs[idx].getReferencedFilterPoolName();
					String mgrName = poolRefs[idx].getReferencedFilterPoolManagerName();

					ISystemFilterPool refdPool = getFilterPool(relatedManagers, mgrName, poolName);
					if ((refdPool == null) && (getFilterPoolManager(relatedManagers, mgrName) == null)) {
						//System.out.println("...looking for broken reference for "+mgrName+"."+poolName);
						refdPool = relatedPoolMgrProvider.getSystemFilterPoolForBrokenReference(this, mgrName, poolName);
					}
					if (refdPool != null) {
						poolRefs[idx].setReferenceToFilterPool(refdPool); // calls refdPool.addReference(poolRef)
					} else {
						badRefs.addElement(poolRefs[idx]);
					}
				}
				if (badRefs.size() == 0)
					return null;
				else {
					for (int idx = 0; idx < badRefs.size(); idx++) {
						ISystemFilterPoolReference badRef = (ISystemFilterPoolReference) badRefs.elementAt(idx);
						//badRef.setReferenceBroken(true);
						super.removeReferencingObject(badRef);
					}
					invalidateFilterPoolReferencesCache();
					quietSave();
					return badRefs;
				}
			}
		}
		return null;
	}

	/**
	 * Utility method to scan across all filter pools in a given named filter pool manager, for a match
	 * on a given filter pool name.
	 * @param mgrs The list of filter pool managers to scan for the given filter pool.
	 * @param mgrName The name of the manager to restrict the search to
	 * @param poolName The name of the filter pool as stored on disk. It may be qualified somehow
	 * to incorporate the manager name too.
	 * @return the filter pool that was found.
	 */
	public static ISystemFilterPool getFilterPool(ISystemFilterPoolManager[] mgrs, String mgrName, String poolName) {
		ISystemFilterPoolManager mgr = getFilterPoolManager(mgrs, mgrName);
		if (mgr == null) return null;
		return mgr.getSystemFilterPool(poolName);
	}

	/**
	 * Utility method to scan across all filter pool managers for a match on a give name.
	 * @param mgrs The list of filter pool managers to scan for the given name
	 * @param mgrName The name of the manager to restrict the search to
	 * @return the filter pool manager that was found or null if not found.
	 */
	public static ISystemFilterPoolManager getFilterPoolManager(ISystemFilterPoolManager[] mgrs, String mgrName) {
		ISystemFilterPoolManager mgr = null;
		for (int idx = 0; (mgr == null) && (idx < mgrs.length); idx++)
			if (mgrs[idx].getName().equals(mgrName)) mgr = mgrs[idx];
		return mgr;
	}

	// ------------------
	// HELPER METHODS...
	// ------------------

	/**
	 * If saving all info in one file, this returns the fully qualified name of that file,
	 * given the unadorned manager name and the prefix (if any) to adorn with.
	 * No longer used.
	 * @param mgrFolder The folder in which to save a reference manager.
	 * @param name The name of the file for a filter pool reference manager.
	 * @param namingPolicy The naming policy for a filter pool reference manager
	 * @return The name of the path to which to save the references in a filter pool reference manager.
	 */
	protected static String getSaveFilePathAndName(IFolder mgrFolder, String name, IRSEFilterNamingPolicy namingPolicy) {
		return SystemFilter.addPathTerminator(getFolderPath(mgrFolder)) + getSaveFileName(namingPolicy.getReferenceManagerSaveFileName(name));
	}

	/**
	 * Appends the correct extension to the file name where this manager is saved.
	 * No longer used.
	 * @param fileNameNoSuffix the file name <i>sans</i> suffix.
	 * @return the unqualified file name used to store this to disk.
	 */
	protected static String getSaveFileName(String fileNameNoSuffix) {
		return fileNameNoSuffix + ISystemFilterConstants.SAVEFILE_SUFFIX;
	}

	/**
	 * @return the full path name of the file in which to save this manager.
	 */
	protected String getSaveFilePathAndName() {
		return SystemFilter.addPathTerminator(getFolderPath(mgrFolder)) + getSaveFileName();
	}

	/**
	 * @return the simple name of the file in which to save this manager.
	 */
	protected String getSaveFileName() {
		return getSaveFileName(namingPolicy.getReferenceManagerSaveFileName(getName()));
	}

	/**
	 * @return the folder that this manager is contained in.
	 */
	public IFolder getFolder() {
		return mgrFolder;
	}

	/**
	 * Set the folder that this manager is contained in.
	 * @param newFolder the new folder
	 */
	public void resetManagerFolder(IFolder newFolder) {
		mgrFolder = newFolder;
	}

	/**
	 * @return the path of the folder that contains this manager.
	 */
	public String getFolderPath() {
		return getResourceHelpers().getFolderPath(mgrFolder);
	}

	/**
	 * @param folder the folder to find the path for
	 * @return the path of the given folder
	 */
	public static String getFolderPath(IFolder folder) {
		return SystemResourceHelpers.getResourceHelpers().getFolderPath(folder);
	}

	/*
	 * To reduce typing...
	 */
	private SystemResourceHelpers getResourceHelpers() {
		return SystemResourceHelpers.getResourceHelpers();
	}

	public String toString() {
		return getName();
	}

}