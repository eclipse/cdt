/*******************************************************************************
 * Copyright (c) 2009, 2014 QNX Software Systems and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     QNX Software Systems - Initial API and implementation
 *     Wind River Systems   - Modified for new DSF Reference Implementation
 *     Ericsson 		  	- Modified for additional features in DSF Reference implementation
 *******************************************************************************/

package org.eclipse.cdt.dsf.mi.service.command;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PipedInputStream;
import java.io.PipedOutputStream;
import java.util.concurrent.RejectedExecutionException;

import org.eclipse.cdt.dsf.concurrent.ConfinedToDsfExecutor;
import org.eclipse.cdt.dsf.concurrent.DsfRunnable;
import org.eclipse.cdt.dsf.concurrent.ThreadSafe;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.debug.service.command.ICommand;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService;
import org.eclipse.cdt.dsf.debug.service.command.ICommandListener;
import org.eclipse.cdt.dsf.debug.service.command.ICommandResult;
import org.eclipse.cdt.dsf.debug.service.command.ICommandToken;
import org.eclipse.cdt.dsf.debug.service.command.IEventListener;
import org.eclipse.cdt.dsf.gdb.internal.GdbPlugin;
import org.eclipse.cdt.dsf.mi.service.command.commands.CLICommand;
import org.eclipse.cdt.dsf.mi.service.command.commands.MICommand;
import org.eclipse.cdt.dsf.mi.service.command.commands.MIInterpreterExecConsole;
import org.eclipse.cdt.dsf.mi.service.command.commands.RawCommand;
import org.eclipse.cdt.dsf.mi.service.command.output.MIConsoleStreamOutput;
import org.eclipse.cdt.dsf.mi.service.command.output.MIInfo;
import org.eclipse.cdt.dsf.mi.service.command.output.MILogStreamOutput;
import org.eclipse.cdt.dsf.mi.service.command.output.MIOOBRecord;
import org.eclipse.cdt.dsf.mi.service.command.output.MIOutput;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.core.runtime.ILog;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

/**
 * This Process implementation tracks the GDB process.  This 
 * process object is displayed in Debug view and is used to
 * accept CLI commands and to write their output to the console.
 * 
 * Note that starting with GDB 7.12, as long as a PTY is available,
 * this process is no longer used.  Instead, the real GDB process
 * along with its console will be used by the user.  A new PTY
 * will be used to communicate using MI.
 * 
 * @see org.eclipse.debug.core.model.IProcess 
 */
