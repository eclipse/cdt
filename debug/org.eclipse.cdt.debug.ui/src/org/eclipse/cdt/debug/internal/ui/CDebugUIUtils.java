/*******************************************************************************
 * Copyright (c) 2000, 2016 QNX Software Systems and others.
 *
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * which accompanies this distribution, and is available at
 * https://www.eclipse.org/legal/epl-2.0/
 *
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 * QNX Software Systems - Initial API and implementation
 * ARM Limited - https://bugs.eclipse.org/bugs/show_bug.cgi?id=186981
 *******************************************************************************/
package org.eclipse.cdt.debug.internal.ui;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Iterator;

import org.eclipse.cdt.debug.core.model.ICBreakpoint;
import org.eclipse.cdt.debug.core.model.ICDebugElementStatus;
import org.eclipse.cdt.debug.core.model.ICStackFrame;
import org.eclipse.cdt.debug.core.model.ICType;
import org.eclipse.cdt.debug.core.model.ICValue;
import org.eclipse.cdt.debug.core.model.IEnableDisableTarget;
import org.eclipse.cdt.debug.ui.CDebugUIPlugin;
import org.eclipse.cdt.debug.ui.breakpoints.CBreakpointPropertyDialogAction;
import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.filesystem.URIUtil;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.content.IContentType;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.model.IBreakpoint;
import org.eclipse.debug.core.model.IValue;
import org.eclipse.debug.core.model.IVariable;
import org.eclipse.debug.ui.DebugUITools;
import org.eclipse.debug.ui.contexts.IDebugContextListener;
import org.eclipse.debug.ui.contexts.IDebugContextProvider;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.Position;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.source.Annotation;
import org.eclipse.jface.text.source.IAnnotationModel;
import org.eclipse.jface.text.source.IVerticalRulerInfo;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorRegistry;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IPathEditorInput;
import org.eclipse.ui.IStorageEditorInput;
import org.eclipse.ui.IURIEditorInput;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.ide.IUnassociatedEditorStrategy;
import org.eclipse.ui.internal.ide.IDEWorkbenchMessages;
import org.eclipse.ui.internal.ide.IDEWorkbenchPlugin;
import org.eclipse.ui.internal.ide.registry.SystemEditorOrTextEditorStrategy;
import org.eclipse.ui.internal.ide.registry.UnassociatedEditorStrategyRegistry;
import org.eclipse.ui.progress.UIJob;
import org.eclipse.ui.texteditor.ITextEditor;
import org.eclipse.ui.texteditor.SimpleMarkerAnnotation;

import com.ibm.icu.text.MessageFormat;

/**
 * Utility methods for C/C++ Debug UI.
 */
public class CDebugUIUtils {

	static public IRegion findWord( IDocument document, int offset ) {
		int start = -1;
		int end = -1;
		try {
			int pos = offset;
			char c;
			while( pos >= 0 ) {
				c = document.getChar( pos );
				if ( !Character.isJavaIdentifierPart( c ) )
					break;
				--pos;
			}
			start = pos;
			pos = offset;
			int length = document.getLength();
			while( pos < length ) {
				c = document.getChar( pos );
				if ( !Character.isJavaIdentifierPart( c ) )
					break;
				++pos;
			}
			end = pos;
		}
		catch( BadLocationException x ) {
		}
		if ( start > -1 && end > -1 ) {
			if ( start == offset && end == offset )
				return new Region( offset, 0 );
			else if ( start == offset )
				return new Region( start, end - start );
			else
				return new Region( start + 1, end - start - 1 );
		}
		return null;
	}

	/**
	 * Returns the currently selected stack frame or the topmost frame 
	 * in the currently selected thread in the Debug view 
	 * of the current workbench page. Returns <code>null</code> 
	 * if no stack frame or thread is selected, or if not called from the UI thread.
	 *  
	 * @return the currently selected stack frame or the topmost frame 
	 * 		   in the currently selected thread
	 */
	static public ICStackFrame getCurrentStackFrame() {
		IAdaptable context = DebugUITools.getDebugContext();
		return ( context != null ) ? (ICStackFrame)context.getAdapter( ICStackFrame.class ) : null;
	}

