/*******************************************************************************
 * Copyright (c) 2011 Mentor Graphics and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * Mentor Graphics - Initial API and implementation
 *******************************************************************************/

package org.eclipse.cdt.dsf.gdb.service.command;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.eclipse.cdt.dsf.debug.service.command.ICommand;
import org.eclipse.cdt.dsf.debug.service.command.ICommandListener;
import org.eclipse.cdt.dsf.debug.service.command.ICommandResult;
import org.eclipse.cdt.dsf.debug.service.command.ICommandToken;
import org.eclipse.cdt.dsf.gdb.IGdbDebugPreferenceConstants;
import org.eclipse.cdt.dsf.gdb.internal.GdbPlugin;
import org.eclipse.cdt.dsf.mi.service.command.AbstractMIControl;
import org.eclipse.cdt.dsf.mi.service.command.commands.MICommand;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.InstanceScope;

/**
 * The command timeout manager registers itself as a command listener and monitors 
 * the command execution time. The goal of this implementation is to gracefully 
 * handle disruptions in the communication between Eclipse and GDB. 
 * 
 * The algorithm used by this class is based on the assumption that the command 
 * execution in GDB is sequential even though DSF can send up to 3 commands at 
 * a time to GDB (see {@link AbstractMIControl}).
 *  
 * @since 4.1
 */
public class GdbCommandTimeoutManager implements ICommandListener, IPreferenceChangeListener {

	public interface ICommandTimeoutListener {
		
		void commandTimedOut( ICommandToken token );
	}

	public final static boolean DEBUG = "true".equals( Platform.getDebugOption( "org.eclipse.cdt.dsf.gdb/debug/timeouts" ) ); //$NON-NLS-1$//$NON-NLS-2$
	
	class QueueEntry {
		long fTimestamp;
		ICommandToken fCommandToken;
		
		QueueEntry( long timestamp, ICommandToken commandToken ) {
			super();
			fTimestamp = timestamp;
			fCommandToken = commandToken;
		}

		/* (non-Javadoc)
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals( Object obj ) {
			if ( obj instanceof QueueEntry ) {
				return fCommandToken.equals( ((QueueEntry)obj).fCommandToken );
			}
			return false;
		}
	}

	enum TimerThreadState {
		INITIALIZING,
		RUNNING,
		HALTED,
		SHUTDOWN
	}

	class TimerThread extends Thread {

		private BlockingQueue<QueueEntry> fQueue;
		private int fWaitTimeout = IGdbDebugPreferenceConstants.COMMAND_TIMEOUT_VALUE_DEFAULT;
		private TimerThreadState fState = TimerThreadState.INITIALIZING;

		TimerThread( BlockingQueue<QueueEntry> queue, int timeout ) {
			super();
			fQueue = queue;
			setWaitTimout( timeout );
		}

		/* (non-Javadoc)
		 * @see java.lang.Thread#run()
		 */
		@Override
		public void run() {
			setState( ( getWaitTimeout() > 0 ) ? 
					TimerThreadState.RUNNING : TimerThreadState.HALTED );
			doRun();
		}

		private void doRun() {
			if ( fState == TimerThreadState.HALTED ) {
				halted();
			}
			else {
				running();
			}
		}

		private void halted() {
			try {
				synchronized( TimerThread.this ) {
					wait();
				}
			}
			catch( InterruptedException e ) {
				doRun();
			}
		}

