/*******************************************************************************
 * Copyright (c) 2011, 2012 Mentor Graphics and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Vladimir Prus (Mentor Graphics) - initial API and implementation
 *******************************************************************************/

package org.eclipse.cdt.dsf.gdb.internal.ui.osview;

import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.cdt.dsf.concurrent.DataRequestMonitor;
import org.eclipse.cdt.dsf.concurrent.Query;
import org.eclipse.cdt.dsf.datamodel.DMContexts;
import org.eclipse.cdt.dsf.datamodel.IDMContext;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService;
import org.eclipse.cdt.dsf.debug.service.command.ICommandControlService.ICommandControlDMContext;
import org.eclipse.cdt.dsf.gdb.internal.GdbPlugin;
import org.eclipse.cdt.dsf.gdb.launching.GdbLaunch;
import org.eclipse.cdt.dsf.service.DsfServicesTracker;
import org.eclipse.cdt.dsf.service.DsfSession;
import org.eclipse.cdt.dsf.ui.viewmodel.datamodel.IDMVMContext;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.DebugContextEvent;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextManager;
import org.eclipse.debug.ui.contexts.IDebugContextService;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Link;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.progress.UIJob;
import org.osgi.framework.Bundle;

public class OSResourcesView extends ViewPart implements DsfSession.SessionEndedListener {

	private final static String FETCH_LINK_TAG = "fetch"; //$NON-NLS-1$

	// The data model for the selected session, or null if no session is
	// selected.
	private SessionOSData fSessionData;
	private Map<String, SessionOSData> fSessionDataCache = new HashMap<String, SessionOSData>();
	private OSData fShownData = null;
	private String fResourceClass = null;


	// UI objects
	private TableViewer fViewer;
	private Comparator fComparator;
	private Composite fNothingLabelContainer;
	private Link fNothingLabel;
	private ResourceClassContributionItem fResourceClassEditor;
	private Action fRefreshAction;

	// Map from resource class name to table column layout.
	private Map<String, ColumnLayout> fColumnLayouts = new HashMap<String, ColumnLayout>();

	private ColumnLayout fColumnLayout = null;

	private IDebugContextListener fDebugContextListener;

	@Override
	public void createPartControl(Composite xparent) {

		Composite parent = new Composite(xparent, SWT.NONE);
		GridLayout topLayout = new GridLayout(1, false);
		topLayout.marginWidth = 0;
		topLayout.marginHeight = 0;
		parent.setLayout(topLayout);

		fViewer = new TableViewer(parent, SWT.MULTI | SWT.H_SCROLL
				| SWT.V_SCROLL);
		GridData viewerData = new GridData(GridData.FILL_BOTH);
		viewerData.exclude = true;
		fViewer.getControl().setLayoutData(viewerData);

		fViewer.setComparator(fComparator = new Comparator());

		Table table = fViewer.getTable();
		table.setHeaderVisible(true);
		table.setVisible(false);

		fNothingLabelContainer = new Composite(parent, SWT.NONE);

		GridData nothingLayout = new GridData(SWT.FILL, SWT.FILL, true, true);
		fNothingLabelContainer.setLayoutData(nothingLayout);

		GridLayout containerLayout = new GridLayout(1, false);
		fNothingLabelContainer.setLayout(containerLayout);

		fNothingLabel = new Link(fNothingLabelContainer, SWT.BORDER);
		fNothingLabel.setText(Messages.OSView_0);
		fNothingLabel.setBackground(fNothingLabel.getDisplay().getSystemColor(
				SWT.COLOR_LIST_BACKGROUND));
		fNothingLabel.addListener (SWT.Selection, new Listener () {
			@Override
			public void handleEvent(Event event) {
				if (event.text.equals("fetch")) //$NON-NLS-1$
					if (fSessionData != null && fResourceClass != null)
						fSessionData.fetchData(fResourceClass);
			}
		});
		fNothingLabelContainer.setBackground(fNothingLabel.getBackground());

		GridData nothingLabelLayout = new GridData(SWT.CENTER, SWT.TOP, true, false);
		fNothingLabel.setLayoutData(nothingLabelLayout);

		fResourceClassEditor = new ResourceClassContributionItem();
		fResourceClassEditor.setListener(new ResourceClassContributionItem.Listener() {

			@Override
			public void resourceClassChanged(String newClass) {
				fResourceClass = newClass;
				// Since user explicitly changed the class, initiate fetch immediately.
				fSessionData.fetchData(fResourceClass);
				update();
			}
		});
		IActionBars bars = getViewSite().getActionBars();
		bars.getToolBarManager().add(fResourceClassEditor);

		fRefreshAction = new Action() {
			@Override
			public void run() {
				if (fSessionData != null && fResourceClass != null)
					fSessionData.fetchData(fResourceClass);
			}
		};
		fRefreshAction.setText(Messages.OSView_3);
		fRefreshAction.setToolTipText(Messages.OSView_3);
        try {
            Bundle bundle= Platform.getBundle("org.eclipse.ui"); //$NON-NLS-1$
            URL url = bundle.getEntry("/"); //$NON-NLS-1$
            url = new URL(url, "icons/full/elcl16/refresh_nav.gif"); //$NON-NLS-1$
            ImageDescriptor candidate = ImageDescriptor.createFromURL(url);
            if (candidate != null && candidate.getImageData() != null) {
                fRefreshAction.setImageDescriptor(candidate);
            }
        } catch (Exception e) {
        }
        bars.getToolBarManager().add(fRefreshAction);
        // Note: we don't need to call 'bars.updateActionBars()' at this point. We're in the middle of creating
        // a view, and therefore toolbar will be built later.

        fResourceClass = fResourceClassEditor.getResourceClassId();

		setupContextListener();
		DsfSession.addSessionEndedListener(this);
	}

