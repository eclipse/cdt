/*******************************************************************************
 * Copyright (c) 2007, 2014 Ericsson and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 * 
 * Contributors:
 *     Ericsson - Initial Implementation
 *     Simon Marchi (Ericsson) - Use runningOnWindows().
 *******************************************************************************/
package org.eclipse.cdt.tests.dsf.gdb.tests;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.debug.core.ICDTLaunchConfigurationConstants;
import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.RequestMonitor;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.debug.service.IBreakpoints;
import org.eclipse.cdt.dsf.debug.service.IBreakpoints.IBreakpointDMContext;
import org.eclipse.cdt.dsf.debug.service.IBreakpoints.IBreakpointDMData;
import org.eclipse.cdt.dsf.debug.service.IBreakpoints.IBreakpointsAddedEvent;
import org.eclipse.cdt.dsf.debug.service.IBreakpoints.IBreakpointsRemovedEvent;
import org.eclipse.cdt.dsf.debug.service.IBreakpoints.IBreakpointsTargetDMContext;
import org.eclipse.cdt.dsf.debug.service.IBreakpoints.IBreakpointsUpdatedEvent;
import org.eclipse.cdt.dsf.debug.service.IExpressions;
import org.eclipse.cdt.dsf.debug.service.IExpressions.IExpressionDMContext;
import org.eclipse.cdt.dsf.debug.service.IFormattedValues;
import org.eclipse.cdt.dsf.debug.service.IFormattedValues.FormattedValueDMContext;
import org.eclipse.cdt.dsf.debug.service.IFormattedValues.FormattedValueDMData;
import org.eclipse.cdt.dsf.debug.service.IRunControl.IContainerDMContext;
import org.eclipse.cdt.dsf.debug.service.IStack.IFrameDMContext;
import org.eclipse.cdt.dsf.gdb.internal.GdbDebugOptions;
import org.eclipse.cdt.dsf.gdb.internal.GdbPlugin;
import org.eclipse.cdt.dsf.mi.service.MIBreakpointDMData;
import org.eclipse.cdt.dsf.mi.service.MIBreakpoints;
import org.eclipse.cdt.dsf.mi.service.MIBreakpoints.MIBreakpointDMContext;
import org.eclipse.cdt.dsf.mi.service.MIRunControl;
import org.eclipse.cdt.dsf.mi.service.command.events.MIBreakpointHitEvent;
import org.eclipse.cdt.dsf.mi.service.command.events.MIStoppedEvent;
import org.eclipse.cdt.dsf.mi.service.command.events.MIWatchpointScopeEvent;
import org.eclipse.cdt.dsf.mi.service.command.events.MIWatchpointTriggerEvent;
import org.eclipse.cdt.dsf.service.DsfServiceEventHandler;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.cdt.tests.dsf.gdb.framework.AsyncCompletionWaitor;
import org.eclipse.cdt.tests.dsf.gdb.framework.BackgroundRunner;
import org.eclipse.cdt.tests.dsf.gdb.framework.BaseTestCase;
import org.eclipse.cdt.tests.dsf.gdb.framework.SyncUtil;
import org.eclipse.cdt.tests.dsf.gdb.launching.TestsPlugin;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;

/*
 * This is the Breakpoint Service test suite.
 * 
 * It is meant to be a regression suite to be executed automatically against
 * the DSF nightly builds.
 * 
 * It is also meant to be augmented with a proper test case(s) every time a
 * feature is added or in the event (unlikely :-) that a bug is found in the
 * Breakpoint Service.
 * 
 * Refer to the JUnit4 documentation for an explanation of the annotations.
 */

@RunWith(BackgroundRunner.class)
public class MIBreakpointsTest extends BaseTestCase {

    // Global constants
    public static final String PLUGIN_ID   = "org.eclipse.cdt.debug.core" ; //$NON-NLS-1$

    public static final String SOURCE_PROJECT = "MIBreakpointsTest";
    public static final String SOURCE_FOLDER  = "src";
    public static final String SOURCE_NAME    = "BreakpointTestApp.cc"; //$NON-NLS-1$
    public static final String EXEC_NAME      = "BreakpointTestApp.exe"; //$NON-NLS-1$

    // Asynchronous Completion
    protected final AsyncCompletionWaitor fWait = new AsyncCompletionWaitor();

    // Services references
    protected DsfSession          fSession;
    protected IBreakpointsTargetDMContext fBreakpointsDmc;
    protected DsfServicesTracker  fServicesTracker;
    protected MIRunControl        fRunControl;
    protected IBreakpoints        fBreakpointService;
    protected IExpressions        fExpressionService;

    // Event Management
    protected static Boolean lock = true;
    protected enum Events { BP_ADDED, BP_UPDATED, BP_REMOVED, BP_HIT, WP_HIT, WP_OOS }
    protected final int BP_ADDED   = Events.BP_ADDED.ordinal();
    protected final int BP_UPDATED = Events.BP_UPDATED.ordinal();
    protected final int BP_REMOVED = Events.BP_REMOVED.ordinal();
    protected final int BP_HIT     = Events.BP_HIT.ordinal();
    protected final int WP_HIT     = Events.WP_HIT.ordinal();
    protected final int WP_OOS     = Events.WP_OOS.ordinal();
    protected int[]   fBreakpointEvents = new int[Events.values().length];
    protected int     fBreakpointEventCount;
    protected int     fBreakpointRef;

    // Some useful constants
	protected final String BREAKPOINT_TYPE_TAG = MIBreakpoints.BREAKPOINT_TYPE;
	protected final String BREAKPOINT_TAG = MIBreakpoints.BREAKPOINT;
	protected final String WATCHPOINT_TAG = MIBreakpoints.WATCHPOINT;

	protected final String FILE_NAME_TAG    = MIBreakpoints.FILE_NAME;
	protected final String LINE_NUMBER_TAG  = MIBreakpoints.LINE_NUMBER;
	protected final String FUNCTION_TAG     = MIBreakpoints.FUNCTION;
	protected final String ADDRESS_TAG      = MIBreakpoints.ADDRESS;
	protected final String CONDITION_TAG    = MIBreakpoints.CONDITION;
	protected final String IGNORE_COUNT_TAG = MIBreakpoints.IGNORE_COUNT;
	protected final String IS_ENABLED_TAG   = MIBreakpoints.IS_ENABLED;
	protected final String THREAD_ID_TAG    = MIBreakpointDMData.THREAD_ID;
	protected final String NUMBER_TAG       = MIBreakpointDMData.NUMBER;

	protected final String EXPRESSION_TAG  = MIBreakpoints.EXPRESSION;
	protected final String READ_TAG        = MIBreakpoints.READ;
	protected final String WRITE_TAG       = MIBreakpoints.WRITE;

    // Target application 'special' locations
	protected  final int    LINE_NUMBER_1   = 20;
	protected  final int    LINE_NUMBER_2   = 21;
	protected  final int    LINE_NUMBER_3   = 27;
    protected  final int    LINE_NUMBER_4   = 36;
    protected  final int    LINE_NUMBER_5   = 49;
    protected  final int    LINE_NUMBER_6   = 50;
    protected  final String FUNCTION        = "zeroBlocks";
    protected  final String SIGNED_FUNCTION = "zeroBlocks(int)";
    protected  final String NO_CONDITION    = "";

    // NOTE: The back-end can reformat the condition. In order for the
    // comparison to work, better specify the condition as the back-end
    // would have it.
    private final String CONDITION_1     = "i == 128";
    private final String CONDITION_2     = "i == 64";
    private final String CONDITION_3     = "j == 20";
    private final String CONDITION_4     = "a == 20";
    private final String CONDITION_5     = "a == 10";
    private final int    IGNORE_COUNT_1  = 128;
    private final int    IGNORE_COUNT_2  = 20;

    private final String EXPRESSION_1 = "charBlock[20]";
    private final String EXPRESSION_2 = "j";
    private final String EXPRESSION_3 = "a";

    // Error messages
    protected final String UNKNOWN_EXECUTION_CONTEXT    = "Unknown execution context";
    protected final String INVALID_BREAKPOINT_LOCATION  = "Invalid breakpoint location";
    protected final String BREAKPOINT_INSERTION_FAILURE = "Breakpoint insertion failure";
    protected final String UNKNOWN_BREAKPOINT           = "Unknown breakpoint";
    
    // ========================================================================
    // Housekeeping stuff
    // ========================================================================

    @Override
    protected void setLaunchAttributes() {
    	super.setLaunchAttributes();
    	
        // Select the binary to run the tests against
        setLaunchAttribute(ICDTLaunchConfigurationConstants.ATTR_PROGRAM_NAME, EXEC_PATH + EXEC_NAME);
    }

    @Override
    public void doBeforeTest() throws Exception {
    	super.doBeforeTest();

        // Get a reference to the breakpoint service
        fSession = getGDBLaunch().getSession();
        Runnable runnable = new Runnable() {
            @Override
			public void run() {
                fServicesTracker = new DsfServicesTracker(TestsPlugin.getBundleContext(), fSession.getId());
                assert(fServicesTracker != null);
        		    
                fRunControl = fServicesTracker.getService(MIRunControl.class);
                assert(fRunControl != null);

                fBreakpointService = fServicesTracker.getService(IBreakpoints.class);
                assert(fBreakpointService != null);

                fExpressionService = fServicesTracker.getService(IExpressions.class);
                assert(fExpressionService != null);

                // Register to breakpoint events
                fRunControl.getSession().addServiceEventListener(MIBreakpointsTest.this, null);

                clearEventCounters();
            }
        };
        fSession.getExecutor().submit(runnable).get();

        IContainerDMContext containerDmc = SyncUtil.getContainerContext();
        fBreakpointsDmc = DMContexts.getAncestorOfType(containerDmc, IBreakpointsTargetDMContext.class);
        assert(fBreakpointsDmc != null);
    }

    @Override
    public void doAfterTest() throws Exception {
    	super.doAfterTest();
    	
		// Clear the references (not strictly necessary)
        Runnable runnable = new Runnable() {
            @Override
			public void run() {
            	fRunControl.getSession().removeServiceEventListener(MIBreakpointsTest.this);
            }
        };
        fSession.getExecutor().submit(runnable).get();
        fBreakpointService = null;
        fRunControl = null;
        fServicesTracker.dispose();
        fServicesTracker = null;

        clearEventCounters();
    }

    // ========================================================================
    // Event Management Functions
    // ========================================================================

    /* -----------------------------------------------------------------------
     * eventDispatched
     * ------------------------------------------------------------------------
     * Processes BreakpointHitEvent.
     * ------------------------------------------------------------------------
     * @param e The BreakpointEvent
     * ------------------------------------------------------------------------
     */
  	@DsfServiceEventHandler
	public void eventDispatched(IBreakpointsAddedEvent e) {
  		synchronized (lock) {
  			if(GdbDebugOptions.DEBUG) GdbDebugOptions.trace(GdbPlugin.getDebugTime() + " Got bp added event\n");
  			fBreakpointEvents[BP_ADDED]++;
  			fBreakpointEventCount++;
  			fBreakpointRef = ((MIBreakpointDMContext) e.getBreakpoints()[0]).getReference();
  			lock.notifyAll();
  		}
	}

  	@DsfServiceEventHandler
	public void eventDispatched(IBreakpointsUpdatedEvent e) {
  		synchronized (lock) {
  			if(GdbDebugOptions.DEBUG) GdbDebugOptions.trace(GdbPlugin.getDebugTime() + " Got bp updated event\n");
  			fBreakpointEvents[BP_UPDATED]++;
  			fBreakpointEventCount++;
  			fBreakpointRef = ((MIBreakpointDMContext) e.getBreakpoints()[0]).getReference();
  			lock.notifyAll();
  		}
	}

  	@DsfServiceEventHandler
	public void eventDispatched(IBreakpointsRemovedEvent e) {
  		synchronized (lock) {
  			if(GdbDebugOptions.DEBUG) GdbDebugOptions.trace(GdbPlugin.getDebugTime() + " Got bp removed event\n");
  			fBreakpointEvents[BP_REMOVED]++;
  			fBreakpointEventCount++;
  			fBreakpointRef = ((MIBreakpointDMContext) e.getBreakpoints()[0]).getReference();
  			lock.notifyAll();
  		}
	}

  	@DsfServiceEventHandler
	public void eventDispatched(MIBreakpointHitEvent e) {
  		synchronized (lock) {
  			if(GdbDebugOptions.DEBUG) GdbDebugOptions.trace(GdbPlugin.getDebugTime() + " Got bp hit event\n");
  			fBreakpointEvents[BP_HIT]++;
  			fBreakpointEventCount++;
  			fBreakpointRef = e.getNumber();
  			lock.notifyAll();
  		}
	}