	/**
	 * Moved from CDebugModelPresentation because it is also used by CVariableLabelProvider.
	 */
	static public String getValueText( IValue value ) {
		StringBuilder label = new StringBuilder();
		if ( value instanceof ICDebugElementStatus && !((ICDebugElementStatus)value).isOK() ) {
			label.append(  MessageFormat.format( CDebugUIMessages.getString( "CDTDebugModelPresentation.4" ), (Object[]) new String[] { ((ICDebugElementStatus)value).getMessage() } ) ); //$NON-NLS-1$
		}
		else if ( value instanceof ICValue ) {
			ICType type = null;
			try {
				type = ((ICValue)value).getType();
			}
			catch( DebugException e ) {
			}
			try {
				String valueString = value.getValueString();
				if ( valueString != null ) {
					valueString = valueString.trim();
					if ( type != null && type.isCharacter() ) {
						if ( valueString.length() == 0 )
							valueString = "."; //$NON-NLS-1$
						label.append( valueString );
					}
					else if ( valueString.length() > 0 ) {
							label.append( valueString );
					}
				}
			}
			catch( DebugException e1 ) {
			}
		}	
		return label.toString();
	}

	/**
	 * Moved from CDebugModelPresentation because it is also used by CVariableLabelProvider.
	 */
	public static String getVariableTypeName( ICType type ) {
		StringBuilder result = new StringBuilder();
		if ( type != null ) {
			String typeName = type.getName();
			if ( typeName != null )
				typeName = typeName.trim();
			if ( type.isArray() && typeName != null ) {
				int index = typeName.indexOf( '[' );
				if ( index != -1 )
					typeName = typeName.substring( 0, index ).trim();
			}
			if ( typeName != null && typeName.length() > 0 ) {
				result.append( typeName );
				if ( type.isArray() ) {
					int[] dims = type.getArrayDimensions();
					for( int i = 0; i < dims.length; ++i ) {
						result.append( '[' );
						result.append( dims[i] );
						result.append( ']' );
					}
				}
			}
		}
		return result.toString();
	}

	public static String getVariableName( IVariable variable ) throws DebugException {
		return decorateText( variable, variable.getName() );
	}

	public static String getEditorFilePath( IEditorInput input ) throws CoreException {
		if ( input instanceof IFileEditorInput ) {
			IPath location = ((IFileEditorInput)input).getFile().getLocation();
			if (location != null) {
				return location.toOSString();
			}
			URI locationURI = ((IFileEditorInput)input).getFile().getLocationURI();
			if (locationURI != null) {
				IPath uriPath = URIUtil.toPath(locationURI);
				if (uriPath != null) {
					return uriPath.toOSString();
				}
			}
			return ""; //$NON-NLS-1$
		}
		if ( input instanceof IStorageEditorInput ) {
			return ((IStorageEditorInput)input).getStorage().getFullPath().toOSString();
		}
		if ( input instanceof IPathEditorInput ) {
			return ((IPathEditorInput)input).getPath().toOSString();
		}
		if ( input instanceof IURIEditorInput)
		{
			IPath uriPath = URIUtil.toPath(((IURIEditorInput)input).getURI());
			if (uriPath != null)
				return uriPath.toOSString();
		}
		return ""; //$NON-NLS-1$
	}

	public static String decorateText( Object element, String text ) {
		if ( text == null )
			return null;
		StringBuilder baseText = new StringBuilder( text );
		if ( element instanceof ICDebugElementStatus && !((ICDebugElementStatus)element).isOK() ) {
			baseText.append( MessageFormat.format( " <{0}>", new Object[] { ((ICDebugElementStatus)element).getMessage() } ) ); //$NON-NLS-1$
		}
		if ( element instanceof IAdaptable ) {
			IEnableDisableTarget target = ((IAdaptable)element).getAdapter( IEnableDisableTarget.class );
			if ( target != null ) {
				if ( !target.isEnabled() ) {
					baseText.append( ' ' );
					baseText.append( CDebugUIMessages.getString( "CDTDebugModelPresentation.25" ) ); //$NON-NLS-1$
				}
			}
		}
		return baseText.toString();
	}

	/**
	 * Helper function to open an error dialog.
	 * @param title
	 * @param message
	 * @param e
	 */
	static public void openError (final String title, final String message, final Exception e)
	{
		UIJob uiJob = new UIJob("open error"){ //$NON-NLS-1$

			@Override
			public IStatus runInUIThread(IProgressMonitor monitor) {
				// open error for the exception
				String detail = ""; //$NON-NLS-1$
				if (e != null)
					detail = e.getMessage();

				Shell shell = CDebugUIPlugin.getDefault().getWorkbench().getActiveWorkbenchWindow().getShell();

				MessageDialog.openError(
						shell,
						title,
						message + "\n" + detail); //$NON-NLS-1$
				return Status.OK_STATUS;
			}};
			uiJob.setSystem(true);
			uiJob.schedule();
	}
	