	private void setupContextListener() {
		IDebugContextManager contextManager = DebugUITools.getDebugContextManager();
		IDebugContextService contextService = contextManager
				.getContextService(getSite().getWorkbenchWindow());

		fDebugContextListener = new IDebugContextListener() {

			@Override
			public void debugContextChanged(DebugContextEvent event) {

				if ((event.getFlags() & DebugContextEvent.ACTIVATED) != 0) {
					ISelection s = event.getContext();
					setDebugContext(s);
				}
			}
		};
		contextService.addDebugContextListener(fDebugContextListener);
		setDebugContext(contextService.getActiveContext());
	}

	@Override
	public void dispose() {
		super.dispose();

		IDebugContextManager contextManager = DebugUITools.getDebugContextManager();
		IDebugContextService contextService = contextManager
				.getContextService(getSite().getWorkbenchWindow());
		contextService.removeDebugContextListener(fDebugContextListener);

		setDebugContext((IDMContext)null);
		DsfSession.removeSessionEndedListener(this);
	}

	private void setDebugContext(ISelection s) {

		IDMContext context = null;
		if (s instanceof IStructuredSelection) {
			IStructuredSelection ss = (IStructuredSelection) s;
			if (ss.size() > 0) {
				Object obj = ss.getFirstElement();
				if (obj instanceof IDMVMContext)
					context = ((IDMVMContext)obj).getDMContext();
				else if (obj instanceof GdbLaunch) {
					GdbLaunch l = (GdbLaunch)obj;
					final DsfServicesTracker tracker = new DsfServicesTracker(GdbPlugin.getBundleContext(), l.getSession().getId());
					Query<IDMContext> contextQuery = new Query<IDMContext>() {
						@Override
						protected void execute(DataRequestMonitor<IDMContext> rm) {
							ICommandControlService commandControl = tracker.getService(ICommandControlService.class);
							if (commandControl != null)
							{
								rm.setData(commandControl.getContext());
							}
							rm.done();
						}
					};
					l.getSession().getExecutor().submit(contextQuery);
					try {
						context = contextQuery.get();
					} catch (Exception e) {
					}
				}
			}
		}
		setDebugContext(context);
	}

	private void setDebugContext(IDMContext context)
	{
		DsfSession newSession = null;

		if (context != null)
		{
			newSession = DsfSession.getSession(context.getSessionId());

			fSessionData = fSessionDataCache.get(newSession.getId());
			if (fSessionData == null)
			{
				ICommandControlDMContext dmCommandContext
					= DMContexts.getAncestorOfType(context, ICommandControlDMContext.class);

				fSessionData = new SessionOSData(newSession, dmCommandContext);
				fSessionDataCache.put(newSession.getId(), fSessionData);

				fSessionData.setUIListener(new SessionOSData.Listener() {

					@Override
					public void update() {
						// Note that fSessionData always calls the listener in
						// UI thread, so we can directly call 'update' here.
						OSResourcesView.this.update();
					}
				}, fViewer.getControl());
			}
		}


		if (newSession == null)
		{
			fSessionData = null;
		}
		update();
	}

