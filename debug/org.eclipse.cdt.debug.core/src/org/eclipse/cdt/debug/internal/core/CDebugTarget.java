/*
 *(c) Copyright QNX Software Systems Ltd. 2002.
 * All Rights Reserved.
 * 
 */
package org.eclipse.cdt.debug.internal.core;

import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.cdt.debug.core.CDebugModel;
import org.eclipse.cdt.debug.core.IFormattedMemoryBlock;
import org.eclipse.cdt.debug.core.IFormattedMemoryRetrieval;
import org.eclipse.cdt.debug.core.IInstructionStep;
import org.eclipse.cdt.debug.core.IRestart;
import org.eclipse.cdt.debug.core.IState;
import org.eclipse.cdt.debug.core.cdi.CDIException;
import org.eclipse.cdt.debug.core.cdi.ICBreakpoint;
import org.eclipse.cdt.debug.core.cdi.ICEndSteppingRange;
import org.eclipse.cdt.debug.core.cdi.ICSessionObject;
import org.eclipse.cdt.debug.core.cdi.ICSignal;
import org.eclipse.cdt.debug.core.cdi.event.ICChangedEvent;
import org.eclipse.cdt.debug.core.cdi.event.ICCreatedEvent;
import org.eclipse.cdt.debug.core.cdi.event.ICDisconnectedEvent;
import org.eclipse.cdt.debug.core.cdi.event.ICEvent;
import org.eclipse.cdt.debug.core.cdi.event.ICEventListener;
import org.eclipse.cdt.debug.core.cdi.event.ICExitedEvent;
import org.eclipse.cdt.debug.core.cdi.event.ICLoadedEvent;
import org.eclipse.cdt.debug.core.cdi.event.ICRestartedEvent;
import org.eclipse.cdt.debug.core.cdi.event.ICResumedEvent;
import org.eclipse.cdt.debug.core.cdi.event.ICSteppingEvent;
import org.eclipse.cdt.debug.core.cdi.event.ICSuspendedEvent;
import org.eclipse.cdt.debug.core.cdi.event.ICTerminatedEvent;
import org.eclipse.cdt.debug.core.cdi.model.ICObject;
import org.eclipse.cdt.debug.core.cdi.model.ICTarget;
import org.eclipse.cdt.debug.core.cdi.model.ICThread;
import org.eclipse.core.resources.IMarkerDelta;
import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.IBreakpointManager;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchListener;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IMemoryBlock;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IThread;

/**
 * 
 * Enter type comment.
 * 
 * @since Aug 1, 2002
 */