		private void running() {
			try {
				while( fState == TimerThreadState.RUNNING ) {
					long timeout = getWaitTimeout();
					QueueEntry entry = fQueue.peek();
					if ( entry != null ) {
						// Calculate the time elapsed since the execution of this command started 
						// and compare it with the command's timeout value.
						// If the elapsed time is greater or equal than the timeout value the command 
						// is marked as timed out. Otherwise, schedule the next check when the timeout 
						// expires.
						long commandTimeout = getTimeoutForCommand( entry.fCommandToken.getCommand() );
						
						if ( DEBUG ) {
							String commandText = entry.fCommandToken.getCommand().toString();
							if ( commandText.endsWith( "\n" ) ) //$NON-NLS-1$
								commandText = commandText.substring( 0, commandText.length() - 1 );
							printDebugMessage( String.format( "Processing command '%s', command timeout is %d", //$NON-NLS-1$ 
									commandText, Long.valueOf( commandTimeout ) ) );
						}

						long currentTime = System.currentTimeMillis();
						long elapsedTime = currentTime - entry.fTimestamp;
						if ( commandTimeout <= elapsedTime ) {
							processTimedOutCommand( entry.fCommandToken );
							fQueue.remove( entry );
							// Reset the timestamp of the next command in the queue because 
							// regardless how long the command has been in the queue GDB will 
							// start executing it only when the previous command is . 
							QueueEntry nextEntry = fQueue.peek();
							if ( nextEntry != null ) {
								setTimeStamp( currentTime, nextEntry );
							}
						}
						else {
							timeout = Math.min( timeout, commandTimeout - elapsedTime );
						
							if ( DEBUG ) {
								String commandText = entry.fCommandToken.getCommand().toString();
								if ( commandText.endsWith( "\n" ) ) //$NON-NLS-1$
									commandText = commandText.substring( 0, commandText.length() - 1 );
								printDebugMessage( String.format( "Setting timeout %d for command '%s'", Long.valueOf( timeout ), commandText ) ); //$NON-NLS-1$
							}
						}
					}
					synchronized( TimerThread.this ) {
						wait( timeout );
					}
					if ( isInterrupted() ) {
						doRun();
					}
				}
			}
			catch( InterruptedException e ) {
				doRun();
			}
		}

		void shutdown() {
			setState( TimerThreadState.SHUTDOWN );
			interrupt();
		}
		
		void haltProcessing() {
			setState( TimerThreadState.HALTED );
			interrupt();
		}
		
		void resumeProcessing() {
			setState( TimerThreadState.RUNNING );
			interrupt();
		}
		
		void setWaitTimout( int waitTimeout ) {
			fWaitTimeout = waitTimeout;
			if ( DEBUG )
				printDebugMessage( String.format( "Wait timeout is set to %d", Integer.valueOf( fWaitTimeout ) ) ); //$NON-NLS-1$
		}
		
		int getWaitTimeout() {
			return fWaitTimeout;
		}
		
		void setState( TimerThreadState state ) {
			fState = state;
		}
	}

	private static final String TIMEOUT_TRACE_IDENTIFIER = "[TMO]"; //$NON-NLS-1$

	private IGDBControl fCommandControl;
	private boolean fTimeoutEnabled = false;
	private int fTimeout = 0;
	private TimerThread fTimerThread;
	private BlockingQueue<QueueEntry> fCommandQueue = new LinkedBlockingQueue<QueueEntry>();
	private CustomTimeoutsMap fCustomTimeouts = new CustomTimeoutsMap();

	private ListenerList fListeners;
	
	public GdbCommandTimeoutManager( IGDBControl commandControl ) {
		fCommandControl = commandControl;
		fListeners = new ListenerList();
	}

	public void initialize() {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode( GdbPlugin.PLUGIN_ID );

		fTimeoutEnabled = Platform.getPreferencesService().getBoolean( 
				GdbPlugin.PLUGIN_ID,
				IGdbDebugPreferenceConstants.PREF_COMMAND_TIMEOUT, 
				false,
				null );

		fTimeout = Platform.getPreferencesService().getInt( 
				GdbPlugin.PLUGIN_ID,
				IGdbDebugPreferenceConstants.PREF_COMMAND_TIMEOUT_VALUE, 
				IGdbDebugPreferenceConstants.COMMAND_TIMEOUT_VALUE_DEFAULT,
				null );
		
		fCustomTimeouts.initializeFromMemento( Platform.getPreferencesService().getString( 
				GdbPlugin.PLUGIN_ID,
				IGdbDebugPreferenceConstants.PREF_COMMAND_CUSTOM_TIMEOUTS, 
				"", //$NON-NLS-1$
				null ) );
		
		node.addPreferenceChangeListener( this );
		
		fCommandControl.addCommandListener( this );
		
		fTimerThread = new TimerThread( fCommandQueue, calculateWaitTimeout() );
		fTimerThread.start();
	}

