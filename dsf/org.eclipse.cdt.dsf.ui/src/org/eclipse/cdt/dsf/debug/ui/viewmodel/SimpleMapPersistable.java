/*****************************************************************
 * Copyright (c) 2011, 2014 Wind River Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Wind River Systems - initial API and implementation
 *****************************************************************/
package org.eclipse.cdt.dsf.debug.ui.viewmodel;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistableElement;

/**
 * Generic persistable for storing a map of simple values.
 * <br>
 * Currently supported value types are {@link Integer} and {@link String}.
 * 
 * @since 2.5
 */
public class SimpleMapPersistable<V> implements IPersistableElement, IAdaptable  {

    private static final String KEY_TYPE = "type"; //$NON-NLS-1$
    private static final String KEY_NAME = "name"; //$NON-NLS-1$
    private static final String KEY_VALUE = "value"; //$NON-NLS-1$

    private Class<V> fType;
	private Map<String, V> fValues = new TreeMap<String, V>();

    /**
     * When using this constructor, a call to {@link #restore(IMemento)} is required before use
     * for proper initialization.  Alternatively, {@link #SimpleMapPersistable(Class)} can be used.
     */
	SimpleMapPersistable() {}

	public SimpleMapPersistable(Class<V> type) {
	    fType = type;
	}
	
	@Override
    public void saveState(IMemento memento) {
		Map<String, V> values = null;
		synchronized (fValues) {
			values = new TreeMap<String, V>(fValues);
		}

        IMemento type = memento.createChild(KEY_TYPE);
	    synchronized (fType) {			
	    	type.putTextData(fType.getName());
	    }
		for (Map.Entry<String, V> entry : values.entrySet()) { 
            IMemento value = memento.createChild(KEY_NAME, entry.getKey());
            putValue(value, entry.getValue());
		}
	}

	private void putValue(IMemento memento, Object value) {
	    if (value instanceof String) {
	        memento.putString(KEY_VALUE, (String)value);
	    } else if (value instanceof Integer) {
	        memento.putInteger(KEY_VALUE, (Integer)value);
	    }
	    else {
	    	assert false;
	    }
	}

	@SuppressWarnings("unchecked")
    void restore(IMemento memento) throws ClassNotFoundException {
	    IMemento type = memento.getChild(KEY_TYPE);
    	fType = (Class<V>)Class.forName(type.getTextData());
	    
		IMemento[] list = memento.getChildren(KEY_NAME);
		Map<String, V> values = Collections.synchronizedMap(new TreeMap<String, V>());
		for (IMemento elem : list) {
		    values.put(elem.getID(), getValue(elem));
		}
		
		fValues = values;
	}

    @SuppressWarnings("unchecked")
    private V getValue(IMemento memento) {
	    synchronized (fType) {			
	    	if (String.class.equals(fType)) {
	    		return (V)memento.getString(KEY_VALUE);
	    	} else if (Integer.class.equals(fType)) {
	    		return (V)memento.getInteger(KEY_VALUE);
	    	} else {
	    		assert false;
	    	}
	    }
        return null;
    }
    

	public V getValue(String key) {
		if (key == null)
			return null;
		synchronized (fValues) {
			return fValues.get(key);
		}
	}

	public void setValue(String key, V value) {
		synchronized (fValues) {
			if (value == null) {
				fValues.remove(key);
			} else {
				fValues.put(key, value);
			}
		}
	}

	@Override
	public String getFactoryId() {
		return SimpleMapPersistableFactory.getFactoryId();
	}

	@Override
	public Object getAdapter(@SuppressWarnings("rawtypes") Class adapter) {
    	if (adapter.isInstance(this)) {
			return this;
    	}
		return null;
	}
}