public class CDebugTarget extends CDebugElement
						  implements IDebugTarget,
						  			 ICEventListener,
						  			 IRestart,
						  			 IInstructionStep,
						  			 IFormattedMemoryRetrieval,
						  			 IState,
						  			 ILaunchListener
{
	/**
	 * Threads contained in this debug target. When a thread
	 * starts it is added to the list. When a thread ends it
	 * is removed from the list.
	 */
	private ArrayList fThreads;

	/**
	 * Associated system process, or <code>null</code> if not available.
	 */
	private IProcess fProcess;

	/**
	 * Underlying CDI target.
	 */
	private ICTarget fCDITarget;

	/**
	 * The name of this target.
	 */
	private String fName;

	/**
	 * Whether this target is suspended.
	 */
	private boolean fSuspended = true;

	/**
	 * Whether terminated
	 */
	private boolean fTerminated;
	
	/**
	 * Whether in the process of terminating
	 */
	private boolean fTerminating;

	/**
	 * Whether disconnected
	 */
	private boolean fDisconnected;

	/**
	 * Whether the target should be resumed on startup
	 */
	private boolean fResumeOnStartup = false; 
	
	/**
	 * The launch this target is contained in
	 */
	private ILaunch fLaunch;	

	/**
	 * Whether terminate is supported.
	 */
	private boolean fSupportsTerminate;

	/**
	 * Whether disconnect is supported.
	 */
	private boolean fSupportsDisconnect;

	/**
	 * Constructor for CDebugTarget.
	 * @param target
	 */
	public CDebugTarget( ILaunch launch, 
						 ICTarget cdiTarget, 
						 String name,
						 IProcess process,
						 boolean allowsTerminate,
						 boolean allowsDisconnect, 
						 boolean resume )
	{
		super( null );
		setLaunch( launch );
		setDebugTarget( this );
		setName( name );
		setProcess( process );
		setCDITarget( cdiTarget );
		setThreadList( new ArrayList( 5 ) );
		initialize();
		DebugPlugin.getDefault().getLaunchManager().addLaunchListener( this );
		getCDISession().getEventManager().addEventListener( this );
	}

	/**
	 * Initialize state from the underlying debug session.
	 * 
	 */
	protected void initialize() 
	{
		initializeState();
		initializeBreakpoints();
		getLaunch().addDebugTarget( this );
		fireCreationEvent();
	}

	/**
	 * Adds all of the pre-existing threads to this debug target.  
	 */
	protected void initializeState()
	{
		ICThread[] threads = new ICThread[0];
		try
		{
			threads = getCDITarget().getThreads();
		}
		catch( CDIException e )
		{
			internalError( e );
		}
		for ( int i = 0; i < threads.length; ++i )
			createRunningThread( threads[i] );

		if ( isResumeOnStartup() )
		{
			setSuspended( false );
		}
	}

	/**
	 * Installs all C/C++ breakpoints that currently exist in
	 * the breakpoint manager.
	 * 
	 */
	protected void initializeBreakpoints()
	{
		IBreakpointManager manager = DebugPlugin.getDefault().getBreakpointManager();
		manager.addBreakpointListener( this );
		IBreakpoint[] bps = (IBreakpoint[])manager.getBreakpoints( CDebugModel.getPluginIdentifier() );
		for ( int i = 0; i < bps.length; i++ )
		{
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugTarget#getProcess()
	 */
	public IProcess getProcess()
	{
		return fProcess;
	}

	/**
	 * Sets the process associated with this debug target,
	 * possibly <code>null</code>. Set on creation.
	 * 
	 * @param process the system process associated with the
	 * 	underlying CDI target, or <code>null</code> if no process is
	 * 	associated with this debug target (for example, a core dump debugging).
	 */
	protected void setProcess( IProcess process )
	{
		fProcess = process;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugTarget#getThreads()
	 */
	public IThread[] getThreads()
	{
		List threads = getThreadList();
		return (IThread[])threads.toArray( new IThread[threads.size()] );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugTarget#hasThreads()
	 */
	public boolean hasThreads() throws DebugException
	{
		return getThreadList().size() > 0;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugTarget#getName()
	 */
	public String getName() throws DebugException
	{
		return fName;
	}

	/**
	 * Sets the name of this debug target.
	 *  
	 * @param name the name of this debug target
	 */
	protected void setName( String name ) 
	{
		fName = name;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDebugTarget#supportsBreakpoint(IBreakpoint)
	 */
	public boolean supportsBreakpoint( IBreakpoint breakpoint )
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchListener#launchRemoved(ILaunch)
	 */
	public void launchRemoved( ILaunch launch )
	{
		if ( !isAvailable() )
		{
			return;
		}
		if ( launch.equals( getLaunch() ) )
		{
			// This target has been deregistered, but it hasn't successfully terminated.
			// Update internal state to reflect that it is disconnected
			disconnected();
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchListener#launchAdded(ILaunch)
	 */
	public void launchAdded( ILaunch launch )
	{
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.ILaunchListener#launchChanged(ILaunch)
	 */
	public void launchChanged( ILaunch launch )
	{
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ITerminate#canTerminate()
	 */
	public boolean canTerminate()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ITerminate#isTerminated()
	 */
	public boolean isTerminated()
	{
		return fTerminated;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ITerminate#terminate()
	 */
	public void terminate() throws DebugException
	{
	}

	/**
	 * Sets whether this debug target is terminated
	 * 
	 * @param terminated <code>true</code> if this debug
	 * 		  target is terminated, otherwise <code>false</code>
	 */
	protected void setTerminated( boolean terminated )
	{
		fTerminated = terminated;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ISuspendResume#canResume()
	 */
	public boolean canResume()
	{
		return isSuspended() && isAvailable();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ISuspendResume#canSuspend()
	 */
	public boolean canSuspend()
	{
		if ( !isSuspended() && isAvailable() )
		{
			// only allow suspend if no threads are currently suspended
			IThread[] threads = getThreads();
			for ( int i = 0; i < threads.length; i++ )
			{
				if ( threads[i].isSuspended() )
				{
					return false;
				}
			}
			return true;
		}
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ISuspendResume#isSuspended()
	 */
	public boolean isSuspended()
	{
		return fSuspended;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ISuspendResume#resume()
	 */
	public void resume() throws DebugException
	{
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.ISuspendResume#suspend()
	 */
	public void suspend() throws DebugException
	{
		if ( isSuspended() )
		{
			return;
		}
		try
		{
			setSuspended( true );
			fireSuspendEvent( DebugEvent.CLIENT_REQUEST );
			getCDITarget().suspend();
			suspendThreads();
		}
		catch( CDIException e )
		{
			setSuspended( false );
			fireResumeEvent( DebugEvent.CLIENT_REQUEST );
			targetRequestFailed( MessageFormat.format( "{0} occurred suspending target.", new String[] { e.toString()} ), e );
		}
	}

	/**
	 * Notifies threads that they have been suspended.
	 * 
	 */
	protected void suspendThreads()
	{
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointAdded(IBreakpoint)
	 */
	public void breakpointAdded( IBreakpoint breakpoint )
	{
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointRemoved(IBreakpoint, IMarkerDelta)
	 */
	public void breakpointRemoved( IBreakpoint breakpoint, IMarkerDelta delta )
	{
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.IBreakpointListener#breakpointChanged(IBreakpoint, IMarkerDelta)
	 */
	public void breakpointChanged( IBreakpoint breakpoint, IMarkerDelta delta )
	{
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDisconnect#canDisconnect()
	 */
	public boolean canDisconnect()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDisconnect#disconnect()
	 */
	public void disconnect() throws DebugException
	{
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IDisconnect#isDisconnected()
	 */
	public boolean isDisconnected()
	{
		return fDisconnected;
	}

	/**
	 * Sets whether this debug target is disconnected
	 * 
	 * @param disconnected <code>true</code> if this debug
	 *  	  target is disconnected, otherwise <code>false</code>
	 */
	protected void setDisconnected( boolean disconnected )
	{
		fDisconnected = disconnected;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockRetrieval#supportsStorageRetrieval()
	 */
	public boolean supportsStorageRetrieval()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.debug.core.model.IMemoryBlockRetrieval#getMemoryBlock(long, long)
	 */
	public IMemoryBlock getMemoryBlock( long startAddress, long length ) throws DebugException
	{
		return null;
	}

	/**
	 * @see org.eclipse.debug.core.model.IDebugElement#getLaunch()
	 */
	public ILaunch getLaunch() 
	{
		return fLaunch;
	}

	/**
	 * Sets the launch this target is contained in
	 * 
	 * @param launch the launch this target is contained in
	 */
	private void setLaunch( ILaunch launch )
	{
		fLaunch = launch;
	}
	
	/**
	 * Returns the list of threads contained in this debug target.
	 * 
	 * @return list of threads
	 */
	protected ArrayList getThreadList()
	{
		return fThreads;
	}
	
	/**
	 * Sets the list of threads contained in this debug target.
	 * Set to an empty collection on creation. Threads are
	 * added and removed as they start and end. On termination
	 * this collection is set to the immutable singleton empty list.
	 * 
	 * @param threads empty list
	 */
	private void setThreadList( ArrayList threads )
	{
		fThreads = threads;
	}

	private void setCDITarget( ICTarget cdiTarget )
	{
		fCDITarget = cdiTarget;
	}

	/* (non-Javadoc)
	 * @see IAdaptable#getAdapter(Class)
	 */
	public Object getAdapter( Class adapter ) 
	{
		if ( adapter.equals( IDebugTarget.class ) )
			return this;
		if ( adapter.equals( ICTarget.class ) )
			return fCDITarget;
		return super.getAdapter( adapter );
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.cdi.event.ICEventListener#handleDebugEvent(ICEvent)
	 */
	public void handleDebugEvent( ICEvent event )
	{
		ICObject source = event.getSource();
		if ( source.getCDITarget().equals( this ) )
		{
			if ( event instanceof ICCreatedEvent )
			{
				if ( source instanceof ICThread )
				{
					handleThreadCreatedEvent( (ICCreatedEvent)event );
				}
			}
			else if ( event instanceof ICSuspendedEvent )
			{
				if ( source instanceof ICTarget )
				{
					handleSuspendedEvent( (ICSuspendedEvent)event );
				}
			}
			else if ( event instanceof ICResumedEvent )
			{
				if ( source instanceof ICTarget )
				{
					handleResumedEvent( (ICResumedEvent)event );
				}
			}
			else if ( event instanceof ICExitedEvent )
			{
				if ( source instanceof ICTarget )
				{
					handleExitedEvent( (ICExitedEvent)event );
				}
			}
			else if ( event instanceof ICTerminatedEvent )
			{
				if ( source instanceof ICTarget )
				{
					handleTerminatedEvent( (ICTerminatedEvent)event );
				}
				else if ( source instanceof ICThread )
				{
					handleThreadTerminatedEvent( (ICTerminatedEvent)event );
				}
			}
			else if ( event instanceof ICDisconnectedEvent )
			{
				if ( source instanceof ICTarget )
				{
					handleDisconnectedEvent( (ICDisconnectedEvent)event );
				}
			}
			else if ( event instanceof ICChangedEvent )
			{
				if ( source instanceof ICTarget )
				{
					handleChangedEvent( (ICChangedEvent)event );
				}
			}
			else if ( event instanceof ICLoadedEvent )
			{
				if ( source instanceof ICTarget )
				{
					handleLoadedEvent( (ICLoadedEvent)event );
				}
			}
			else if ( event instanceof ICRestartedEvent )
			{
				if ( source instanceof ICTarget )
				{
					handleRestartedEvent( (ICRestartedEvent)event );
				}
			}
			else if ( event instanceof ICSteppingEvent )
			{
				if ( source instanceof ICTarget )
				{
					handleSteppingEvent( (ICSteppingEvent)event );
				}
			}
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.IRestart#canRestart()
	 */
	public boolean canRestart()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.IRestart#restart()
	 */
	public void restart() throws DebugException
	{
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.IInstructionStep#canStepIntoInstruction()
	 */
	public boolean canStepIntoInstruction()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.IInstructionStep#canStepOverInstruction()
	 */
	public boolean canStepOverInstruction()
	{
		return false;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.IInstructionStep#stepIntoInstruction()
	 */
	public void stepIntoInstruction() throws DebugException
	{
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.IInstructionStep#stepOverInstruction()
	 */
	public void stepOverInstruction() throws DebugException
	{
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.IFormattedMemoryRetrieval#getFormattedMemoryBlock(long, int, int, int, int, char)
	 */
	public IFormattedMemoryBlock getFormattedMemoryBlock( long startAddress,
														  int format,
														  int wordSize,
														  int numberOfRows,
														  int numberOfColumns,
														  char paddingChar ) throws DebugException
	{
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.IFormattedMemoryRetrieval#getFormattedMemoryBlock(long, int, int, int, int)
	 */
	public IFormattedMemoryBlock getFormattedMemoryBlock( long startAddress,
														  int format,
														  int wordSize,
														  int numberOfRows,
														  int numberOfColumns ) throws DebugException
	{
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.IFormattedMemoryRetrieval#getSupportedFormats()
	 */
	public int[] getSupportedFormats() throws DebugException
	{
		return null;
	}

	/**
	 * Returns whether this target is available to handle client 
	 * requests.
	 * 
	 * @return whether this target is available to handle client requests
	 */
	public boolean isAvailable()
	{
		return false;
	}

	/**
	 * Sets whether this target is suspended.
	 * 
	 * @param suspended whether this target is suspended
	 */
	private void setSuspended( boolean suspended )
	{
		fSuspended = suspended;
	}

	/**
	 * Returns whether this target is in the process of terminating.
	 * 
	 * @return whether this target is terminating
	 */
	protected boolean isTerminating()
	{
		return fTerminating;
	}

	/**
	 * Sets whether this target is in the process of terminating.
	 * 
	 * @param terminating whether this target is terminating
	 */
	protected void setTerminating( boolean terminating )
	{
		fTerminating = terminating;
	}

	/**
	 * Updates the state of this target to be terminated,
	 * if not already terminated.
	 */
	protected void terminated()
	{
		setTerminating( false );
		if ( !isTerminated() )
		{
			setTerminated( true );
			setDisconnected( true );
			cleanup();
			fireTerminateEvent();
		}
	}
	
	/**
	 * Updates the state of this target for disconnection
	 * from the VM.
	 */
	protected void disconnected()
	{
		if ( !isDisconnected() )
		{
			setDisconnected( true );
			cleanup();
			fireTerminateEvent();
		}
	}

	/** 
	 * Cleans up the internal state of this debug target as a result 
	 * of a session ending.
	 * 
	 */
	protected void cleanup()
	{
		removeAllThreads();
		getCDISession().getEventManager().removeEventListener( this );
		DebugPlugin.getDefault().getBreakpointManager().removeBreakpointListener( this );
		DebugPlugin.getDefault().getLaunchManager().removeLaunchListener( this );
		removeAllBreakpoints();
	}
	
	/**
	 * Removes all threads from this target's collection
	 * of threads, firing a terminate event for each.
	 * 
	 */
	protected void removeAllThreads()
	{
	}

	/**
	 * Removes all breakpoints from this target.
	 * 
	 */
	protected void removeAllBreakpoints() 
	{
	}

	/**
	 * Creates, adds and returns a thread for the given underlying 
	 * CDI thread. A creation event is fired for the thread.
	 * Returns <code>null</code> if during the creation of the thread 
	 * this target is set to the disconnected state.
	 * 
	 * @param thread the underlying CDI thread
	 * @return model thread
	 */
	protected CThread createThread( ICThread cdiThread )
	{
		CThread thread = null;
		thread = new CThread( this, cdiThread );
		if ( isDisconnected() )
		{
			return null;
		}
		getThreadList().add( thread );
		thread.fireCreationEvent();
		return thread;
	}

	/**
	 * Creates a new thread from the given CDI thread and initializes
	 * its state to "Running".
	 * 
	 * @see CDebugTarget#createThread(ICThread)
	 */
	protected CThread createRunningThread( ICThread cdiThread )
	{
		CThread thread = null;
		thread = new CThread( this, cdiThread );
		if ( isDisconnected() )
		{
			return null;
		}
		thread.setRunning( true );
		getThreadList().add( thread );
		thread.fireCreationEvent();
		return thread;
	}

	/**
	 * Sets whether this target should be resumed on startup.
	 * 
	 * @param resume whether this target should be resumed on startup
	 */
	private void setResumeOnStartup( boolean resume )
	{
		fResumeOnStartup = resume;
	}
	
	/**
	 * Returns whether this target should be resumed on startup.
	 * 
	 * @return whether this target should be resumed on startup
	 */
	protected boolean isResumeOnStartup()
	{
		return fResumeOnStartup;
	}

	private void handleSuspendedEvent( ICSuspendedEvent event )
	{
		setSuspended( true );
		ICSessionObject reason = event.getReason();
		if ( reason instanceof ICEndSteppingRange )
		{
			handleEndSteppingRange( (ICEndSteppingRange)reason );
		}
		else if ( reason instanceof ICBreakpoint )
		{
			handleBreakpointHit( (ICBreakpoint)reason );
		}
		else if ( reason instanceof ICSignal )
		{
			handleSuspendedBySignal( (ICSignal)reason );
		}
	}

	private void handleResumedEvent( ICResumedEvent event )
	{
		setSuspended( false );
		fireResumeEvent( DebugEvent.UNSPECIFIED );
	}
	
	private void handleEndSteppingRange( ICEndSteppingRange endSteppingRange )
	{
		fireSuspendEvent( DebugEvent.UNSPECIFIED );
	}

	private void handleBreakpointHit( ICBreakpoint breakpoint )
	{
		fireSuspendEvent( DebugEvent.BREAKPOINT );
	}
	
	private void handleSuspendedBySignal( ICSignal signal )
	{
		fireSuspendEvent( DebugEvent.UNSPECIFIED );
	}

	private void handleExitedEvent( ICExitedEvent event )
	{
		fireChangeEvent( DebugEvent.STATE );
	}

	private void handleTerminatedEvent( ICTerminatedEvent event )
	{
		terminated();
	}

	private void handleDisconnectedEvent( ICDisconnectedEvent event )
	{
		disconnected();
	}

	private void handleChangedEvent( ICChangedEvent event )
	{
	}

	private void handleLoadedEvent( ICLoadedEvent event )
	{
	}

	private void handleRestartedEvent( ICRestartedEvent event )
	{
	}

	private void handleSteppingEvent( ICSteppingEvent event )
	{
	}

	private void handleThreadCreatedEvent( ICCreatedEvent event )
	{
		ICThread cdiThread = (ICThread)event.getSource();
		CThread thread= findThread( cdiThread );
		if ( thread == null ) 
		{
			createThread( cdiThread );
		} 
		else 
		{
			thread.disposeStackFrames();
			thread.fireChangeEvent( DebugEvent.CONTENT );
		}
	}

	private void handleThreadTerminatedEvent( ICTerminatedEvent event )
	{
		ICThread cdiThread = (ICThread)event.getSource();
		CThread thread = findThread( cdiThread );
		if ( thread != null) 
		{
			getThreadList().remove( thread );
			thread.terminated();
		}
	}

	/**
	 * Finds and returns the model thread for the associated CDI thread, 
	 * or <code>null</code> if not found.
	 * 
	 * @param the underlying CDI thread
	 * @return the associated model thread
	 */
	public CThread findThread( ICThread cdiThread )
	{
		List threads = getThreadList();
		for ( int i = 0; i < threads.size(); i++ )
		{
			CThread t = (CThread)threads.get( i );
			if ( t.getCDIThread().equals( cdiThread ) )
				return t;
		}
		return null;
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.debug.core.IState#getCurrentState()
	 */
	public int getCurrentState()
	{
		return IState.UNKNOWN;
	}
}