  	@DsfServiceEventHandler
	public void eventDispatched(MIWatchpointTriggerEvent e) {
  		synchronized (lock) {
  			if(GdbDebugOptions.DEBUG) GdbDebugOptions.trace(GdbPlugin.getDebugTime() + " Got wp hit event\n");
  			fBreakpointEvents[WP_HIT]++;
  			fBreakpointEventCount++;
  			fBreakpointRef = e.getNumber();
  			lock.notifyAll();
  		}
	}

  	@DsfServiceEventHandler
	public void eventDispatched(MIWatchpointScopeEvent e) {
  		synchronized (lock) {
  			if(GdbDebugOptions.DEBUG) GdbDebugOptions.trace(GdbPlugin.getDebugTime() + " Got wp scope event\n");
  			fBreakpointEvents[WP_OOS]++;
  			fBreakpointEventCount++;
  			fBreakpointRef = e.getNumber();
  			lock.notifyAll();
  		}
	}

	// Clears the counters
	protected void clearEventCounters() {
		synchronized (lock) {
			for (int i = 0; i < fBreakpointEvents.length; i++) {
				fBreakpointEvents[i] = 0;
			}
			fBreakpointEventCount = 0;
		}
	}

	// Get the breakpoint hit count
	protected int getBreakpointEventCount(int event) {
		int count = 0;
  		synchronized (lock) {
  			count = fBreakpointEvents[event];
  		}
		return count;
	}

	/**
	 * Suspends the calling thread until [count] number of breakpoint events
	 * have been received in the current test. NOTE: too simple for real life
	 * but good enough for this test suite
	 * 
	 * @param count
	 *            the number breakpoint events to wait for
	 * @param timeout
	 *            max wait time, in milliseconds
	 */
	private void waitForBreakpointEvent(int count, int timeout) throws Exception {
		long startMs = System.currentTimeMillis();
		synchronized (lock) {
			// Make sure we don't wait forever, in case an event never
			// arrives.  The test will check if everything was received
			while (fBreakpointEventCount < count) {
				try {
					lock.wait(30);
				} catch (InterruptedException ex) {
				}
				if (System.currentTimeMillis() - startMs > timeout) {
					throw new Exception("Timed out waiting for " + count + " breakpoint events to occur. Only " + fBreakpointEventCount + " occurred.");
				}
			}
		}
	}
	
	/**
	 * Simplified variant that just waits up to two seconds
	 */
	protected void waitForBreakpointEvent(int count) throws Exception {
		waitForBreakpointEvent(count, TestsPlugin.massageTimeout(2000));
	}

    // ========================================================================
    // Helper Functions
    // ========================================================================

    /* ------------------------------------------------------------------------
     * evaluateExpression
     * ------------------------------------------------------------------------
     * Invokes the ExpressionService to evaluate an expression. In theory,
     * we shouldn't rely on another service to test this one but we need a
     * way to access a variable from the test application in order verify
     * that the memory operations (read/write) are working properly.
     * ------------------------------------------------------------------------
     * @param expression Expression to resolve @return Resolved expression
     * @throws InterruptedException
     * ------------------------------------------------------------------------
     */
    private BigInteger evaluateExpression(IDMContext ctx, String expression) throws Throwable {

        // Get a stack context (temporary - should be an MIcontainerDMC)
		final IExpressionDMContext expressionDMC = SyncUtil.createExpression(ctx, expression);
		final FormattedValueDMContext formattedValueDMC = SyncUtil.getFormattedValue(fExpressionService,
				expressionDMC, IFormattedValues.DECIMAL_FORMAT);

		// Create the DataRequestMonitor which will store the operation result in the wait object
		final DataRequestMonitor<FormattedValueDMData> drm =
			new DataRequestMonitor<FormattedValueDMData>(fSession.getExecutor(), null) {
			@Override
			protected void handleCompleted() {
				if (isSuccess()) {
					fWait.setReturnInfo(getData());
				}
				fWait.waitFinished(getStatus());
			}
		};

		// Evaluate the expression (asynchronously)
		fWait.waitReset();
		fSession.getExecutor().submit(new Runnable() {
			@Override
			public void run() {
				fExpressionService.getFormattedExpressionValue(formattedValueDMC, drm);
			}
		});

		// Wait for completion
		fWait.waitUntilDone(TestsPlugin.massageTimeout(2000));
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Return the string formatted by the back-end
		String result = "";
		Object returnInfo = fWait.getReturnInfo();
		if (returnInfo instanceof FormattedValueDMData)
			result = ((FormattedValueDMData) returnInfo).getFormattedValue();
		return new BigInteger(result);
    }

    /* ------------------------------------------------------------------------
     * getBreakpoints
     * ------------------------------------------------------------------------
     * Retrieves the installed breakpoints list
     * ------------------------------------------------------------------------
     * Typical usage:
     *    IBreakpointDMContext[] breakpoints = getBreakpoints(context);
     * ------------------------------------------------------------------------
     * @param context       the execution context
     * ------------------------------------------------------------------------
     */
    protected IBreakpointDMContext[] getBreakpoints(final IBreakpointsTargetDMContext context) throws InterruptedException
    {
    	// Clear the completion waiter
		fWait.waitReset();

        // Set the Request Monitor
        final DataRequestMonitor<IBreakpointDMContext[]> drm =
            new DataRequestMonitor<IBreakpointDMContext[]>(fBreakpointService.getExecutor(), null) {
                @Override
                protected void handleCompleted() {
                    fWait.waitFinished(getStatus());
                }
            };

        // Issue the breakpoint request
        fWait.waitReset();
        fBreakpointService.getExecutor().submit(new Runnable() {
            @Override
			public void run() {
                fBreakpointService.getBreakpoints(context, drm);
            }
        });

        // Wait for completion
        fWait.waitUntilDone(TestsPlugin.massageTimeout(2000));
        assertTrue(fWait.getMessage(), fWait.isOK());

        // Return the string formatted by the back-end
        return drm.getData();
    }

    /* ------------------------------------------------------------------------
     * getBreakpoint
     * ------------------------------------------------------------------------
     * Retrieves the installed breakpoint
     * ------------------------------------------------------------------------
     * Typical usage:
     *    IBreakpointDMContext breakpoint = ...;
     *    IBreakpointDMData bp = getBreakpoint(breakpoint);
     * ------------------------------------------------------------------------
     * @param breakpoint    the breakpoint to retrieve
     * ------------------------------------------------------------------------
     */
    protected IBreakpointDMData getBreakpoint(final IBreakpointDMContext breakpoint) throws InterruptedException
    {
    	// Clear the completion waiter
		fWait.waitReset();

        // Set the Request Monitor
        final DataRequestMonitor<IBreakpointDMData> drm =
            new DataRequestMonitor<IBreakpointDMData>(fBreakpointService.getExecutor(), null) {
                @Override
                protected void handleCompleted() {
                    fWait.waitFinished(getStatus());
                }
            };

        // Issue the breakpoint request
        fWait.waitReset();
        fBreakpointService.getExecutor().submit(new Runnable() {
            @Override
			public void run() {
                fBreakpointService.getBreakpointDMData(breakpoint, drm);
            }
        });

        // Wait for completion
        fWait.waitUntilDone(TestsPlugin.massageTimeout(2000));
        assertTrue(fWait.getMessage(), fWait.isOK());

        // Return the string formatted by the back-end
        return drm.getData();
    }

	/**
	 * Utility method for setting a line breakpoint in the test's source file and
	 * then running to it.
	 * 
	 * @param lineNumber the line to set the breakpoint on
	 * @return the breakpoint DM context
	 * @throws Throwable
	 */
    private IBreakpointDMContext insertAndRunToLineBreakpoint(int lineNumber) throws Throwable {
    	clearEventCounters();
    	
		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, lineNumber);

		// Install the breakpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);

		clearEventCounters();
		SyncUtil.resumeUntilStopped(2000);
		waitForBreakpointEvent(1);	// breakpoint hit 
		clearEventCounters();
		