	@Override
	public void sessionEnded(DsfSession session) {
		String id = session.getId();
		SessionOSData data = fSessionDataCache.remove(id);
		if (data != null) {
			data.dispose();
		}
	}

	private void update() {

		if (fViewer.getControl().isDisposed())
			return;

		if (fSessionData == null)
		{
			hideTable(Messages.OSView_4);
			fResourceClassEditor.setEnabled(false);
			fRefreshAction.setEnabled(false);
			return;
		}

		boolean enable = fSessionData.canFetchData();
		fRefreshAction.setEnabled(enable);
		fResourceClass = fResourceClassEditor.updateClasses(fSessionData.getResourceClasses());
		fResourceClassEditor.setEnabled(enable);

		if (!fSessionData.osResourcesSupported()) {
			fRefreshAction.setEnabled(false);
			fResourceClassEditor.setEnabled(false);
			hideTable(Messages.OSView_10);
			return;
		}

		if (fSessionData.fetchingClasses()) {
			fRefreshAction.setEnabled(false);
			fResourceClassEditor.setEnabled(false);
			hideTable(Messages.OSView_11);
			return;
		}

		if (fResourceClass == null)
		{
			fRefreshAction.setEnabled(false);
			fResourceClassEditor.setEnabled(true);
			hideTable(Messages.OSView_5);
			return;
		}

		final OSData data = fSessionData.existingData(fResourceClass);

		if (fSessionData.fetchingContent())
		{
			hideTable(Messages.OSView_6);
		}
		else if (data == null)
		{
			hideTable(NLS.bind(Messages.OSView_7, FETCH_LINK_TAG));
		}
		else
		{
			SimpleDateFormat format = new SimpleDateFormat(Messages.OSView_8);
			fRefreshAction.setToolTipText(format.format(fSessionData.timestamp(fResourceClass)));
			if (data != fShownData)
			{
				fShownData = data;
				Job job = new UIJob(Messages.OSView_9) {
					@Override
					public IStatus runInUIThread(IProgressMonitor monitor) {
						populateTable(data);
						showTable();
						return Status.OK_STATUS;
					}

				};
				job.setPriority(Job.INTERACTIVE);
				job.schedule();
			}
			else
			{
				showTable();
			}
		}
		fRefreshAction.setEnabled(fSessionData.canFetchData());
		fResourceClassEditor.setEnabled(fSessionData.canFetchData());

	}

	/* Hide the table that would show OS awareness data if it were available. Display
	 * 'message' instead.
	 */
	private void hideTable(String message) {
		setContentDescription(""); //$NON-NLS-1$
		fViewer.getControl().setVisible(false);
		((GridData) fViewer.getControl().getLayoutData()).exclude = true;
		fNothingLabelContainer.setVisible(true);
		((GridData) fNothingLabelContainer.getLayoutData()).exclude = false;
		fNothingLabelContainer.getParent().layout();
		fNothingLabel.setText(message);
		fNothingLabelContainer.layout();

		// If the table is not shown, we don't want the menu to have stale
		// list of columns to select from.
		IActionBars bars = getViewSite().getActionBars();
		bars.getMenuManager().removeAll();
		bars.updateActionBars();
	}

	private void showTable() {
		fViewer.getControl().setVisible(true);
		((GridData) fViewer.getControl().getLayoutData()).exclude = false;
		fNothingLabelContainer.setVisible(false);
		((GridData) fNothingLabelContainer.getLayoutData()).exclude = true;
		fNothingLabelContainer.getParent().layout();

		populateViewMenu(fShownData);
	}