    /**
     * Resolves the {@link IBreakpoint} from the given editor and ruler information. Returns <code>null</code>
     * if no breakpoint exists or the operation fails.
     * 
     * @param editor the editor
     * @param info the current ruler information
     * @return the {@link IBreakpoint} from the current editor position or <code>null</code>
     */
    public static IBreakpoint getBreakpointFromEditor(ITextEditor editor, IVerticalRulerInfo info) {
        IAnnotationModel annotationModel = editor.getDocumentProvider().getAnnotationModel(editor.getEditorInput());
        IDocument document = editor.getDocumentProvider().getDocument(editor.getEditorInput());
        if (annotationModel != null) {
            Iterator<Annotation> iterator = annotationModel.getAnnotationIterator();
            while (iterator.hasNext()) {
                Object object = iterator.next();
                if (object instanceof SimpleMarkerAnnotation) {
                    SimpleMarkerAnnotation markerAnnotation = (SimpleMarkerAnnotation) object;
                    IMarker marker = markerAnnotation.getMarker();
                    try {
                        if (marker.isSubtypeOf(IBreakpoint.BREAKPOINT_MARKER)) {
                            Position position = annotationModel.getPosition(markerAnnotation);
                            int line = document.getLineOfOffset(position.getOffset());
                            if (line == info.getLineOfLastMouseButtonActivity()) {
                                IBreakpoint breakpoint = DebugPlugin.getDefault().getBreakpointManager().getBreakpoint(marker);
                                if (breakpoint != null) {
                                    return breakpoint;
                                }
                            }
                        }
                    } catch (CoreException e) {
                    } catch (BadLocationException e) {
                    }
                }
            }
        }
        return null;
    }

    public static void editBreakpointProperties(IWorkbenchPart part, final ICBreakpoint bp) {
        final ISelection debugContext = DebugUITools.getDebugContextForPart(part);
        CBreakpointPropertyDialogAction propertiesAction = new CBreakpointPropertyDialogAction(
            part.getSite(), 
            new ISelectionProvider() {
                @Override
                public ISelection getSelection() {
                    return new StructuredSelection( bp );
                }
                @Override public void addSelectionChangedListener( ISelectionChangedListener listener ) {}
                @Override public void removeSelectionChangedListener( ISelectionChangedListener listener ) {}
                @Override public void setSelection( ISelection selection ) {}
            }, 
            new IDebugContextProvider() {
                @Override
                public ISelection getActiveContext() {
                    return debugContext;
                }
                @Override public void addDebugContextListener(IDebugContextListener listener) {}
                @Override public void removeDebugContextListener(IDebugContextListener listener) {}
                @Override public IWorkbenchPart getPart() { return null; }
                
            }
            );
        propertiesAction.run();
        propertiesAction.dispose();
    }
    
    /**
     * Formats the given key stroke or click name and the modifier keys 
     * to a key binding string that can be used in action texts. 
     * 
     * @param modifierKeys the modifier keys
     * @param keyOrClick a key stroke or click, e.g. "Double Click"
     * @return the formatted keyboard shortcut string, e.g. "Shift+Double Click"
     * 
     * @since 8.1
     */
    public static final String formatKeyBindingString(int modifierKeys, String keyOrClick) {
        // this should actually all be delegated to KeyStroke class
        return KeyStroke.getInstance(modifierKeys, KeyStroke.NO_KEY).format() + keyOrClick; 
    }