	public void dispose() {
		fTimerThread.shutdown();
		fListeners.clear();
		InstanceScope.INSTANCE.getNode( GdbPlugin.PLUGIN_ID ).removePreferenceChangeListener( this );
		fCommandControl.removeCommandListener( this );
		fCommandQueue.clear();
		fCustomTimeouts.clear();
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.dsf.debug.service.command.ICommandListener#commandQueued(org.eclipse.cdt.dsf.debug.service.command.ICommandToken)
	 */
	@Override
	public void commandQueued( ICommandToken token ) {
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.dsf.debug.service.command.ICommandListener#commandSent(org.eclipse.cdt.dsf.debug.service.command.ICommandToken)
	 */
	@Override
	public void commandSent( ICommandToken token ) {
		if ( !isTimeoutEnabled() )
			return;
		int commandTimeout = getTimeoutForCommand( token.getCommand() );
		if ( DEBUG ) {
			String commandText = token.getCommand().toString();
			if ( commandText.endsWith( "\n" ) ) //$NON-NLS-1$
				commandText = commandText.substring( 0, commandText.length() - 1 );
			printDebugMessage( String.format( "Command '%s' sent, timeout = %d", commandText, Integer.valueOf( commandTimeout ) ) ); //$NON-NLS-1$
		}
		if ( commandTimeout == 0 )
			// Skip commands with no timeout 
			return;
		try {
			fCommandQueue.put( new QueueEntry( System.currentTimeMillis(), token ) );
		}
		catch( InterruptedException e ) {
			// ignore
		}
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.dsf.debug.service.command.ICommandListener#commandRemoved(org.eclipse.cdt.dsf.debug.service.command.ICommandToken)
	 */
	@Override
	public void commandRemoved( ICommandToken token ) {
		if ( !isTimeoutEnabled() )
			return;
		removeCommand( token, false );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.cdt.dsf.debug.service.command.ICommandListener#commandDone(org.eclipse.cdt.dsf.debug.service.command.ICommandToken, org.eclipse.cdt.dsf.debug.service.command.ICommandResult)
	 */
	@Override
	public void commandDone( ICommandToken token, ICommandResult result ) {
		if ( !isTimeoutEnabled() )
			return;
		removeCommand( token, true );
	}

	/* (non-Javadoc)
	 * @see org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener#preferenceChange(org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent)
	 */
	@Override
	public void preferenceChange( PreferenceChangeEvent event ) {
		String property = event.getKey();
		if ( IGdbDebugPreferenceConstants.PREF_COMMAND_TIMEOUT.equals( property ) ) {
			if ( event.getNewValue() == null || !event.getNewValue().equals( event.getOldValue() ) ) {
				fTimeoutEnabled = Boolean.parseBoolean( event.getNewValue().toString() );
				updateWaitTimeout();
				fTimerThread.setState( ( fTimerThread.getWaitTimeout() > 0 ) ? 
						TimerThreadState.RUNNING : TimerThreadState.HALTED );
				fTimerThread.interrupt();
			}
		}
		else if ( IGdbDebugPreferenceConstants.PREF_COMMAND_TIMEOUT_VALUE.equals( property ) ) {
			if ( !event.getNewValue().equals( event.getOldValue() ) ) {
				try {
					fTimeout = Integer.parseInt( event.getNewValue().toString() );
					updateWaitTimeout();
					fTimerThread.setState( ( fTimerThread.getWaitTimeout() > 0 ) ? 
							TimerThreadState.RUNNING : TimerThreadState.HALTED );
					fTimerThread.interrupt();
				}
				catch( NumberFormatException e ) {
					GdbPlugin.getDefault().getLog().log( new Status( IStatus.ERROR, GdbPlugin.PLUGIN_ID, "Invlaid timeout value" ) ); //$NON-NLS-1$
				}
			}
		}
		else if ( IGdbDebugPreferenceConstants.PREF_COMMAND_CUSTOM_TIMEOUTS.equals( property ) ) {
			if ( event.getNewValue() instanceof String ) {
				fCustomTimeouts.initializeFromMemento( (String)event.getNewValue() );
			}
			else if ( event.getNewValue() == null ) {
				fCustomTimeouts.clear();
			}
			updateWaitTimeout();
			fTimerThread.setState( ( fTimerThread.getWaitTimeout() > 0 ) ? 
					TimerThreadState.RUNNING : TimerThreadState.HALTED );
			fTimerThread.interrupt();
		}
	}

	protected int getTimeoutForCommand( ICommand<? extends ICommandResult> command ) {
		if ( !(command instanceof MICommand<?>) )
			return 0;
		@SuppressWarnings( "unchecked" )
		Integer timeout = fCustomTimeouts.get( ((MICommand<? extends MIInfo>)command).getOperation() );
		return ( timeout != null ) ? timeout.intValue() : fTimeout;
	}

	protected void processTimedOutCommand( ICommandToken token ) {
		if ( DEBUG ) {
			String commandText = token.getCommand().toString();
			if ( commandText.endsWith( "\n" ) ) //$NON-NLS-1$
				commandText = commandText.substring( 0, commandText.length() - 1 );
			printDebugMessage( String.format( "Command '%s' is timed out", commandText ) ); //$NON-NLS-1$
		}
		for ( Object l : fListeners.getListeners() ) {
			((ICommandTimeoutListener)l).commandTimedOut( token );
		}
	}

	public void addCommandTimeoutListener( ICommandTimeoutListener listener ) {
		fListeners.add( listener );
	}

	public void removeCommandTimeoutListener( ICommandTimeoutListener listener ) {
		fListeners.remove( listener );
	}
	
	private void updateWaitTimeout() {
		fTimerThread.setWaitTimout( calculateWaitTimeout() );
	}

	private boolean isTimeoutEnabled() {
		return fTimeoutEnabled;
	}

	private void printDebugMessage( String message ) {
		System.out.println( String.format( "%s %s  %s", GdbPlugin.getDebugTime(), TIMEOUT_TRACE_IDENTIFIER, message ) ); //$NON-NLS-1$		
	}

	private int calculateWaitTimeout() {
		int waitTimeout = 0;
		if ( isTimeoutEnabled() ) {
			waitTimeout = fTimeout;
			for ( Integer t : fCustomTimeouts.values() ) {
				if ( t.intValue() > 0 ) {
					waitTimeout = Math.min( waitTimeout, t.intValue() );
				}
			}
		}
		return waitTimeout;
	}

	private void removeCommand( ICommandToken token, boolean done ) {
		fCommandQueue.remove( new QueueEntry( 0, token ) );
		if ( DEBUG ) {
			String commandText = token.getCommand().toString();
			if ( commandText.endsWith( "\n" ) ) //$NON-NLS-1$
				commandText = commandText.substring( 0, commandText.length() - 1 );
			String message = ( done ) ? 
					String.format( "Command '%s' is done", commandText ) : //$NON-NLS-1$
					String.format( "Command '%s' removed", commandText ); //$NON-NLS-1$
			printDebugMessage( message );
		}
		// Reset the timestamp of the next command in the queue because 
		// regardless how long it has been in the queue GDB started executing 
		QueueEntry nextEntry = fCommandQueue.peek();
		if ( nextEntry != null ) {
			setTimeStamp( System.currentTimeMillis(), nextEntry );
		}
	}

	private void setTimeStamp( long currentTime, QueueEntry nextEntry ) {
		if ( nextEntry != null ) {
			nextEntry.fTimestamp = currentTime;
			
			if ( DEBUG ) {
				String commandText = nextEntry.fCommandToken.getCommand().toString();
				if ( commandText.endsWith( "\n" ) ) //$NON-NLS-1$
					commandText = commandText.substring( 0, commandText.length() - 1 );
				printDebugMessage( String.format( "Setting the timestamp for command '%s' to %d", commandText, Long.valueOf( currentTime ) ) ); //$NON-NLS-1$
			}
		}
	}
}