		return ref;
    }
    
    /* ------------------------------------------------------------------------
     * insertBreakpoint
     * ------------------------------------------------------------------------
     * Issues an add breakpoint request.
     * ------------------------------------------------------------------------
     * Typical usage:
     *    bp = insertBreakpoint(context, attributes);
     *    assertTrue(fWait.getMessage(), fWait.isOK());
     * ------------------------------------------------------------------------
     * @param context       the execution context
     * @param attributes    the breakpoint attributes
     * ------------------------------------------------------------------------
     */
    protected IBreakpointDMContext insertBreakpoint(final IBreakpointsTargetDMContext context,
    		final Map<String,Object> attributes) throws InterruptedException
    {
    	// Clear the completion waiter
		fWait.waitReset();

		// Set the Request Monitor
        final DataRequestMonitor<IBreakpointDMContext> drm =
            new DataRequestMonitor<IBreakpointDMContext>(fBreakpointService.getExecutor(), null) {
                @Override
                protected void handleCompleted() {
                    fWait.waitFinished(getStatus());
                }
            };

        // Issue the remove insertion request
        fBreakpointService.getExecutor().submit(new Runnable() {
            @Override
			public void run() {
                fBreakpointService.insertBreakpoint(context, attributes, drm);
            }
        });

        // Wait for the result and return the breakpoint id
        fWait.waitUntilDone(TestsPlugin.massageTimeout(2000));
        return drm.getData();
    }

    /* ------------------------------------------------------------------------
     * removeBreakpoint
     * ------------------------------------------------------------------------
     * Issues a remove breakpoint request.
     * ------------------------------------------------------------------------
     * Typical usage:
     *    IBreakpointDMContext breakpoint = ...;
     *    removeBreakpoint(context, breakpoint);
     *    assertTrue(fWait.getMessage(), fWait.isOK());
     * ------------------------------------------------------------------------
     * @param breakpoint the breakpoint to remove
     * ------------------------------------------------------------------------
     */
    private void removeBreakpoint(final IBreakpointDMContext breakpoint) throws InterruptedException
    {
    	// Clear the completion waiter
		fWait.waitReset();

        // Set the Request Monitor
        final RequestMonitor rm =
            new RequestMonitor(fBreakpointService.getExecutor(), null) {
                @Override
                protected void handleCompleted() {
                    fWait.waitFinished(getStatus());
                }
            };

        // Issue the add breakpoint request
        fBreakpointService.getExecutor().submit(new Runnable() {
            @Override
			public void run() {
                fBreakpointService.removeBreakpoint(breakpoint, rm);
            }
        });

        // Wait for the result
        fWait.waitUntilDone(TestsPlugin.massageTimeout(2000));
    }

    /* ------------------------------------------------------------------------
     * updateBreakpoint
     * ------------------------------------------------------------------------
     * Issues an update breakpoint request.
     * ------------------------------------------------------------------------
     * Typical usage:
     *    updateBreakpoint(context, breakpoint, properties);
     *    assertTrue(fWait.getMessage(), fWait.isOK());
     * ------------------------------------------------------------------------
     * @param breakpoint the breakpoint to update
     * @param delta      the delta properties
     * ------------------------------------------------------------------------
     */
    private void updateBreakpoint(final IBreakpointDMContext breakpoint,
    		final Map<String, Object> delta) throws InterruptedException
    {
    	// Clear the completion waiter
		fWait.waitReset();

        // Set the Request Monitor
        final RequestMonitor rm =
            new RequestMonitor(fBreakpointService.getExecutor(), null) {
                @Override
                protected void handleCompleted() {
                    fWait.waitFinished(getStatus());
                }
            };

        // Issue the update breakpoint request
        fBreakpointService.getExecutor().submit(new Runnable() {
            @Override
			public void run() {
                fBreakpointService.updateBreakpoint(breakpoint, delta, rm);
            }
        });

        // Wait for the result
        fWait.waitUntilDone(TestsPlugin.massageTimeout(2000));
    }

    // ========================================================================
    // Test Cases
    // ------------------------------------------------------------------------
    // Templates:
    // ------------------------------------------------------------------------
    // @Test
    // public void basicTest() {
    //     // First test to run
    //     assertTrue("", true);
    // }
    // ------------------------------------------------------------------------
    // @Test(timeout=5000)
    // public void timeoutTest() {
    //     // Second test to run, which will timeout if not finished on time
    //     assertTrue("", true);
    // }
    // ------------------------------------------------------------------------
    // @Test(expected=FileNotFoundException.class)
    // public void exceptionTest() throws FileNotFoundException {
    //     // Third test to run which expects an exception
    //     throw new FileNotFoundException("Just testing");
    // }
    // ========================================================================

	///////////////////////////////////////////////////////////////////////////
    // Add Breakpoint tests
    ///////////////////////////////////////////////////////////////////////////

    // ------------------------------------------------------------------------
	// insertBreakpoint_InvalidContext
	// ------------------------------------------------------------------------
    @Test
	public void insertBreakpoint_InvalidContext() throws Throwable {

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);

		// Perform the test
		String expected = UNKNOWN_EXECUTION_CONTEXT;
		insertBreakpoint(null, breakpoint);
		assertFalse(fWait.getMessage(), fWait.isOK());
		assertTrue("Wrong error message: expected '" + expected + "', received '" + fWait.getMessage() + "'",
				fWait.getMessage().contains(expected));

		// Ensure that no BreakpointEvent was received
		assertTrue("BreakpointEvent problem: expected " + 0 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 0);
    }

    // ------------------------------------------------------------------------
	// insertBreakpoint_InvalidFileName
	// ------------------------------------------------------------------------
	@Test
	public void insertBreakpoint_InvalidFileName() throws Throwable {

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME + "_bad");
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);

		// Perform the test
		String expected = BREAKPOINT_INSERTION_FAILURE;
		insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertFalse(fWait.getMessage(), fWait.isOK());
		assertTrue("Wrong error message: expected '" + expected + "', received '" + fWait.getMessage() + "'",
				fWait.getMessage().contains(expected));

		// Ensure that no BreakpointEvent was received
		assertTrue("BreakpointEvent problem: expected " + 0 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 0);
	}

	// ------------------------------------------------------------------------
	// insertBreakpoint_InvalidLineNumber
	// ------------------------------------------------------------------------
	@Test
	public void insertBreakpoint_InvalidLineNumber() throws Throwable {

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, 0);

		// Perform the test
		String expected = BREAKPOINT_INSERTION_FAILURE;
		insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertFalse(fWait.getMessage(), fWait.isOK());
		assertTrue("Wrong error message: expected '" + expected + "', received '" + fWait.getMessage() + "'",
				fWait.getMessage().contains(expected));

		// Ensure that no BreakpointEvent was received
		assertTrue("BreakpointEvent problem: expected " + 0 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 0);
	}

	// ------------------------------------------------------------------------
	// insertBreakpoint_InvalidFunctionName
	// ------------------------------------------------------------------------
	@Test
	public void insertBreakpoint_InvalidFunctionName() throws Throwable {

		// Create a function breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(FUNCTION_TAG, "invalid-function-name");

		// Perform the test
		String expected = BREAKPOINT_INSERTION_FAILURE;
		insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertFalse(fWait.getMessage(), fWait.isOK());
		assertTrue("Wrong error message: expected '" + expected + "', received '" + fWait.getMessage() + "'",
				fWait.getMessage().contains(expected));

		// Ensure that no BreakpointEvent was received
		assertTrue("BreakpointEvent problem: expected " + 0 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 0);
	}

	// ------------------------------------------------------------------------
	// insertBreakpoint_InvalidAddress
	// ------------------------------------------------------------------------
	@Test
	public void insertBreakpoint_InvalidAddress() throws Throwable {

		// Create an address breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(ADDRESS_TAG, "0x0z");

		// Perform the test
		String expected = BREAKPOINT_INSERTION_FAILURE;
		insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertFalse(fWait.getMessage(), fWait.isOK());
		assertTrue("Wrong error message: expected '" + expected + "', received '" + fWait.getMessage() + "'",
				fWait.getMessage().contains(expected));

		// Ensure that no BreakpointEvent was received
		assertTrue("BreakpointEvent problem: expected " + 0 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 0);
	}


	// ------------------------------------------------------------------------
	// insertBreakpoint_Address
	// Set a breakpoint on an address
	// Ensure that it is set correctly at the back-end.
	// ------------------------------------------------------------------------
	@Test
	public void insertBreakpoint_Address() throws Throwable {

		// Create an address breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		final BigInteger ADDRESS= new BigInteger("00affe00", 16);
		breakpoint.put(ADDRESS_TAG, "0x"+ADDRESS.toString(16));

		// Perform the test
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Ensure that the breakpoint was correctly installed
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertEquals("BreakpointService problem: breakpoint mismatch (wrong address)",
				ADDRESS, breakpoint1.getAddresses()[0].getValue());
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong condition)",
				breakpoint1.getCondition().equals(NO_CONDITION));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong ignore count)",
				breakpoint1.getIgnoreCount() == 0);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong state)",
				breakpoint1.isEnabled());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!breakpoint1.isPending());

		// Ensure the BreakpointService holds only the right breakpoints
		IBreakpointDMContext[] breakpoints = getBreakpoints(fBreakpointsDmc);
		assertTrue("BreakpointService problem: expected " + 1 + " breakpoint(s), received "
				+ breakpoints.length, breakpoints.length == 1);
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(breakpoints[0]);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				breakpoint1.equals(breakpoint2));
	}

	// ------------------------------------------------------------------------
	// insertBreakpoint_LineNumber
	// Set a breakpoint on a line number.
	// Ensure that it is set correctly at the back-end.
	// ------------------------------------------------------------------------
	@Test
	public void insertBreakpoint_LineNumber() throws Throwable {

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);

		// Perform the test
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Ensure that the breakpoint was correctly installed
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong file name)",
				breakpoint1.getFileName().equals(SOURCE_NAME));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong line number)",
				breakpoint1.getLineNumber() == LINE_NUMBER_1);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong condition)",
				breakpoint1.getCondition().equals(NO_CONDITION));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong ignore count)",
				breakpoint1.getIgnoreCount() == 0);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong state)",
				breakpoint1.isEnabled());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!breakpoint1.isPending());

		// Ensure the BreakpointService holds only the right breakpoints
		IBreakpointDMContext[] breakpoints = getBreakpoints(fBreakpointsDmc);
		assertTrue("BreakpointService problem: expected " + 1 + " breakpoint(s), received "
				+ breakpoints.length, breakpoints.length == 1);
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(breakpoints[0]);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				breakpoint1.equals(breakpoint2));
	}

	// ------------------------------------------------------------------------
	// insertBreakpoint_Disabled
	// Set a disabled breakpoint on a line number.
	// Ensure that it is set correctly at the back-end.
	// ------------------------------------------------------------------------
	@Test
	public void insertBreakpoint_Disabled() throws Throwable {

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);
		breakpoint.put(IS_ENABLED_TAG, false);

		// Perform the test
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Ensure that the breakpoint was correctly installed
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong file name)",
				breakpoint1.getFileName().equals(SOURCE_NAME));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong line number)",
				breakpoint1.getLineNumber() == LINE_NUMBER_1);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong condition)",
				breakpoint1.getCondition().equals(NO_CONDITION));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong ignore count)",
				breakpoint1.getIgnoreCount() == 0);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong state)",
				!breakpoint1.isEnabled());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!breakpoint1.isPending());

		// Ensure the BreakpointService holds only the right breakpoints
		IBreakpointDMContext[] breakpoints = getBreakpoints(fBreakpointsDmc);
		assertTrue("BreakpointService problem: expected " + 1 + " breakpoint(s), received "
				+ breakpoints.length, breakpoints.length == 1);
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(breakpoints[0]);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				breakpoint1.equals(breakpoint2));
    }

	// ------------------------------------------------------------------------
	// insertBreakpoint_FunctionName
	// Set a breakpoint on a function name.
	// Ensure that it is set correctly at the back-end.
	// ------------------------------------------------------------------------
	@Test
	public void insertBreakpoint_FunctionName() throws Throwable {

		// Create a function breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(FUNCTION_TAG, FUNCTION);

		// Perform the test
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Ensure that the breakpoint was correctly installed
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong file name)",
				breakpoint1.getFileName().equals(SOURCE_NAME));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong function)",
				breakpoint1.getFunctionName().equals(SIGNED_FUNCTION));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong condition)",
				breakpoint1.getCondition().equals(NO_CONDITION));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong ignore count)",
				breakpoint1.getIgnoreCount() == 0);
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!breakpoint1.isPending());

		// Ensure the BreakpointService holds only the right breakpoints
		IBreakpointDMContext[] breakpoints = getBreakpoints(fBreakpointsDmc);
		assertTrue("BreakpointService problem: expected " + 1 + " breakpoint(s), received "
				+ breakpoints.length, breakpoints.length == 1);
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(breakpoints[0]);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				breakpoint1.equals(breakpoint2));
	}

	// ------------------------------------------------------------------------
	// insertBreakpoint_Condition
	// Set a conditional breakpoint.
	// Ensure that it is set correctly at the back-end.
	// ------------------------------------------------------------------------
	@Test
	public void insertBreakpoint_Condition() throws Throwable {

		// Create a conditional line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);
		breakpoint.put(CONDITION_TAG, CONDITION_1);

		// Perform the test
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Ensure that the breakpoint was correctly installed
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong file name)",
				breakpoint1.getFileName().equals(SOURCE_NAME));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong line number)",
				breakpoint1.getLineNumber() == LINE_NUMBER_1);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong condition)",
				breakpoint1.getCondition().equals(CONDITION_1));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong ignore count)",
				breakpoint1.getIgnoreCount() == 0);
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!breakpoint1.isPending());

		// Ensure the BreakpointService holds only the right breakpoints
		IBreakpointDMContext[] breakpoints = getBreakpoints(fBreakpointsDmc);
		assertTrue("BreakpointService problem: expected " + 1 + " breakpoint(s), received "
				+ breakpoints.length, breakpoints.length == 1);
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(breakpoints[0]);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				breakpoint1.equals(breakpoint2));
	}

 	// ------------------------------------------------------------------------
	// insertBreakpoint_IgnoreCnt
	// Set a breakpoint with an ignore count.
	// Ensure that it is set correctly at the back-end.
	// ------------------------------------------------------------------------
	@Test
	public void insertBreakpoint_IgnoreCnt() throws Throwable {

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);
		breakpoint.put(IGNORE_COUNT_TAG, IGNORE_COUNT_1);

		// Perform the test
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Ensure that the breakpoint was correctly installed
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong file name)",
				breakpoint1.getFileName().equals(SOURCE_NAME));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong line number)",
				breakpoint1.getLineNumber() == LINE_NUMBER_1);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong condition)",
				breakpoint1.getCondition().equals(NO_CONDITION));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong ignore count)",
				breakpoint1.getIgnoreCount() == IGNORE_COUNT_1);
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!breakpoint1.isPending());

		// Ensure the BreakpointService holds only the right breakpoints
		IBreakpointDMContext[] breakpoints = getBreakpoints(fBreakpointsDmc);
		assertTrue("BreakpointService problem: expected " + 1 + " breakpoint(s), received "
				+ breakpoints.length, breakpoints.length == 1);
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(breakpoints[0]);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				breakpoint1.equals(breakpoint2));
 	}

	// ------------------------------------------------------------------------
	// insertBreakpoint_MultipleBreakpoints
	// Set multiple distinct breakpoints.
	// Ensure that the state is kosher.
	// ------------------------------------------------------------------------
	@Test
	public void insertBreakpoint_MultipleBreakpoints() throws Throwable {
		
		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);

		// Perform the test
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Ensure that the breakpoint was correctly installed
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong file name)",
				breakpoint1.getFileName().equals(SOURCE_NAME));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong line number)",
				breakpoint1.getLineNumber() == LINE_NUMBER_1);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong condition)",
				breakpoint1.getCondition().equals(NO_CONDITION));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong ignore count)",
				breakpoint1.getIgnoreCount() == 0);
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!breakpoint1.isPending());

		// Create a function breakpoint
		breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(FUNCTION_TAG, FUNCTION);

		// Perform the test
		ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Ensure that the breakpoint was correctly installed
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong file name)",
				breakpoint2.getFileName().equals(SOURCE_NAME));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong function)",
				breakpoint2.getFunctionName().equals(SIGNED_FUNCTION));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong condition)",
				breakpoint2.getCondition().equals(NO_CONDITION));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong ignore count)",
				breakpoint2.getIgnoreCount() == 0);
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!breakpoint2.isPending());

		// Ensure the BreakpointService holds only the right breakpoints
		IBreakpointDMContext[] breakpoints = getBreakpoints(fBreakpointsDmc);
		assertTrue("BreakpointService problem: expected " + 2 + " breakpoint(s), received "
				+ breakpoints.length, breakpoints.length == 2);
		MIBreakpointDMData svc_bp1 = (MIBreakpointDMData) getBreakpoint(breakpoints[0]);
		MIBreakpointDMData svc_bp2 = (MIBreakpointDMData) getBreakpoint(breakpoints[1]);

		// The breakpoint references are not necessarily retrieved in the order the
		// breakpoints were initially set...
		int ref1 = breakpoint1.getNumber();
		int ref2 = svc_bp1.getNumber();
		if (ref1 == ref2) {
			assertTrue("BreakpointService problem: breakpoint mismatch", svc_bp1.equals(breakpoint1));
			assertTrue("BreakpointService problem: breakpoint mismatch", svc_bp2.equals(breakpoint2));
		} else {
			assertTrue("BreakpointService problem: breakpoint mismatch", svc_bp1.equals(breakpoint2));
			assertTrue("BreakpointService problem: breakpoint mismatch", svc_bp2.equals(breakpoint1));
		}
	}

	// ------------------------------------------------------------------------
	// insertBreakpoint_Duplicate
	// Set 2 identical breakpoints.
	// For GDB, no problem...
	// ------------------------------------------------------------------------
	@Test
	public void insertBreakpoint_Duplicate() throws Throwable {
		
		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);

		// Perform the test
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Ensure that the breakpoint was correctly installed
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong file name)",
				breakpoint1.getFileName().equals(SOURCE_NAME));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong line number)",
				breakpoint1.getLineNumber() == LINE_NUMBER_1);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong condition)",
				breakpoint1.getCondition().equals(NO_CONDITION));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong ignore count)",
				breakpoint1.getIgnoreCount() == 0);
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!breakpoint1.isPending());

		// Create a second line breakpoint, same attributes...
		ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Ensure that the breakpoint was correctly installed
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong file name)",
				breakpoint2.getFileName().equals(SOURCE_NAME));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong line number)",
				breakpoint2.getLineNumber() == LINE_NUMBER_1);
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong condition)",
				breakpoint2.getCondition().equals(NO_CONDITION));
		assertTrue("BreakpointService problem: breakpoint mismatch (wrong ignore count)",
				breakpoint2.getIgnoreCount() == 0);
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!breakpoint2.isPending());

		// Ensure the BreakpointService holds only the right breakpoints
		IBreakpointDMContext[] breakpoints = getBreakpoints(fBreakpointsDmc);
		assertTrue("BreakpointService problem: expected " + 2 + " breakpoint(s), received "
				+ breakpoints.length, breakpoints.length == 2);
		MIBreakpointDMData svc_bp1 = (MIBreakpointDMData) getBreakpoint(breakpoints[0]);
		MIBreakpointDMData svc_bp2 = (MIBreakpointDMData) getBreakpoint(breakpoints[1]);

		// The breakpoint references are not necessarily retrieved in the order the
		// breakpoints were initially set...
		int ref1 = breakpoint1.getNumber();
		int ref2 = svc_bp1.getNumber();
		if (ref1 == ref2) {
			assertTrue("BreakpointService problem: breakpoint mismatch", svc_bp1.equals(breakpoint1));
			assertTrue("BreakpointService problem: breakpoint mismatch", svc_bp2.equals(breakpoint2));
		} else {
			assertTrue("BreakpointService problem: breakpoint mismatch", svc_bp1.equals(breakpoint2));
			assertTrue("BreakpointService problem: breakpoint mismatch", svc_bp2.equals(breakpoint1));
		}
	}

	// ------------------------------------------------------------------------
	// insertBreakpoint_WhileTargetRunning
	// Set breakpoint while the target is running and make sure it eventually
	// gets hit.
	// ------------------------------------------------------------------------
	@Test
	public void insertBreakpoint_WhileTargetRunning() throws Throwable {
		// Interrupting the target on Windows is susceptible to an additional,
		// unwanted suspension. That means that silently interrupting the target
		// to set/modify/remove a breakpoint then resuming it can leave the
		// target in a suspended state. Unfortunately, there is nothing
		// practical CDT can do to address this issue except wait for the gdb
		// folks to resolve it. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=304096#c27
	    if (runningOnWindows()) {
	    	return;
	    }
	    
		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_5);

		// Run the program. It will make a two second sleep() call, during which time... 
		SyncUtil.resume();

		// ...we install the breakpoint
		MIBreakpointDMContext ref = (MIBreakpointDMContext) insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Wait for breakpoint to hit and for the expected number of breakpoint events to have occurred 
		MIStoppedEvent event = SyncUtil.waitForStop(3000);
		waitForBreakpointEvent(2);

    	// Ensure the correct BreakpointEvent was received
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: expected " + 2 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 2);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_HIT event(s), received "
				+ getBreakpointEventCount(BP_HIT), getBreakpointEventCount(BP_HIT) == 1);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				fBreakpointRef == breakpoint1.getNumber());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!breakpoint1.isPending());
		clearEventCounters();
		
		assertTrue("Did not stop because of breakpoint, but stopped because of: " +
				event.getClass().getCanonicalName(), event instanceof MIBreakpointHitEvent);
		assertTrue("Did not stop because of the correct breakpoint at line " + LINE_NUMBER_5,
				   ((MIBreakpointHitEvent)event).getNumber() == ref.getReference());
	}
	
	// ------------------------------------------------------------------------
	// insertInvalidBreakpoint_WhileTargetRunning
	// Set an invalid breakpoint while the target is running, then set a valid
	// breakpoint and make sure the second breakpoints eventually gets hit.
	//
	// We had a problem where an invalid breakpoint set when the target was running
	// would leave us in a bad state (Bug 314628)
	// ------------------------------------------------------------------------
	@Test
	public void insertInvalidBreakpoint_WhileTargetRunning() throws Throwable {
		// Interrupting the target on Windows is susceptible to an additional,
		// unwanted suspension. That means that silently interrupting the target
		// to set/modify/remove a breakpoint then resuming it can leave the
		// target in a suspended state. Unfortunately, there is nothing
		// practical CDT can do to address this issue except wait for the gdb
		// folks to resolve it. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=304096#c27
	    if (runningOnWindows()) {
	    	return;
	    }
		
		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, "Bad file name");
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_5);

		// Run the program. It will make a two second sleep() call, during which time... 
		SyncUtil.resume();

		// ...we install the bad breakpoint and check that it failed
		insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), !fWait.isOK());

		// Now install a proper breakpoint an see that it hits without having to resume
		// the target.  This will show that the target was still properly running.
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		MIBreakpointDMContext ref = (MIBreakpointDMContext) insertBreakpoint(fBreakpointsDmc, breakpoint);

		// Wait for breakpoint to hit and for the expected number of breakpoint events to have occurred 
		MIStoppedEvent event = SyncUtil.waitForStop(3000);
		waitForBreakpointEvent(2);
		
    	// Ensure the correct BreakpointEvent was received
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: expected " + 2 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 2);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_HIT event(s), received "
				+ getBreakpointEventCount(BP_HIT), getBreakpointEventCount(BP_HIT) == 1);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				fBreakpointRef == breakpoint1.getNumber());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!breakpoint1.isPending());
		clearEventCounters();
		
		assertTrue("Did not stop because of breakpoint, but stopped because of: " +
				event.getClass().getCanonicalName(), event instanceof MIBreakpointHitEvent);
		assertTrue("Did not stop because of the correct breakpoint at line " + LINE_NUMBER_5,
				   ((MIBreakpointHitEvent)event).getNumber() == ref.getReference());
	}
	
	///////////////////////////////////////////////////////////////////////////
	// Add Watchpoint tests
	///////////////////////////////////////////////////////////////////////////

	// ------------------------------------------------------------------------
	// insertWatchpoint_Write
	// Set a write watchpoint.
	// Ensure that the state is kosher.
	// ------------------------------------------------------------------------
	@Test
	public void insertWatchpoint_Write() throws Throwable {

		// Create a write watchpoint
		Map<String, Object> watchpoint = new HashMap<String, Object>();
		watchpoint.put(BREAKPOINT_TYPE_TAG, WATCHPOINT_TAG);
		watchpoint.put(EXPRESSION_TAG, EXPRESSION_1);
		watchpoint.put(WRITE_TAG, true);

		// Perform the test
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, watchpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Ensure that the watchpoint was correctly installed
		MIBreakpointDMData watchpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointService problem: watchpoint mismatch (wrong expression)",
				watchpoint1.getExpression().equals(EXPRESSION_1));
		assertTrue("BreakpointService problem: watchpoint mismatch (wrong read state)",
				!watchpoint1.isReadWatchpoint());
		assertTrue("BreakpointService problem: watchpoint mismatch (wrong write state)",
				watchpoint1.isWriteWatchpoint());
		assertTrue("BreakpointService problem: watchpoint mismatch (wrong access state)",
				!watchpoint1.isAccessWatchpoint());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!watchpoint1.isPending());


		// Ensure the BreakpointService holds only the right watchpoints
		IBreakpointDMContext[] watchpoints = getBreakpoints(fBreakpointsDmc);
		assertTrue("BreakpointService problem: expected " + 1 + " watchpoints(s), received "
				+ watchpoints.length, watchpoints.length == 1);
		MIBreakpointDMData watchpoint2 = (MIBreakpointDMData) getBreakpoint(watchpoints[0]);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				watchpoint1.equals(watchpoint2));
	}

	// ------------------------------------------------------------------------
	// insertWatchpoint_Read
	// Set a read watchpoint.
	// Ensure that the state is kosher.
	// ------------------------------------------------------------------------
	@Test
	public void insertWatchpoint_Read() throws Throwable {

		// Create a read watchpoint
		Map<String, Object> watchpoint = new HashMap<String, Object>();
		watchpoint.put(BREAKPOINT_TYPE_TAG, WATCHPOINT_TAG);
		watchpoint.put(EXPRESSION_TAG, EXPRESSION_1);
		watchpoint.put(READ_TAG, true);

		// Perform the test
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, watchpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Ensure that the watchpoint was correctly installed
		MIBreakpointDMData watchpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointService problem: watchpoint mismatch (wrong expression)",
				watchpoint1.getExpression().equals(EXPRESSION_1));
		assertTrue("BreakpointService problem: watchpoint mismatch (wrong read state)",
				watchpoint1.isReadWatchpoint());
		assertTrue("BreakpointService problem: watchpoint mismatch (wrong write state)",
				!watchpoint1.isWriteWatchpoint());
		assertTrue("BreakpointService problem: watchpoint mismatch (wrong access state)",
				!watchpoint1.isAccessWatchpoint());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!watchpoint1.isPending());

		// Ensure the BreakpointService holds only the right watchpoints
		IBreakpointDMContext[] watchpoints = getBreakpoints(fBreakpointsDmc);
		assertTrue("BreakpointService problem: expected " + 1 + " watchpoints(s), received "
				+ watchpoints.length, watchpoints.length == 1);
		MIBreakpointDMData watchpoint2 = (MIBreakpointDMData) getBreakpoint(watchpoints[0]);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				watchpoint1.equals(watchpoint2));
	}

	// ------------------------------------------------------------------------
	// insertWatchpoint_Access
	// Set an access watchpoint.
	// Ensure that the state is kosher.
	// ------------------------------------------------------------------------
	@Test
	public void insertWatchpoint_Access() throws Throwable {

		// Create an access watchpoint
		Map<String, Object> watchpoint = new HashMap<String, Object>();
		watchpoint.put(BREAKPOINT_TYPE_TAG, WATCHPOINT_TAG);
		watchpoint.put(EXPRESSION_TAG, EXPRESSION_1);
		watchpoint.put(READ_TAG, true);
		watchpoint.put(WRITE_TAG, true);

		// Perform the test
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, watchpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Ensure that the watchpoint was correctly installed
		MIBreakpointDMData watchpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointService problem: watchpoint mismatch (wrong expression)",
				watchpoint1.getExpression().equals(EXPRESSION_1));
		assertTrue("BreakpointService problem: watchpoint mismatch (wrong read state)",
				!watchpoint1.isReadWatchpoint());
		assertTrue("BreakpointService problem: watchpoint mismatch (wrong write state)",
				!watchpoint1.isWriteWatchpoint());
		assertTrue("BreakpointService problem: watchpoint mismatch (wrong access state)",
				watchpoint1.isAccessWatchpoint());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!watchpoint1.isPending());

		// Ensure the BreakpointService holds only the right watchpoints
		IBreakpointDMContext[] watchpoints = getBreakpoints(fBreakpointsDmc);
		assertTrue("BreakpointService problem: expected " + 1 + " watchpoints(s), received "
				+ watchpoints.length, watchpoints.length == 1);
		MIBreakpointDMData watchpoint2 = (MIBreakpointDMData) getBreakpoint(watchpoints[0]);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				watchpoint1.equals(watchpoint2));
	}

	// ------------------------------------------------------------------------
	// insertWatchpoint_WhileTargetRunning
	// Set a write watchpoint while the experiment is running.
	// ------------------------------------------------------------------------
	@Ignore("Not supported because the frame where we stop does not contain the expression")
	@Test
	public void insertWatchpoint_WhileTargetRunning() throws Throwable {
		// Interrupting the target on Windows is susceptible to an additional,
		// unwanted suspension. That means that silently interrupting the target
		// to set/modify/remove a breakpoint then resuming it can leave the
		// target in a suspended state. Unfortunately, there is nothing
		// practical CDT can do to address this issue except wait for the gdb
		// folks to resolve it. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=304096#c27
	    if (runningOnWindows()) {
	    	return;
	    }

		// Create a write watchpoint
		Map<String, Object> watchpoint = new HashMap<String, Object>();
		watchpoint.put(BREAKPOINT_TYPE_TAG, WATCHPOINT_TAG);
		watchpoint.put(EXPRESSION_TAG, EXPRESSION_3);
		watchpoint.put(WRITE_TAG, true);

		// Run the program
		SyncUtil.resume();

		// Install watchpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, watchpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Wait for breakpoint to hit and for the expected number of breakpoint events to have occurred 
		MIStoppedEvent event = SyncUtil.waitForStop(3000);
		waitForBreakpointEvent(2);

		// Ensure that right BreakpointEvents were received
		assertTrue("BreakpointEvent problem: expected " + 2 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 2);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_HIT event(s), received "
				+ getBreakpointEventCount(BP_HIT), getBreakpointEventCount(BP_HIT) == 1);
		clearEventCounters();

		// Ensure that the watchpoint was correctly installed
		MIBreakpointDMData watchpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointService problem: watchpoint mismatch (wrong expression)",
				watchpoint1.getExpression().equals(EXPRESSION_1));
		assertTrue("BreakpointService problem: watchpoint mismatch (wrong read state)",
				!watchpoint1.isReadWatchpoint());
		assertTrue("BreakpointService problem: watchpoint mismatch (wrong write state)",
				watchpoint1.isWriteWatchpoint());
		assertTrue("BreakpointService problem: watchpoint mismatch (wrong access state)",
				!watchpoint1.isAccessWatchpoint());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!watchpoint1.isPending());

		// Ensure the BreakpointService holds only the right watchpoints
		IBreakpointDMContext[] watchpoints = getBreakpoints(fBreakpointsDmc);
		assertTrue("BreakpointService problem: expected " + 1 + " watchpoints(s), received "
				+ watchpoints.length, watchpoints.length == 1);
		MIBreakpointDMData watchpoint2 = (MIBreakpointDMData) getBreakpoint(watchpoints[0]);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				watchpoint1.equals(watchpoint2));
				
		assertTrue("Did not stop because of watchpoint, but stopped because of: " +
				event.getClass().getCanonicalName(), event instanceof MIWatchpointTriggerEvent);
		assertTrue("Did not stop because of the watchpoint",
				   ((MIWatchpointTriggerEvent)event).getNumber() == watchpoint1.getReference());
	}

	///////////////////////////////////////////////////////////////////////////
	// Remove Breakpoint tests
	///////////////////////////////////////////////////////////////////////////

	// ------------------------------------------------------------------------
	// removeBreakpoint_SimpleCase
	// Set a breakpoint and then remove it.
	// Ensure that the state is kosher.
	// ------------------------------------------------------------------------
	@Test
	public void removeBreakpoint_SimpleCase() throws Throwable {

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);

		// Install the breakpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Remove the installed breakpoint
		removeBreakpoint(ref);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_REMOVED event(s), received "
				+ getBreakpointEventCount(BP_REMOVED), getBreakpointEventCount(BP_REMOVED) == 1);
		clearEventCounters();

		// Ensure the breakpoint was effectively removed
		IBreakpointDMContext[] breakpoints = getBreakpoints(fBreakpointsDmc);
		assertTrue("BreakpointService problem: expected " + 0 + " breakpoint(s), received "
				+ breakpoints.length, breakpoints.length == 0);
	}

	// ------------------------------------------------------------------------
	// removeBreakpoint_InvalidBreakpoint
	// Try removing a non-existing breakpoint.
	// ------------------------------------------------------------------------
	@Test
	public void removeBreakpoint_InvalidBreakpoint() throws Throwable {

		// Create an invalid breakpoint reference
		IBreakpointDMContext invalid_ref =
			new MIBreakpointDMContext((MIBreakpoints) fBreakpointService, new IDMContext[] { fBreakpointsDmc }, 0);
		
		// Remove the invalid breakpoint
		String expected = UNKNOWN_BREAKPOINT;
		removeBreakpoint(invalid_ref);
		assertFalse(fWait.getMessage(), fWait.isOK());
		assertTrue("Wrong error message: expected '" + expected + "', received '" + fWait.getMessage() + "'",
				fWait.getMessage().contains(expected));

		// Ensure that right BreakpointEvents were received
		assertTrue("BreakpointEvent problem: expected " + 0 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 0);

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);

		// Install the breakpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		IBreakpointDMContext saved_ref = ref;
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Ensure the breakpoint list is OK
		IBreakpointDMContext[] breakpoints = getBreakpoints(fBreakpointsDmc);
		assertTrue("BreakpointService problem: expected " + 1 + " breakpoint(s), received "
				+ breakpoints.length, breakpoints.length == 1);

		// Remove the installed breakpoint
		removeBreakpoint(ref);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_REMOVED event(s), received "
				+ getBreakpointEventCount(BP_REMOVED), getBreakpointEventCount(BP_REMOVED) == 1);
		clearEventCounters();

		// Ensure the breakpoint list is OK
		breakpoints = getBreakpoints(fBreakpointsDmc);
		assertTrue("BreakpointService problem: expected " + 0 + " breakpoint(s), received "
				+ breakpoints.length, breakpoints.length == 0);

		// Remove the un-installed breakpoint
		removeBreakpoint(saved_ref);
		assertFalse(fWait.getMessage(), fWait.isOK());
		assertTrue("Wrong error message: expected '" + expected + "', received '" + fWait.getMessage() + "'",
				fWait.getMessage().contains(expected));

		// Ensure that right BreakpointEvents were received
		assertTrue("BreakpointEvent problem: expected " + 0 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 0);

		// Ensure the breakpoint list is OK
		breakpoints = getBreakpoints(fBreakpointsDmc);
		assertTrue("BreakpointService problem: expected " + 0 + " breakpoint(s), received "
				+ breakpoints.length, breakpoints.length == 0);

		// Re-install the breakpoint
		ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref);

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Remove an un-installed breakpoint (again)
		removeBreakpoint(saved_ref);
		assertFalse(fWait.getMessage(), fWait.isOK());
		assertTrue("Wrong error message: expected '" + expected + "', received '" + fWait.getMessage() + "'",
				fWait.getMessage().contains(expected));

		// Ensure that right BreakpointEvents were received
		assertTrue("BreakpointEvent problem: expected " + 0 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 0);

		// Ensure that the existing breakpoint is unaffected
		breakpoints = getBreakpoints(fBreakpointsDmc);
		assertTrue("BreakpointService problem: expected " + 1 + " breakpoint(s), received "
				+ breakpoints.length, breakpoints.length == 1);
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(breakpoints[0]);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				breakpoint1.equals(breakpoint2));
	}

	// ------------------------------------------------------------------------
	// removeBreakpoint_MixedCase
	// Set a number of breakpoints and then remove them in disorder.
	// Ensure that the right breakpoints are left after each iteration.
	// ------------------------------------------------------------------------
	@Test
	public void removeBreakpoint_MixedCase() throws Throwable {

		// Create a line breakpoint
		for (int i = 0; i < 4; i++) {
			Map<String, Object> breakpoint = new HashMap<String, Object>();
			breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
			breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
			breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1 + i);
			insertBreakpoint(fBreakpointsDmc, breakpoint);
			assertTrue(fWait.getMessage(), fWait.isOK());

			// Ensure that right BreakpointEvents were received
			int expected = i + 1;
			waitForBreakpointEvent(expected);
			assertTrue("BreakpointEvent problem: expected " + expected + " BREAKPOINT event(s), received "
					+ fBreakpointEventCount, fBreakpointEventCount ==  expected);
			assertTrue("BreakpointEvent problem: expected " + expected + " BREAKPOINT_ADDED event(s), received "
					+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == expected);
		}
		clearEventCounters();

		// Get the list of breakpoints
		IBreakpointDMContext[] breakpoints = getBreakpoints(fBreakpointsDmc);
		assertTrue("BreakpointService problem: expected " + 4 + " breakpoint(s), received "
				+ breakpoints.length, breakpoints.length == 4);

		// Remove the breakpoint one at a time in the following order: 1, 3, 2, 4
		int[] indices = { 0, 2, 1, 3 };
		int breakpoints_left = 4;
		for (int i = 0; i < breakpoints_left; i++) {

			// Remove the selected breakpoint
			IBreakpointDMContext index = breakpoints[indices[i]];
			removeBreakpoint(index);
			fWait.waitUntilDone(TestsPlugin.massageTimeout(2000));
			assertTrue(fWait.getMessage(), fWait.isOK());
			breakpoints_left--;

			// Ensure that right BreakpointEvents were received
			int expected = i + 1;
			waitForBreakpointEvent(expected);
			assertTrue("BreakpointEvent problem: expected " + expected + " BREAKPOINT event(s), received "
					+ fBreakpointEventCount, fBreakpointEventCount == expected);
			assertTrue("BreakpointEvent problem: expected " + expected + " BREAKPOINT_REMOVED event(s), received "
					+ getBreakpointEventCount(BP_REMOVED), getBreakpointEventCount(BP_REMOVED) == expected);

	        // Ensure the breakpoint was effectively removed
			IBreakpointDMContext[] remaining_breakpoints = getBreakpoints(fBreakpointsDmc);
			assertTrue("BreakpointService problem: expected " + breakpoints_left + " breakpoint(s), received "
					+ remaining_breakpoints.length, remaining_breakpoints.length == breakpoints_left);
			for (int j = 0; j < breakpoints_left; j++) {
				assertTrue("BreakpointService problem: removed breakpoint still present (" + index + ")",
						!remaining_breakpoints[j].equals(index));
			}
		}
		clearEventCounters();
	}

	// ------------------------------------------------------------------------
	// removeBreakpoint_WhileTargetRunning
	// Remove breakpoint while the target is running and make sure it is does
	// not get hit.
	// ------------------------------------------------------------------------
	@Test
	public void removeBreakpoint_WhileTargetRunning() throws Throwable {
		// Interrupting the target on Windows is susceptible to an additional,
		// unwanted suspension. That means that silently interrupting the target
		// to set/modify/remove a breakpoint then resuming it can leave the
		// target in a suspended state. Unfortunately, there is nothing
		// practical CDT can do to address this issue except wait for the gdb
		// folks to resolve it. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=304096#c27
	    if (runningOnWindows()) {
	    	return;
	    }

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_5);

		// Install the breakpoint
		MIBreakpointDMContext ref = (MIBreakpointDMContext) insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Install a second breakpoint
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_6);

		// Install the breakpoint
		MIBreakpointDMContext ref1 = (MIBreakpointDMContext) insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Run the program
		SyncUtil.resume();
		
		// Remove the first breakpoint
		removeBreakpoint(ref);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Wait for breakpoint to hit and for the expected number of breakpoint events to have occurred 
		MIStoppedEvent event = SyncUtil.waitForStop(3000);
		waitForBreakpointEvent(4);

		// Ensure the correct BreakpointEvent was received
		assertTrue("BreakpointEvent problem: expected " + 4 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 4);
		assertTrue("BreakpointEvent problem: expected " + 2 + " BREAKPOINT_HIT event(s), received "
				+ getBreakpointEventCount(BP_HIT), getBreakpointEventCount(BP_HIT) == 1);
		clearEventCounters();
		
		assertTrue("Did not stop on a breakpoint!",
				event instanceof MIBreakpointHitEvent);
		assertTrue("Did not stop because of the correct breakpoint at line " + LINE_NUMBER_5,
				   ((MIBreakpointHitEvent)event).getNumber() == ref1.getReference());
	}
	
	///////////////////////////////////////////////////////////////////////////
	// Breakpoint Update tests
	///////////////////////////////////////////////////////////////////////////

	// ------------------------------------------------------------------------
	// updateBreakpoint_InvalidBreakpoint
	// Updates a non-existing breakpoint.
	// For GDB, no problem...
	// ------------------------------------------------------------------------
	@Test
	public void updateBreakpoint_InvalidBreakpoint() throws Throwable {

		// Create an invalid breakpoint reference
		IBreakpointDMContext invalid_ref =
			new MIBreakpointDMContext((MIBreakpoints) fBreakpointService, new IDMContext[] { fBreakpointsDmc }, 0);
		
		// Update the invalid breakpoint
		String expected = UNKNOWN_BREAKPOINT;
		Map<String, Object> properties = new HashMap<String, Object>();
		properties.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		properties.put(FILE_NAME_TAG, SOURCE_NAME);
		properties.put(LINE_NUMBER_TAG, LINE_NUMBER_1);
		updateBreakpoint(invalid_ref, properties);
		assertFalse(fWait.getMessage(), fWait.isOK());
		assertTrue("Wrong error message: expected '" + expected + "', received '" + fWait.getMessage() + "'",
				fWait.getMessage().contains(expected));

		// Ensure that no BreakpointEvent was received
		assertTrue("BreakpointEvent problem: expected " + 0 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 0);
	}

	// ------------------------------------------------------------------------
	// updateBreakpoint_AddCondition
	// Set a breakpoint and then add a condition.
	// Ensure that the new breakpoint reflects the changes
	// ------------------------------------------------------------------------
	@Test
	public void updateBreakpoint_AddCondition() throws Throwable {

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);

		// Install the breakpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Modify the condition
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(CONDITION_TAG, CONDITION_1);
		updateBreakpoint(ref, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		clearEventCounters();

		// Verify the state of the breakpoint
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: breakpoint mismatch (wrong condition)",
				breakpoint2.getCondition().equals(CONDITION_1));
	}

	// ------------------------------------------------------------------------
	// updateBreakpoint_RemoveCondition
	// Set a conditional breakpoint and then remove the condition.
	// Ensure that the new breakpoint reflects the changes
	// ------------------------------------------------------------------------
	@Test
	public void updateBreakpoint_RemoveCondition() throws Throwable {

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);
		breakpoint.put(CONDITION_TAG, CONDITION_1);

		// Install the breakpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Remove the condition
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(CONDITION_TAG, null);
		updateBreakpoint(ref, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		clearEventCounters();

		// Verify the state of the breakpoint
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: breakpoint mismatch (wrong condition)",
				breakpoint2.getCondition().equals(""));
	}

	// ------------------------------------------------------------------------
	// updateBreakpoint_ModifyCondition
	// Set a conditional breakpoint and then modify the condition.
	// Ensure that the new breakpoint reflects the changes
	// ------------------------------------------------------------------------
	@Test
	public void updateBreakpoint_ModifyCondition() throws Throwable {

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);
		breakpoint.put(CONDITION_TAG, CONDITION_1);

		// Install the breakpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Update the condition
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(CONDITION_TAG, CONDITION_2);
		updateBreakpoint(ref, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		clearEventCounters();

		// Verify the state of the breakpoint
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: breakpoint mismatch (wrong condition)",
				breakpoint2.getCondition().equals(CONDITION_2));
	}

	// ------------------------------------------------------------------------
	// updateBreakpoint_ModifyCondition_WhileTargetRunning
	// Change the condition of a breakpoint while the target is running and make sure
	// it does get hit.
	// ------------------------------------------------------------------------
	@Test
	public void updateBreakpoint_ModifyCondition_WhileTargetRunning() throws Throwable {
		// Interrupting the target on Windows is susceptible to an additional,
		// unwanted suspension. That means that silently interrupting the target
		// to set/modify/remove a breakpoint then resuming it can leave the
		// target in a suspended state. Unfortunately, there is nothing
		// practical CDT can do to address this issue except wait for the gdb
		// folks to resolve it. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=304096#c27
	    if (runningOnWindows()) {
	    	return;
	    }

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_5);
		breakpoint.put(CONDITION_TAG, CONDITION_4);

		// Install the breakpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Prepare to update the condition
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(CONDITION_TAG, CONDITION_5);
		
		// Run the program
		SyncUtil.resume();
		//Update the condition
		updateBreakpoint(ref, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Wait for breakpoint to hit and for the expected number of breakpoint events to have occurred 
		MIStoppedEvent event = SyncUtil.waitForStop(3000);
		waitForBreakpointEvent(2);

		// Ensure that right BreakpointEvents were received
		assertTrue("BreakpointEvent problem: expected " + 2 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 2);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_HIT event(s), received "
				+ getBreakpointEventCount(BP_HIT), getBreakpointEventCount(BP_HIT) == 1);
		clearEventCounters();
		
		// Verify the state of the breakpoint
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: breakpoint mismatch (wrong condition)",
				breakpoint2.getCondition().equals(CONDITION_5));

		assertTrue("Did not stop on our modified breakpoint!",
				event instanceof MIBreakpointHitEvent);
		assertTrue("Did not stop because of the correct breakpoint at line " + LINE_NUMBER_5,
				   ((MIBreakpointHitEvent)event).getNumber() == breakpoint2.getReference());
	}
	
	// ------------------------------------------------------------------------
	// updateWatchpoint_AddCondition
	// Set a watchpoint and then add a condition.
	// Ensure that the new breakpoint reflects the changes
	// ------------------------------------------------------------------------
	@Test
	public void updateWatchpoint_AddCondition() throws Throwable {
		// Run to the point where the variable is initialized
		insertAndRunToLineBreakpoint(LINE_NUMBER_1);

		// Create a write watchpoint
		Map<String, Object> watchpoint = new HashMap<String, Object>();
		watchpoint.put(BREAKPOINT_TYPE_TAG, WATCHPOINT_TAG);
		watchpoint.put(EXPRESSION_TAG, EXPRESSION_1);
		watchpoint.put(WRITE_TAG, true);

		// Install the watchpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, watchpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Add the condition
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(CONDITION_TAG, CONDITION_1);
		updateBreakpoint(ref, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		clearEventCounters();

		// Verify the state of the watchpoint
		MIBreakpointDMData watchpoint2 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: breakpoint mismatch (wrong condition)",
				watchpoint2.getCondition().equals(CONDITION_1));
	}

	// ------------------------------------------------------------------------
	// updateWatchpoint_RemoveCondition
	// Set a conditional watchpoint and then remove the condition.
	// Ensure that the new breakpoint reflects the changes
	// ------------------------------------------------------------------------
	@Test
	public void updateWatchpoint_RemoveCondition() throws Throwable {
		// Run to the point where the variable is initialized
		insertAndRunToLineBreakpoint(LINE_NUMBER_1);
		
		// Create a write watchpoint
		Map<String, Object> watchpoint = new HashMap<String, Object>();
		watchpoint.put(BREAKPOINT_TYPE_TAG, WATCHPOINT_TAG);
		watchpoint.put(EXPRESSION_TAG, EXPRESSION_1);
		watchpoint.put(WRITE_TAG, true);
		watchpoint.put(CONDITION_TAG, CONDITION_1);

		// Install the watchpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, watchpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Remove the condition
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(CONDITION_TAG, null);
		updateBreakpoint(ref, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		clearEventCounters();

		// Verify the state of the watchpoint
		MIBreakpointDMData watchpoint2 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: breakpoint mismatch (wrong condition)",
				watchpoint2.getCondition().equals(""));
	}

	// ------------------------------------------------------------------------
	// updateWatchpoint_ModifyCondition
	// Set a conditional watchpoint and then modify the condition.
	// Ensure that the new breakpoint reflects the changes
	// ------------------------------------------------------------------------
	@Test
	public void updateWatchpoint_ModifyCondition() throws Throwable {
		// Run to the point where the variable is initialized
		insertAndRunToLineBreakpoint(LINE_NUMBER_1);

		// Create a write watchpoint
		Map<String, Object> watchpoint = new HashMap<String, Object>();
		watchpoint.put(BREAKPOINT_TYPE_TAG, WATCHPOINT_TAG);
		watchpoint.put(EXPRESSION_TAG, EXPRESSION_1);
		watchpoint.put(WRITE_TAG, true);
		watchpoint.put(CONDITION_TAG, CONDITION_1);

		// Install the watchpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, watchpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT events, received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event, received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Update the condition
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(CONDITION_TAG, CONDITION_2);
		updateBreakpoint(ref, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		clearEventCounters();

		// Verify the state of the breakpoint
		MIBreakpointDMData watchpoint2 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: breakpoint mismatch (wrong condition)",
				watchpoint2.getCondition().equals(CONDITION_2));
	}

	// ------------------------------------------------------------------------
	// updateBreakpoint_AddCount
	// Set a breakpoint and then add an ignore count.
	// Ensure that the new breakpoint reflects the changes
	// ------------------------------------------------------------------------
	@Test
	public void updateBreakpoint_AddCount() throws Throwable {

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);

		// Install the breakpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Add a count
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(IGNORE_COUNT_TAG, IGNORE_COUNT_2);
		updateBreakpoint(ref, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		clearEventCounters();

		// Verify the state of the breakpoint
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: breakpoint mismatch (wrong count)",
				breakpoint2.getIgnoreCount() == IGNORE_COUNT_2);
	}

	// ------------------------------------------------------------------------
	// updateBreakpoint_RemoveCount
	// Set a conditional breakpoint and then remove the count..
	// Ensure that the new breakpoint reflects the changes
	// ------------------------------------------------------------------------
	@Test
	public void updateBreakpoint_RemoveCount() throws Throwable {

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);
		breakpoint.put(IGNORE_COUNT_TAG, IGNORE_COUNT_2);

		// Install the breakpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Remove the count
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(IGNORE_COUNT_TAG, null);
		updateBreakpoint(ref, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		clearEventCounters();

		// Verify the state of the breakpoint
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: breakpoint mismatch (wrong count)",
				breakpoint2.getIgnoreCount() == 0);
	}

	// ------------------------------------------------------------------------
	// updateBreakpoint_ModifyCount
	// Set a conditional breakpoint and then modify the count.
	// Ensure that the new breakpoint reflects the changes
	// ------------------------------------------------------------------------
	@Test
	public void updateBreakpoint_ModifyCount() throws Throwable {

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);
		breakpoint.put(IGNORE_COUNT_TAG, IGNORE_COUNT_1);

		// Install the breakpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Update the count
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(IGNORE_COUNT_TAG, IGNORE_COUNT_2);
		updateBreakpoint(ref, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		clearEventCounters();

		// Verify the state of the breakpoint
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: breakpoint mismatch (wrong count)",
				breakpoint2.getIgnoreCount() == IGNORE_COUNT_2);
	}

	// ------------------------------------------------------------------------
	// updateBreakpoint_ModifyCount_WhileTargetRunning
	// Change the ignore count of a breakpoint while the target is running and make sure
	// it does get hit.
	// ------------------------------------------------------------------------
	@Test
	public void updateBreakpoint_ModifyCount_WhileTargetRunning() throws Throwable {
		// Interrupting the target on Windows is susceptible to an additional,
		// unwanted suspension. That means that silently interrupting the target
		// to set/modify/remove a breakpoint then resuming it can leave the
		// target in a suspended state. Unfortunately, there is nothing
		// practical CDT can do to address this issue except wait for the gdb
		// folks to resolve it. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=304096#c27
	    if (runningOnWindows()) {
	    	return;
	    }

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_5);
		breakpoint.put(IGNORE_COUNT_TAG, IGNORE_COUNT_1);

		// Install the breakpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Prepare to update the count
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(IGNORE_COUNT_TAG, 0);
		
		// Run the program
		SyncUtil.resume();

		//Update the count
		updateBreakpoint(ref, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Wait for breakpoint to hit and for the expected number of breakpoint events to have occurred 
		MIStoppedEvent event = SyncUtil.waitForStop(3000);
		waitForBreakpointEvent(2);

		// Ensure that right BreakpointEvents were received
		assertTrue("BreakpointEvent problem: expected " + 2 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 2);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_HIT event(s), received "
				+ getBreakpointEventCount(BP_HIT), getBreakpointEventCount(BP_HIT) == 1);
		clearEventCounters();
		
		// Verify the state of the breakpoint
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: breakpoint mismatch (wrong count)",
				breakpoint2.getIgnoreCount() == 0);

		assertTrue("Did not stop on our modified breakpoint!",
				event instanceof MIBreakpointHitEvent);
		assertTrue("Did not stop because of the correct breakpoint at line " + LINE_NUMBER_5,
				   ((MIBreakpointHitEvent)event).getNumber() == breakpoint2.getReference());	
	}
	
	// ------------------------------------------------------------------------
	// updateBreakpoint_Disable
	// Set 2 breakpoints and disable the first one.
	// Ensure that we stop on the second one.
	// ------------------------------------------------------------------------
	@Test
	public void updateBreakpoint_Disable() throws Throwable {

		// Create a first line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);

		// Install the breakpoint
		IBreakpointDMContext ref1 = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Create a second line breakpoint
		breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_2);

		// Install the breakpoint
		IBreakpointDMContext ref2 = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(2);
		assertTrue("BreakpointEvent problem: expected " + 2 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 2);
		assertTrue("BreakpointEvent problem: expected " + 2 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 2);
		clearEventCounters();

		// Verify the state of the breakpoints
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref1);
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(ref2);
		assertTrue("BreakpointService problem: breakpoint state error",
				breakpoint1.isEnabled() && breakpoint2.isEnabled());

		// Disable the first breakpoint
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(IS_ENABLED_TAG, false);
		updateBreakpoint(ref1, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		clearEventCounters();

		// Verify the state of the breakpoints
		breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref1);
		breakpoint2 = (MIBreakpointDMData) getBreakpoint(ref2);
		assertTrue("BreakpointService problem: breakpoint state error",
				!breakpoint1.isEnabled() && breakpoint2.isEnabled());

		// Run until the breakpoint is hit and the event generated
		SyncUtil.resumeUntilStopped(1000);

		// Ensure the BreakpointEvent was received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_HIT event(s), received "
				+ getBreakpointEventCount(BP_HIT), getBreakpointEventCount(BP_HIT) == 1);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				fBreakpointRef == breakpoint2.getNumber());
		clearEventCounters();
	}

	// ------------------------------------------------------------------------
	// updateBreakpoint_Disable_WhileTargetRunning
	// Disable a breakpoint while the target is running and make sure
	// it does not get hit.
	// ------------------------------------------------------------------------
	@Test
	public void updateBreakpoint_Disable_WhileTargetRunning() throws Throwable {
		// Interrupting the target on Windows is susceptible to an additional,
		// unwanted suspension. That means that silently interrupting the target
		// to set/modify/remove a breakpoint then resuming it can leave the
		// target in a suspended state. Unfortunately, there is nothing
		// practical CDT can do to address this issue except wait for the gdb
		// folks to resolve it. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=304096#c27
	    if (runningOnWindows()) {
	    	return;
	    }

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_5);

		// Install the breakpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Install a second breakpoint
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_6);
		MIBreakpointDMContext ref1 = (MIBreakpointDMContext) insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(2);
		
		assertTrue("BreakpointEvent problem: expected " + 2 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 2);
		assertTrue("BreakpointEvent problem: expected " + 2 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 2);
		clearEventCounters();

		// Prepare to disable the  breakpoint
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(IS_ENABLED_TAG, false);

		// Run the program
		SyncUtil.resume();
		
		// Disable the breakpoint
		updateBreakpoint(ref, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Wait for breakpoint to hit and for the expected number of breakpoint events to have occurred 
		MIStoppedEvent event = SyncUtil.waitForStop(3000);
		waitForBreakpointEvent(2);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_HIT event(s), received "
				+ getBreakpointEventCount(BP_HIT), getBreakpointEventCount(BP_HIT) == 1);
		clearEventCounters();

		assertTrue("Did not stop on a breakpoint!",
				event instanceof MIBreakpointHitEvent);
		assertTrue("Did not stop because of the correct breakpoint at line " + LINE_NUMBER_5,
				   ((MIBreakpointHitEvent)event).getNumber() == ref1.getReference());
	}
	
	// ------------------------------------------------------------------------
	// updateBreakpoint_Enable
	// In a loop, set 2 breakpoints and disable the first one. After hitting
	// the second one, enable the first one again.
	// Ensure that we stop on the first one on the next iteration.
	// ------------------------------------------------------------------------
	@Test
	public void updateBreakpoint_Enable() throws Throwable {

		// Create a first line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);

		// Install the breakpoint
		IBreakpointDMContext ref1 = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Create a second line breakpoint
		breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_2);

		// Install the breakpoint
		IBreakpointDMContext ref2 = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(2);
		assertTrue("BreakpointEvent problem: expected " + 2 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 2);
		assertTrue("BreakpointEvent problem: expected " + 2 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 2);
		clearEventCounters();

		// Verify the state of the breakpoints
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref1);
		MIBreakpointDMData breakpoint2 = (MIBreakpointDMData) getBreakpoint(ref2);
		assertTrue("BreakpointService problem: breakpoint state error",
				breakpoint1.isEnabled() && breakpoint2.isEnabled());

		// Disable the first breakpoint
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(IS_ENABLED_TAG, false);
		updateBreakpoint(ref1, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		clearEventCounters();

		// Verify the state of the breakpoints
		breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref1);
		breakpoint2 = (MIBreakpointDMData) getBreakpoint(ref2);
		assertTrue("BreakpointService problem: breakpoint state error",
				!breakpoint1.isEnabled() && breakpoint2.isEnabled());

		// Run until the breakpoint is hit and the event generated
		SyncUtil.resumeUntilStopped(1000);

		// Ensure the BreakpointEvent was received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_HIT event(s), received "
				+ getBreakpointEventCount(BP_HIT), getBreakpointEventCount(BP_HIT) == 1);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				fBreakpointRef == breakpoint2.getNumber());
		clearEventCounters();

		// Enable the first breakpoint
		delta = new HashMap<String, Object>();
		delta.put(IS_ENABLED_TAG, true);
		updateBreakpoint(ref1, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		clearEventCounters();

		// Verify the state of the breakpoints
		breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref1);
		breakpoint2 = (MIBreakpointDMData) getBreakpoint(ref2);
		assertTrue("BreakpointService problem: breakpoint state error",
				breakpoint1.isEnabled() && breakpoint2.isEnabled());

		// Run until the breakpoint is hit and the event generated
		SyncUtil.resumeUntilStopped(1000);

		// Ensure the BreakpointEvent was received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_HIT event(s), received "
				+ getBreakpointEventCount(BP_HIT), getBreakpointEventCount(BP_HIT) == 1);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				fBreakpointRef == breakpoint1.getNumber());
		clearEventCounters();
	}

	// ------------------------------------------------------------------------
	// updateBreakpoint_Enable_WhileTargetRunning
	// Enable a disabled breakpoint while the target is running and make sure
	// it does get hit.
	// ------------------------------------------------------------------------
	@Test
	public void updateBreakpoint_Enable_WhileTargetRunning() throws Throwable {
		// Interrupting the target on Windows is susceptible to an additional,
		// unwanted suspension. That means that silently interrupting the target
		// to set/modify/remove a breakpoint then resuming it can leave the
		// target in a suspended state. Unfortunately, there is nothing
		// practical CDT can do to address this issue except wait for the gdb
		// folks to resolve it. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=304096#c27
	    if (runningOnWindows()) {
	    	return;
	    }

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_5);
		breakpoint.put(IS_ENABLED_TAG, false);

		// Install the breakpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Prepare to enable the  breakpoint
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(IS_ENABLED_TAG, true);

		// Run the program
		SyncUtil.resume();
		
		// Enable the breakpoint
		updateBreakpoint(ref, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Wait for breakpoint to hit and for the expected number of breakpoint events to have occurred 
		MIStoppedEvent event = SyncUtil.waitForStop(3000);
		waitForBreakpointEvent(2);

		// Ensure that right BreakpointEvents were received
		assertTrue("BreakpointEvent problem: expected " + 2 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 2);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_HIT event(s), received "
				+ getBreakpointEventCount(BP_HIT), getBreakpointEventCount(BP_HIT) == 1);
		clearEventCounters();
		
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("Did not stop on our enabled breakpoint!",
				event instanceof MIBreakpointHitEvent);
		assertTrue("Did not stop because of the correct breakpoint at line " + LINE_NUMBER_5,
				   ((MIBreakpointHitEvent)event).getNumber() == breakpoint1.getReference());	
	}
	///////////////////////////////////////////////////////////////////////////
	// Breakpoint Hit tests
	///////////////////////////////////////////////////////////////////////////

	// ------------------------------------------------------------------------
	// breakpointHit_LineNumber
	// Set a breakpoint on a line number.
	// ------------------------------------------------------------------------
	@Test
	public void breakpointHit_LineNumber() throws Throwable {

		// Create a line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);

		// Install the breakpoint
		MIBreakpointDMContext ref = (MIBreakpointDMContext) insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Run until the breakpoint is hit and the event generated
		SyncUtil.resumeUntilStopped(1000);

		// Ensure the correct BreakpointEvent was received
		waitForBreakpointEvent(1);
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_HIT event(s), received "
				+ getBreakpointEventCount(BP_HIT), getBreakpointEventCount(BP_HIT) == 1);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				fBreakpointRef == breakpoint1.getNumber());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!breakpoint1.isPending());
		clearEventCounters();
	}

	// ------------------------------------------------------------------------
	// breakpointHit_Function
	// Set a breakpoint on a function name.
	// ------------------------------------------------------------------------
	@Test
	public void breakpointHit_Function() throws Throwable {

		// Create a function breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(FUNCTION_TAG, FUNCTION);

		// Install the breakpoint
		MIBreakpointDMContext ref = (MIBreakpointDMContext) insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Run until the breakpoint is hit and the event generated
		SyncUtil.resumeUntilStopped(1000);

		// Ensure the correct BreakpointEvent was received
		waitForBreakpointEvent(1);
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_HIT event(s), received "
				+ getBreakpointEventCount(BP_HIT), getBreakpointEventCount(BP_HIT) == 1);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				fBreakpointRef == breakpoint1.getNumber());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!breakpoint1.isPending());
		clearEventCounters();
	}

	// ------------------------------------------------------------------------
	// breakpointHit_Condition
	// Set a breakpoint on a line where a variable being increased (loop).
	// Set a condition so that the break occurs only after variable == count.
	// Ensure that the variable was increased 'count' times.
	// ------------------------------------------------------------------------
	@Test
	public void breakpointHit_Condition() throws Throwable {

		// Create a conditional line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);
		breakpoint.put(CONDITION_TAG, CONDITION_1);

		// Install the breakpoint
		MIBreakpointDMContext ref = (MIBreakpointDMContext) insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Run until the breakpoint is hit and the event generated
		MIStoppedEvent stoppedEvent = SyncUtil.resumeUntilStopped(2000);
        IFrameDMContext frameDmc = SyncUtil.getStackFrame(stoppedEvent.getDMContext(), 0);

		// Ensure the correct BreakpointEvent was received
		waitForBreakpointEvent(1);
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_HIT event(s), received "
				+ getBreakpointEventCount(BP_HIT), getBreakpointEventCount(BP_HIT) == 1);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				fBreakpointRef == breakpoint1.getNumber());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!breakpoint1.isPending());
		clearEventCounters();

		// Verify that the condition is met
		int i = evaluateExpression(frameDmc, "i").intValue();
		assertTrue("BreakpointEvent problem: breakpoint mismatch (wrong condition)", i == 128);
	}

	// ------------------------------------------------------------------------
	// breakpointHit_UpdatedCondition
	// Set a breakpoint on a line where a variable being increased (loop).
	// Set a condition so that the break occurs only after variable == count.
	// Ensure that the variable was increased 'count' times.
	// ------------------------------------------------------------------------
	@Test
	public void breakpointHit_UpdatedCondition() throws Throwable {

		// Create a conditional line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);

		// Install the breakpoint
		MIBreakpointDMContext ref = (MIBreakpointDMContext) insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Add the condition
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(CONDITION_TAG, CONDITION_1);
		updateBreakpoint(ref, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		clearEventCounters();

		// Run until the breakpoint is hit and the event generated
		MIStoppedEvent stoppedEvent = SyncUtil.resumeUntilStopped(2000);
        IFrameDMContext frameDmc = SyncUtil.getStackFrame(stoppedEvent.getDMContext(), 0);

		// Ensure the correct BreakpointEvent was received
		waitForBreakpointEvent(1);
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_HIT event(s), received "
				+ getBreakpointEventCount(BP_HIT), getBreakpointEventCount(BP_HIT) == 1);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				fBreakpointRef == breakpoint1.getNumber());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!breakpoint1.isPending());
		clearEventCounters();

		// Verify that the condition is met
		int i = evaluateExpression(frameDmc, "i").intValue();
		assertTrue("BreakpointEvent problem: breakpoint mismatch (wrong condition)", i == 128);
	}

	// ------------------------------------------------------------------------
	// breakpointHit_Count
	// Set a breakpoint on a line where a variable being increased (loop).
	// Set an ignore count != 0.
	// Ensure that the variable was increased 'ignoreCount' times.
	// ------------------------------------------------------------------------
	@Test
	public void breakpointHit_Count() throws Throwable {

		// Create a conditional line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);
		breakpoint.put(IGNORE_COUNT_TAG, IGNORE_COUNT_2);

		// Install the breakpoint
		MIBreakpointDMContext ref = (MIBreakpointDMContext) insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Run until the breakpoint is hit and the event generated
		MIStoppedEvent stoppedEvent = SyncUtil.resumeUntilStopped(1000);
        IFrameDMContext frameDmc = SyncUtil.getStackFrame(stoppedEvent.getDMContext(), 0);

		// Ensure the correct BreakpointEvent was received
		waitForBreakpointEvent(1);
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_HIT event(s), received "
				+ getBreakpointEventCount(BP_HIT), getBreakpointEventCount(BP_HIT) == 1);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				fBreakpointRef == breakpoint1.getNumber());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!breakpoint1.isPending());
		clearEventCounters();

		// Verify that the condition is met
		int i = evaluateExpression(frameDmc, "i").intValue();
		assertTrue("BreakpointEvent problem: breakpoint mismatch (wrong condition)", i == IGNORE_COUNT_2);
	}

	// ------------------------------------------------------------------------
	// breakpointHit_UpdatedCount
	// Set a breakpoint on a line where a variable being increased (loop).
	// Set an ignore count != 0.
	// Ensure that the variable was increased 'ignoreCount' times.
	// ------------------------------------------------------------------------
	@Test
	public void breakpointHit_UpdatedCount() throws Throwable {

		// Create a conditional line breakpoint
		Map<String, Object> breakpoint = new HashMap<String, Object>();
		breakpoint.put(BREAKPOINT_TYPE_TAG, BREAKPOINT_TAG);
		breakpoint.put(FILE_NAME_TAG, SOURCE_NAME);
		breakpoint.put(LINE_NUMBER_TAG, LINE_NUMBER_1);

		// Install the breakpoint
		MIBreakpointDMContext ref = (MIBreakpointDMContext) insertBreakpoint(fBreakpointsDmc, breakpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Add a count
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(IGNORE_COUNT_TAG, IGNORE_COUNT_2);
		updateBreakpoint(ref, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		clearEventCounters();

		// Run until the breakpoint is hit and the event generated
		MIStoppedEvent stoppedEvent = SyncUtil.resumeUntilStopped(1000);
        IFrameDMContext frameDmc = SyncUtil.getStackFrame(stoppedEvent.getDMContext(), 0);

		// Ensure the correct BreakpointEvent was received
		waitForBreakpointEvent(1);
		MIBreakpointDMData breakpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_HIT event(s), received "
				+ getBreakpointEventCount(BP_HIT), getBreakpointEventCount(BP_HIT) == 1);
		assertTrue("BreakpointService problem: breakpoint mismatch",
				fBreakpointRef == breakpoint1.getNumber());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!breakpoint1.isPending());
		clearEventCounters();

		// Verify that the condition is met
		int i = evaluateExpression(frameDmc, "i").intValue();
		assertTrue("BreakpointEvent problem: breakpoint mismatch (wrong condition)", i == IGNORE_COUNT_2);
	}

	// ------------------------------------------------------------------------
	// breakpointHit_WriteWatchpoint
	// Set a write watchpoint and go.
	// Ensure that the correct event is received.
	// ------------------------------------------------------------------------
	@Test
	public void breakpointHit_WriteWatchpoint() throws Throwable {

		// Create a write watchpoint
		Map<String, Object> watchpoint = new HashMap<String, Object>();
		watchpoint.put(BREAKPOINT_TYPE_TAG, WATCHPOINT_TAG);
		watchpoint.put(EXPRESSION_TAG, EXPRESSION_1);
		watchpoint.put(WRITE_TAG, true);

		// Install the watchpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, watchpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Run until the breakpoint is hit and the event generated
		MIStoppedEvent stoppedEvent = SyncUtil.resumeUntilStopped(1000);
        IFrameDMContext frameDmc = SyncUtil.getStackFrame(stoppedEvent.getDMContext(), 0);

		// Ensure the correct BreakpointEvent was received
		waitForBreakpointEvent(1);
		MIBreakpointDMData watchpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " WATCHPOINT_HIT event(s), received "
				+ getBreakpointEventCount(WP_HIT), getBreakpointEventCount(WP_HIT) == 1);
		assertTrue("BreakpointService problem: watchpoint mismatch",
				fBreakpointRef == watchpoint1.getNumber());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!watchpoint1.isPending());
		clearEventCounters();

		// Verify that the condition is met
		int i = evaluateExpression(frameDmc, "i").intValue();
		assertTrue("BreakpointEvent problem: breakpoint mismatch (wrong condition)", i == IGNORE_COUNT_2);
	}

	// ------------------------------------------------------------------------
	// breakpointHit_ReadWatchpoint
	// Set a read watchpoint and go.
	// Ensure that the correct event is received.
	// ------------------------------------------------------------------------
	@Test
	public void breakpointHit_ReadWatchpoint() throws Throwable {

		// Create a write watchpoint
		Map<String, Object> watchpoint = new HashMap<String, Object>();
		watchpoint.put(BREAKPOINT_TYPE_TAG, WATCHPOINT_TAG);
		watchpoint.put(EXPRESSION_TAG, EXPRESSION_1);
		watchpoint.put(READ_TAG, true);

		// Install the watchpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, watchpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Run until the breakpoint is hit and the event generated
		MIStoppedEvent stoppedEvent = SyncUtil.resumeUntilStopped(1000);
        IFrameDMContext frameDmc = SyncUtil.getStackFrame(stoppedEvent.getDMContext(), 0);

		// Ensure the correct BreakpointEvent was received
		waitForBreakpointEvent(1);
		MIBreakpointDMData watchpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " WATCHPOINT_HIT event(s), received "
				+ getBreakpointEventCount(WP_HIT), getBreakpointEventCount(WP_HIT) == 1);
		assertTrue("BreakpointService problem: watchpoint mismatch",
				fBreakpointRef == watchpoint1.getNumber());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!watchpoint1.isPending());
		clearEventCounters();

		// Verify that the condition is met
		int i = evaluateExpression(frameDmc, "i").intValue();
		assertTrue("BreakpointEvent problem: breakpoint mismatch (wrong condition)", i == IGNORE_COUNT_2);
	}

	// ------------------------------------------------------------------------
	// breakpointHit_AccessWatchpoint
	// Set an access watchpoint and go.
	// Ensure that the correct event is received.
	// ------------------------------------------------------------------------
	@Test
	public void breakpointHit_AccessWatchpoint() throws Throwable {

		// Create an access watchpoint
		Map<String, Object> watchpoint = new HashMap<String, Object>();
		watchpoint.put(BREAKPOINT_TYPE_TAG, WATCHPOINT_TAG);
		watchpoint.put(EXPRESSION_TAG, EXPRESSION_1);
		watchpoint.put(READ_TAG, true);
		watchpoint.put(WRITE_TAG, true);

		// Install the watchpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, watchpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Run until the breakpoint is hit and the event generated
		MIStoppedEvent stoppedEvent = SyncUtil.resumeUntilStopped(1000);
        IFrameDMContext frameDmc = SyncUtil.getStackFrame(stoppedEvent.getDMContext(), 0);

		// Ensure the correct BreakpointEvent was received
		waitForBreakpointEvent(1);
		MIBreakpointDMData watchpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " WATCHPOINT_HIT event(s), received "
				+ getBreakpointEventCount(WP_HIT), getBreakpointEventCount(WP_HIT) == 1);
		assertTrue("BreakpointService problem: watchpoint mismatch",
				fBreakpointRef == watchpoint1.getNumber());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!watchpoint1.isPending());
		clearEventCounters();

		// Verify that the condition is met
		int i = evaluateExpression(frameDmc, "i").intValue();
		assertTrue("BreakpointEvent problem: breakpoint mismatch (wrong condition)", i == IGNORE_COUNT_2);
	}

	// ------------------------------------------------------------------------
	// breakpointHit_watchpointUpdateCount
	// Set a write watchpoint, add an ignoreCount and go.
	// Ensure that the correct event is received.
	// ------------------------------------------------------------------------
	@Test
	public void breakpointHit_watchpointUpdateCount() throws Throwable {
		// Run to the point where the variable is initialized
		insertAndRunToLineBreakpoint(LINE_NUMBER_4);
		
		// Create a write watchpoint
		Map<String, Object> watchpoint = new HashMap<String, Object>();
		watchpoint.put(BREAKPOINT_TYPE_TAG, WATCHPOINT_TAG);
		watchpoint.put(EXPRESSION_TAG, EXPRESSION_2);
		watchpoint.put(WRITE_TAG, true);

		// Install the watchpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, watchpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Add a count
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(IGNORE_COUNT_TAG, IGNORE_COUNT_2);
		updateBreakpoint(ref, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		clearEventCounters();

		// Run until the breakpoint is hit and the event generated
		MIStoppedEvent stoppedEvent = SyncUtil.resumeUntilStopped(1000);
        IFrameDMContext frameDmc = SyncUtil.getStackFrame(stoppedEvent.getDMContext(), 0);

		// Ensure the correct BreakpointEvent was received
		waitForBreakpointEvent(1);
		MIBreakpointDMData watchpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " WATCHPOINT_HIT event(s), received "
				+ getBreakpointEventCount(WP_HIT), getBreakpointEventCount(WP_HIT) == 1);
		assertTrue("BreakpointService problem: watchpoint mismatch",
				fBreakpointRef == watchpoint1.getNumber());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!watchpoint1.isPending());
		clearEventCounters();

		// Verify that the condition is met
		int j = evaluateExpression(frameDmc, EXPRESSION_2).intValue();
		assertTrue("Watchpoint problem: " + EXPRESSION_2 +" was " + j + " instead of " + IGNORE_COUNT_2,
				   j == IGNORE_COUNT_2);
	}

	// ------------------------------------------------------------------------
	// breakpointHit_watchpointUpdateCondition
	// Set a write watchpoint, add a condition and go.
	// Ensure that the correct event is received.
	// ------------------------------------------------------------------------
	@Test
	public void breakpointHit_watchpointUpdateCondition() throws Throwable {
		// Run to the point where the variable is initialized
		insertAndRunToLineBreakpoint(LINE_NUMBER_4);
		
		// Create a write watchpoint
		Map<String, Object> watchpoint = new HashMap<String, Object>();
		watchpoint.put(BREAKPOINT_TYPE_TAG, WATCHPOINT_TAG);
		watchpoint.put(EXPRESSION_TAG, EXPRESSION_2);
		watchpoint.put(WRITE_TAG, true);

		// Install the watchpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, watchpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Add a condition
		Map<String, Object> delta = new HashMap<String, Object>();
		delta.put(CONDITION_TAG, CONDITION_3);
		updateBreakpoint(ref, delta);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_UPDATED event(s), received "
				+ getBreakpointEventCount(BP_UPDATED), getBreakpointEventCount(BP_UPDATED) == 1);
		clearEventCounters();

		// Run until the breakpoint is hit and the event generated
		MIStoppedEvent stoppedEvent = SyncUtil.resumeUntilStopped();
        IFrameDMContext frameDmc = SyncUtil.getStackFrame(stoppedEvent.getDMContext(), 0);

		// Ensure the correct BreakpointEvent was received
		waitForBreakpointEvent(1);
		MIBreakpointDMData watchpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " WATCHPOINT_HIT event(s), received "
				+ getBreakpointEventCount(WP_HIT), getBreakpointEventCount(WP_HIT) == 1);
		assertTrue("BreakpointService problem: watchpoint mismatch",
				fBreakpointRef == watchpoint1.getNumber());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!watchpoint1.isPending());
		clearEventCounters();

		// Verify that the condition is met
		int j = evaluateExpression(frameDmc, EXPRESSION_2).intValue();
		assertTrue("Watchpoint problem: " + EXPRESSION_2 +" was " + j + " instead of " + 20,
				   j == 20);
	}

	// ------------------------------------------------------------------------
	// breakpointHit_WatchpointOutOfScope
	// Set an access watchpoint and watch it go out of scope.
	// Ensure that the correct event is received.
	// ------------------------------------------------------------------------
	@Ignore("All GDBs seem to have a bug in this situation")
	@Test
	public void breakpointHit_WatchpointOutOfScope() throws Throwable {
		// Run to the point where the variable is initialized
		insertAndRunToLineBreakpoint(LINE_NUMBER_4);

		// Create a write watchpoint
		Map<String, Object> watchpoint = new HashMap<String, Object>();
		watchpoint.put(BREAKPOINT_TYPE_TAG, WATCHPOINT_TAG);
		watchpoint.put(EXPRESSION_TAG, EXPRESSION_2);
		watchpoint.put(READ_TAG, true);
		watchpoint.put(WRITE_TAG, true);
		// Make sure watchpoint is not triggered by the expression actually changing
		watchpoint.put(IGNORE_COUNT_TAG, 1000);

		// Install the watchpoint
		IBreakpointDMContext ref = insertBreakpoint(fBreakpointsDmc, watchpoint);
		assertTrue(fWait.getMessage(), fWait.isOK());

		// Ensure that right BreakpointEvents were received
		waitForBreakpointEvent(1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT_ADDED event(s), received "
				+ getBreakpointEventCount(BP_ADDED), getBreakpointEventCount(BP_ADDED) == 1);
		clearEventCounters();

		// Run until the breakpoint is hit and the event generated
		SyncUtil.resumeUntilStopped();

		// Ensure the correct BreakpointEvent was received
		waitForBreakpointEvent(1);
		MIBreakpointDMData watchpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: expected " + 1 + " BREAKPOINT event(s), received "
				+ fBreakpointEventCount, fBreakpointEventCount == 1);
		assertTrue("BreakpointEvent problem: expected " + 1 + " WATCHPOINT_HIT event(s), received "
				+ getBreakpointEventCount(WP_OOS), getBreakpointEventCount(WP_OOS) == 1);
		assertTrue("BreakpointService problem: watchpoint mismatch",
				fBreakpointRef == watchpoint1.getNumber());
		assertTrue("BreakpointService problem: breakpoint mismatch (pending)",
				!watchpoint1.isPending());
		clearEventCounters();

		// Ensure the watchpoint is gone
		getBreakpoints(fBreakpointsDmc);
		watchpoint1 = (MIBreakpointDMData) getBreakpoint(ref);
		assertTrue("BreakpointEvent problem: expected watchpoint to be deleted after going out of scope",
				watchpoint1 == null);
	}
}