@ThreadSafe
public abstract class AbstractCLIProcess extends Process 
    implements IEventListener, ICommandListener 
{
    public static final String PRIMARY_PROMPT = "(gdb)"; //$NON-NLS-1$
    public static final String SECONDARY_PROMPT = ">"; //$NON-NLS-1$
    
    // This is the command that will end a secondary prompt
    private static final String SECONDARY_PROMPT_END_COMMAND = "end"; //$NON-NLS-1$

    private final DsfSession fSession;
    private final ICommandControlService fCommandControl;
	private final OutputStream fOutputStream = new CLIOutputStream();
    
    // Client process console stream.
    private PipedInputStream fMIInConsolePipe;
    private PipedOutputStream fMIOutConsolePipe;
    private PipedInputStream fMIInLogPipe;
    private PipedOutputStream fMIOutLogPipe;

    private boolean fDisposed = false;
    
    /**
     * Counter for tracking console commands sent by services.  
     * 
     * Services may issue console commands when the available MI commands are
     * not sufficient.  However, these commands may produce console and log 
     * output which should not be written to the user CLI terminal.
     *   
     * This counter is incremented any time a console command is seen which was
     * not generated by this class.  It is decremented whenever a service CLI 
     * command is finished.  When counter value is 0, the CLI process writes 
     * the console output. 
     */
    private int fSuppressConsoleOutputCounter = 0;
    
    // Primary prompt == "(gdb)"
    // Secondary Prompt == ">"
    // Secondary_prompt_missing means that the backend should be sending the secondary
    // prompt but it isn't.  So we do it ourselves.
    private enum PromptType { IN_PRIMARY_PROMPT, IN_SECONDARY_PROMPT, IN_SECONDARY_PROMPT_MISSING };
    private PromptType fPrompt = PromptType.IN_PRIMARY_PROMPT;

    /**
     * @since 1.1
     */
    @ConfinedToDsfExecutor("fSession#getExecutor")
	public AbstractCLIProcess(ICommandControlService commandControl) throws IOException {
        fSession = commandControl.getSession();
        fCommandControl = commandControl;
        
        commandControl.addEventListener(this);
        commandControl.addCommandListener(this);

        PipedInputStream miInConsolePipe = null;
        PipedOutputStream miOutConsolePipe = null;
        PipedInputStream miInLogPipe = null;
        PipedOutputStream miOutLogPipe = null;
        
        try {
        	// Using a LargePipedInputStream see https://bugs.eclipse.org/bugs/show_bug.cgi?id=223154
            miOutConsolePipe = new PipedOutputStream();
            miInConsolePipe = new LargePipedInputStream(miOutConsolePipe);
            miOutLogPipe = new PipedOutputStream();
            miInLogPipe = new LargePipedInputStream(miOutLogPipe);
        } catch (IOException e) {
            ILog log = GdbPlugin.getDefault().getLog();
            if (log != null) {
                log.log(new Status(
                    IStatus.ERROR, GdbPlugin.PLUGIN_ID, -1, "Error when creating log pipes", e)); //$NON-NLS-1$
            }                   
        }
        // Must initialize these outside of the try block because they are final.
        fMIOutConsolePipe = miOutConsolePipe;
        fMIInConsolePipe = miInConsolePipe;
        fMIOutLogPipe = miOutLogPipe;
        fMIInLogPipe = miInLogPipe; 
	}
    
    protected DsfSession getSession() { return fSession; }

    /**
     * @since 1.1
     */
	protected ICommandControlService getCommandControlService() { return fCommandControl; }
	
	protected boolean isDisposed() { return fDisposed; }
	
    @ConfinedToDsfExecutor("fSession#getExecutor")
    public void dispose() {
    	if (fDisposed) return;
    	
        fCommandControl.removeEventListener(this);
        fCommandControl.removeCommandListener(this);
        
        closeIO();
        fDisposed = true;

        // We have memory leaks that prevent this class from being
        // GCed.  The problem becomes bad because we are holding
        // two LargePipedInputStream and eventually, the JUnit tests
        // run out of memory.  To address this particular problem,
        // before the actual causes of the leaks are fixed, lets
        // make sure we release all our four streams which all have
        // a reference to a LargePipedInputStream
        // Bug 323071
        fMIInConsolePipe = null;
        fMIInLogPipe = null;
        fMIOutConsolePipe = null;
        fMIOutLogPipe = null;
    }
    
    private void closeIO() {
        try {
            fMIOutConsolePipe.close();
        } catch (IOException e) {}
        try {
            fMIInConsolePipe.close();
        } catch (IOException e) {}
        try {
            fMIOutLogPipe.close();
        } catch (IOException e) {}
        try {
            fMIInLogPipe.close();
        } catch (IOException e) {}
        
    }
    
	/**
	 * @see java.lang.Process#getErrorStream()
	 */
	@Override
    public InputStream getErrorStream() {
        return fMIInLogPipe;
	}

	/**
	 * @see java.lang.Process#getInputStream()
	 */
	@Override
    public InputStream getInputStream() {
        return fMIInConsolePipe;
	}

	/**
	 * @see java.lang.Process#getOutputStream()
	 */
	@Override
    public OutputStream getOutputStream() {
		return fOutputStream;
	}


	@Override
    public void eventReceived(Object output) {
    	if (fSuppressConsoleOutputCounter > 0) return;
    	for (MIOOBRecord oobr : ((MIOutput)output).getMIOOBRecords()) {
    		if (oobr instanceof MIConsoleStreamOutput)  
    		{
                MIConsoleStreamOutput out = (MIConsoleStreamOutput) oobr;
                String str = out.getString();
                
                if (str.trim().equals(SECONDARY_PROMPT)) {
                    // Make sure to skip any secondary prompt that we
                    // have already printed ourselves.  This would happen
                    // when a new version of the backend starts sending
                    // the secondary prompt for a command that it didn't
                    // use to.  In this case, we still send it ourselves.
                	if (inMissingSecondaryPrompt()) {
                		return;
                	}
                	// Add a space for readability
                	str = SECONDARY_PROMPT + ' ';
                }

                setPrompt(str);
                try {
                	if (fMIOutConsolePipe != null) {
                		fMIOutConsolePipe.write(str.getBytes());
                		fMIOutConsolePipe.flush();
                	}
                } catch (IOException e) {
                }
            } else if (oobr instanceof MILogStreamOutput) {
            	MILogStreamOutput out = (MILogStreamOutput) oobr;
                String str = out.getString();
                if (str != null) {
                    try {
                    	if (fMIOutLogPipe != null) {
	                        fMIOutLogPipe.write(str.getBytes());
	                        fMIOutLogPipe.flush();
                    	}
                    } catch (IOException e) {
                    }
                }
            }
        }
    }
    
	@Override
    public void commandQueued(ICommandToken token) {
            // Ignore
    }

	@Override
    public void commandSent(ICommandToken token) {
    	// Bug 285170
    	// Don't reset the fPrompt here, in case we are
    	// dealing with the missing secondary prompt.
    	
        ICommand<?> command = token.getCommand();
        // Check if the command is a CLI command and if it did not originate from this class.
        if ((command instanceof CLICommand<?> || command instanceof MIInterpreterExecConsole<?>) &&
            !(command instanceof ProcessCLICommand || command instanceof ProcessMIInterpreterExecConsole)) 
        {
            fSuppressConsoleOutputCounter++;
        }
        
    	// Bug 285170
        // Deal with missing secondary prompt, if needed.
        // The only two types we care about are ProcessMIInterpreterExecConsole
        // and RawCommand, both of which are MICommands
        if (command instanceof MICommand<?>) {
        	checkMissingSecondaryPrompt((MICommand<?>)command);
        }
    }
    
    private void checkMissingSecondaryPrompt(MICommand<?> command) {                          
        // If the command send is one of ours, check if it is one that is missing a secondary prompt
    	if (command instanceof ProcessMIInterpreterExecConsole) {
    		String[] operations = ((ProcessMIInterpreterExecConsole)command).getParameters();                                                         
    		if (operations != null && operations.length > 0) {                                                     
    			// Get the command name.                                                                           
    			String operation = operations[0];                                                                   
    			int indx = operation.indexOf(' ');                                                                 
    			if (indx != -1) {                                                                                  
    				operation = operation.substring(0, indx).trim();                                               
    			} else {                                                                                           
    				operation = operation.trim();                                                                  
    			}                                                                                                  

    			if (isMissingSecondaryPromptCommand(operation)) {                             
    				// For such commands, the backend does not send the secondary prompt                             
    				// so we set it manually.  We'll remain in this state until we get 
    				// a commandDone() call.
    				// This logic will still work when a new version of the backend
    				// fixes this lack of secondary prompt.
    				fPrompt = PromptType.IN_SECONDARY_PROMPT_MISSING;                                                                                   
    			}                                                                                                   
    		}
    	}
    	
    	// Even if the previous check didn't kick in, we may already be in the missing
    	// secondary prompt case.  If so, we'll print the prompt ourselves.
    	// Just make sure that this command is not ending the secondary prompt.
    	if (fPrompt == PromptType.IN_SECONDARY_PROMPT_MISSING) {
    		String operation = command.getOperation();
    		if (operation != null) {
    			int indx = operation.indexOf(' ');
    			if (indx != -1) {
    				operation = operation.substring(0, indx).trim();
    			} else {
    				operation = operation.trim();
    			}

    			if (!operation.equals(SECONDARY_PROMPT_END_COMMAND)) {
                	// Add a space for readability
                	String str = SECONDARY_PROMPT + ' ';
    				try {
                    	if (fMIOutConsolePipe != null) {
                    		fMIOutConsolePipe.write(str.getBytes());
                    		fMIOutConsolePipe.flush();
                    	}
    				} catch (IOException e) {
    				}
    			}
    		}
    	}
    }

    /**                                                                                                        
     * Check to see if the user typed a command that we know the backend
     * does not send the secondary prompt for, but should.                                                   
     * If so, we'll need to pretend we are receiving the secondary prompt.
     *                                     
     * @since 3.0
     */                                                                                                        
    protected boolean isMissingSecondaryPromptCommand(String operation) {
    	return false;
    }
    
	@Override
    public void commandRemoved(ICommandToken token) {
            // Ignore
    }

	@Override
    public void commandDone(ICommandToken token, ICommandResult result) {
    	// Whenever we get a command that is completed, we know we must be in the primary prompt
    	fPrompt = PromptType.IN_PRIMARY_PROMPT;
    	
        ICommand<?> command = token.getCommand();
    	if ((command instanceof CLICommand<?> || command instanceof MIInterpreterExecConsole<?>) &&
    			!(command instanceof ProcessCLICommand || command instanceof ProcessMIInterpreterExecConsole)) 
        {
            fSuppressConsoleOutputCounter--;
        }
     }

    void setPrompt(String line) {
        fPrompt = PromptType.IN_PRIMARY_PROMPT;
        // See https://bugs.eclipse.org/bugs/show_bug.cgi?id=109733
        if (line == null)
            return;
        line = line.trim();
        if (line.equals(SECONDARY_PROMPT)) {
            fPrompt = PromptType.IN_SECONDARY_PROMPT;
        }
    }

    public boolean inPrimaryPrompt() {
        return fPrompt == PromptType.IN_PRIMARY_PROMPT;
    }

    public boolean inSecondaryPrompt() {
        return fPrompt == PromptType.IN_SECONDARY_PROMPT || fPrompt == PromptType.IN_SECONDARY_PROMPT_MISSING;
    }
    
    /**
	 * @since 3.0
	 */
    public boolean inMissingSecondaryPrompt() {
        return fPrompt == PromptType.IN_SECONDARY_PROMPT_MISSING;
    }

    private boolean isMIOperation(String operation) {
    	// The definition of an MI command states that it starts with
    	//  [ token ] "-"
    	// where 'token' is optional and a sequence of digits.
    	// However, we don't accept a token from the user, because
    	// we will be adding our own token when actually sending the command.
    	if (operation.startsWith("-")) { //$NON-NLS-1$
    		return true;
    	}
    	return false;
    }

    private class CLIOutputStream extends OutputStream {
        private final StringBuffer buf = new StringBuffer();
        
        @Override
        public void write(int b) throws IOException {
            buf.append((char)b);
            if (b == '\n') {
                // Throw away the newline.
                final String bufString = buf.toString().trim();
                buf.setLength(0);
                try {
                    fSession.getExecutor().execute(new DsfRunnable() { @Override public void run() {
                        try {
                            post(bufString);
                        } catch (IOException e) {
                            // Pipe closed.
                        }
                    }});
                } catch (RejectedExecutionException e) {
                    // Session disposed.
                }
            }
        }
                        
        // Encapsulate the string sent to gdb in a fake
        // command and post it to the TxThread.
        public void post(String str) throws IOException {
            if (isDisposed()) return;
            ICommand<MIInfo> cmd = null;
            // 1-
            // if We have the secondary prompt it means
            // that GDB is waiting for more feedback, use a RawCommand
            // 2-
            // Do not use the interpreter-exec for stepping operation
            // the UI will fall out of step.  
            // Also, do not use "interpreter-exec console" for MI commands.
            // 3-
            // Normal Command Line Interface.
            boolean secondary = inSecondaryPrompt();
            if (secondary) {
                cmd = new RawCommand(getCommandControlService().getContext(), str);
            }
            else if (! isMIOperation(str) &&
            		 ! CLIEventProcessor.isSteppingOperation(str))
            {
                cmd = new ProcessMIInterpreterExecConsole(getCommandControlService().getContext(), str);
            } 
            else {
                cmd = new ProcessCLICommand(getCommandControlService().getContext(), str);
            }
            final ICommand<MIInfo> finalCmd = cmd; 
            fSession.getExecutor().execute(new DsfRunnable() { @Override public void run() {
                if (isDisposed()) return;
                // Do not wait around for the answer.
                getCommandControlService().queueCommand(finalCmd, null);
            }});
        }
    }
    
    private class ProcessCLICommand extends CLICommand<MIInfo> {
        public ProcessCLICommand(IDMContext ctx, String oper) {
            super(ctx, oper);
        }
    }
    
    private class ProcessMIInterpreterExecConsole extends MIInterpreterExecConsole<MIInfo> {
        public ProcessMIInterpreterExecConsole(IDMContext ctx, String cmd) {
            super(ctx, cmd);
        }
    }
}