	private void populateTable(final OSData data)
	{
		final Table table = fViewer.getTable();

		while (table.getColumnCount() > 0)
			table.getColumns()[0].dispose();

		fColumnLayout = fColumnLayouts.get(fResourceClass);
		if (fColumnLayout == null)
		{
			fColumnLayout = new ColumnLayout(fResourceClass);
			fColumnLayouts.put(fResourceClass, fColumnLayout);
		}

		populateViewMenu(data);

		int sortColumn = fColumnLayout.getSortColumn();
		if (sortColumn < data.getColumnCount())
		{
			fComparator.setColumn(sortColumn,
					data.getColumnIsInteger(sortColumn));
		}
		fComparator.setDirection(fColumnLayout.getSortDirection());

		fViewer.getTable().setEnabled(true);


		if (fViewer.getContentProvider() == null)
		{
			ContentLabelProviderWrapper<OSData> wrapper =
					new ContentLabelProviderWrapper<OSData>(data);
			fViewer.setContentProvider(wrapper);
			fViewer.setLabelProvider(wrapper);
		}
		else
		{
			// Retarget current content/label providers in atomic fashion. See comments
			// on ContentLabelProviderWrapper.
			@SuppressWarnings("unchecked")
			ContentLabelProviderWrapper<OSData> wrapper = (ContentLabelProviderWrapper<OSData>)fViewer.getContentProvider();
			wrapper.setData(data);
		}
		fViewer.setInput(getViewSite());
		fViewer.getControl().setVisible(true);

		for (int i = 0; i < fViewer.getTable().getColumnCount(); ++i)
		{
			TableColumn col = fViewer.getTable().getColumns()[i];
			String cn = col.getText();

			if (i == sortColumn) {
				table.setSortDirection(fColumnLayout.getSortDirection() == 1 ? SWT.DOWN : SWT.UP);
				table.setSortColumn(col);
			}

			if (fColumnLayout.getVisible(cn))
			{
				int w = fColumnLayout.getWidth(cn);
				if (w > 0)
					col.setWidth(w);
				else
					col.pack();
			}
			else
			{
				col.setWidth(0);
				col.setResizable(false);
			}
		}
	}

	private void populateViewMenu(final OSData data) {

		final Table table = fViewer.getTable();

		IActionBars bars = getViewSite().getActionBars();
		bars.getMenuManager().setVisible(true);
		bars.getMenuManager().removeAll();

		for (int i = 0; i < data.getColumnCount(); ++i) {
			final String cn = data.getColumnName(i);
			final TableColumn c = new TableColumn(table, SWT.LEFT);
			c.setText(cn);

			c.addListener(SWT.Resize, new Listener() {

				@Override
				public void handleEvent(Event event) {
					fColumnLayout.setWidth(cn, c.getWidth());
				}
			});

			final int final_index = i;
			c.addSelectionListener(new SelectionAdapter() {
				@Override
				public void widgetSelected(SelectionEvent e) {
					int dir = table.getSortDirection();
					if (table.getSortColumn() == c) {
						dir = dir == SWT.UP ? SWT.DOWN : SWT.UP;
					} else {
						dir = SWT.DOWN;
					}
					table.setSortDirection(dir);
					table.setSortColumn(c);
					fComparator.setColumn(final_index, data.getColumnIsInteger(final_index));
					fComparator.setDirection(dir == SWT.DOWN ? 1 : -1);
					fColumnLayout.setSortColumn(final_index);
					fColumnLayout.setSortDirection(dir == SWT.DOWN ? 1 : -1);
					fViewer.refresh();
				}
			});

			Action a = new Action(cn, IAction.AS_CHECK_BOX) {
				@Override
				public void run() {
					if (isChecked()) {
						int w = fColumnLayout.getWidth(cn);
						if (w > 0)
							c.setWidth(w);
						else
							c.pack();
						c.setResizable(true);
					} else {
						int w = c.getWidth();
						c.setWidth(0);
						// Make sure we remember the width the column
						// had before hiding.
						fColumnLayout.setWidth(cn, w);
						c.setResizable(false);
					}
					fColumnLayout.setVisible(cn, isChecked());
				}
			};
			a.setChecked(fColumnLayout.getVisible(cn));
			a.setText(cn);
			bars.getMenuManager().add(a);
		}
		bars.updateActionBars();
	}

	class Comparator extends ViewerComparator
	{
		private int fColumn = 0;
		private boolean fInteger = false;
		private int fDirection = 1;

		public void setColumn(int column, boolean isInteger)
		{
			fColumn = column;
			fInteger = isInteger;
		}

		public void setDirection(int direction)
		{
			assert direction == 1 || direction == -1;
			fDirection = direction;
		}

		@Override
		public int compare(Viewer viewer, Object xe1, Object xe2) {

			String[] e1 = (String[])xe1;
			String[] e2 = (String[])xe2;
			if (fInteger) {
				Integer i1 = Integer.parseInt(e1[fColumn]);
				Integer i2 = Integer.parseInt(e2[fColumn]);
				return fDirection*(i1 - i2);
			} else {
				return fDirection*(e1[fColumn].compareTo(e2[fColumn]));
			}
		}
	};

	@Override
	public void setFocus() {
		fViewer.getControl().setFocus();
	}
}