	/**
	 * Returns an editor id appropriate for opening the given file
	 * store.
	 * <p>
	 * The editor descriptor is determined using a multi-step process. This
	 * method will attempt to resolve the editor based on content-type bindings
	 * as well as traditional name/extension bindings.
	 * </p>
	 * <ol>
	 * <li>The workbench editor registry is consulted to determine if an editor
	 * extension has been registered for the file type. If so, an instance of
	 * the editor extension is opened on the file. See
	 * <code>IEditorRegistry.getDefaultEditor(String)</code>.</li>
	 * <li>The operating system is consulted to determine if an in-place
	 * component editor is available (e.g. OLE editor on Win32 platforms).</li>
	 * <li>The operating system is consulted to determine if an external editor
	 * is available.</li>
	 * <li>The workbench editor registry is consulted to determine if the
	 * default text editor is available.</li>
	 * </ol>
	 * </p>
	 * 
	 * @param fileStore 
	 *            the file store
	 * @return the id of an editor, appropriate for opening the file
	 * @throws PartInitException
	 *             if no editor can be found
	 * @todo The IDE class has this method as a private, copied here so that it can be
	 * exposed. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=516470
	 * @deprecated Deprecated on creation as this is waiting for Bug 516470 to be resolved
	 */
	@Deprecated
	public static String getEditorId(IFileStore fileStore, boolean allowInteractive) throws PartInitException {
		String name = fileStore.fetchInfo().getName();
		if (name == null) {
			throw new IllegalArgumentException();
		}

		IContentType contentType = null;
		try {
			InputStream is = null;
			try {
				is = fileStore.openInputStream(EFS.NONE, null);
				contentType = Platform.getContentTypeManager().findContentTypeFor(is, name);
			} finally {
				if (is != null) {
					is.close();
				}
			}
		} catch (CoreException ex) {
			// continue without content type
		} catch (IOException ex) {
			// continue without content type
		}

		IEditorRegistry editorReg = PlatformUI.getWorkbench().getEditorRegistry();

		IEditorDescriptor defaultEditor = editorReg.getDefaultEditor(name, contentType);
		defaultEditor = IDE.overrideDefaultEditorAssociation(new FileStoreEditorInput(fileStore), contentType, defaultEditor);
		return getEditorDescriptor(name, editorReg, defaultEditor, allowInteractive).getId();
	}

	/**
	 * Get the editor descriptor for a given name using the editorDescriptor
	 * passed in as a default as a starting point.
	 * 
	 * @param name
	 *            The name of the element to open.
	 * @param editorReg
	 *            The editor registry to do the lookups from.
	 * @param defaultDescriptor
	 *            IEditorDescriptor or <code>null</code>
	 * @return IEditorDescriptor
	 * @throws PartInitException
	 *             if no valid editor can be found
	 * 
	 * @todo The IDE class has this method as a private, copied here so that it can be
	 * exposed via getEditorId. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=516470
	 * @deprecated Deprecated on creation as this is waiting for Bug 516470 to be resolved
	 */
	@Deprecated
	private static IEditorDescriptor getEditorDescriptor(String name,
			IEditorRegistry editorReg, IEditorDescriptor defaultDescriptor, boolean allowInteractive)
			throws PartInitException {

		if (defaultDescriptor != null) {
			return defaultDescriptor;
		}

		IUnassociatedEditorStrategy strategy = getUnassociatedEditorStrategy(allowInteractive);
		IEditorDescriptor editorDesc;
		try {
			editorDesc = strategy.getEditorDescriptor(name, editorReg);
		} catch (CoreException e) {
			throw new PartInitException(IDEWorkbenchMessages.IDE_noFileEditorFound, e);
		}

		// if no valid editor found, bail out
		if (editorDesc == null) {
			throw new PartInitException(
					IDEWorkbenchMessages.IDE_noFileEditorFound);
		}

		return editorDesc;
	}

	/**
	 * @param allowInteractive
	 *            Whether interactive strategies are considered
	 * @return The strategy to use in order to open unknown file. Either as set
	 *         by preference, or a {@link SystemEditorOrTextEditorStrategy} if
	 *         none is explicitly configured. Never returns {@code null}.
	 * 
	 * @todo The IDE class has this method as a private, copied here so that it can be
	 * exposed via getEditorId. See https://bugs.eclipse.org/bugs/show_bug.cgi?id=516470
	 * @deprecated Deprecated on creation as this is waiting for Bug 516470 to be resolved
	 */
	private static IUnassociatedEditorStrategy getUnassociatedEditorStrategy(boolean allowInteractive) {
		String preferedStrategy = IDEWorkbenchPlugin.getDefault().getPreferenceStore()
				.getString(IDE.UNASSOCIATED_EDITOR_STRATEGY_PREFERENCE_KEY);
		IUnassociatedEditorStrategy res = null;
		UnassociatedEditorStrategyRegistry registry = IDEWorkbenchPlugin.getDefault()
				.getUnassociatedEditorStrategyRegistry();
		if (allowInteractive || !registry.isInteractive(preferedStrategy)) {
			res = registry.getStrategy(preferedStrategy);
		}
		if (res == null) {
			res = new SystemEditorOrTextEditorStrategy();
		}
		return res;
	}

    
}
