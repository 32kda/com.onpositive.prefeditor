package com.onpositive.prefeditor.views;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.AbstractTreeViewer;
import org.eclipse.jface.viewers.ColumnViewerEditor;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationEvent;
import org.eclipse.jface.viewers.ColumnViewerEditorActivationStrategy;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.viewers.TreeViewerEditor;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.PlatformUI;

import com.onpositive.prefeditor.dialogs.NewPreferenceDialog;
import com.onpositive.prefeditor.model.IPreferenceProvider;
import com.onpositive.prefeditor.model.KeyValue;
import com.onpositive.prefeditor.views.PrefsLabelProvider.Column;

public abstract class ViewerPage extends Composite {

	protected TreeViewer viewer;
	protected PrefsContentProvider contentProvider;
	private PrefsLabelProvider keyLabelProvider;
	private PreferenceView preferenceView;

	public ViewerPage(Composite parent, PreferenceView preferenceView) {
		super(parent, SWT.NONE);
		this.preferenceView = preferenceView;
		setLayout(new FillLayout());
		
		createViewer();
        
        viewer.getTree().addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent event) {
				preferenceView.viewerFocusLost(event);
			}
			
			@Override
			public void focusGained(FocusEvent event) {
				preferenceView.viewerFocusGained(event);
			}
		});
        
        viewer.addDoubleClickListener(new IDoubleClickListener() {
			
			@Override
			public void doubleClick(final DoubleClickEvent event) {
				final IStructuredSelection selection = (IStructuredSelection) event
						.getSelection();
				if (selection == null || selection.isEmpty())
					return;

				final Object sel = selection.getFirstElement();

				final ITreeContentProvider provider = (ITreeContentProvider) viewer
						.getContentProvider();

				if (!provider.hasChildren(sel))
					return;

				if (viewer.getExpandedState(sel))
					viewer.collapseToLevel(sel,
							AbstractTreeViewer.ALL_LEVELS);
				else
					viewer.expandToLevel(sel, 1);
			}
		});
        
        ColumnViewerEditorActivationStrategy strategy = new ColumnViewerEditorActivationStrategy(viewer) {
    		@Override
    		protected boolean isEditorActivationEvent(ColumnViewerEditorActivationEvent event) {
    			return event.eventType == ColumnViewerEditorActivationEvent.MOUSE_DOUBLE_CLICK_SELECTION &&
    				   event.getSource() instanceof ViewerCell &&
    				   ((ViewerCell) event.getSource()).getColumnIndex() == 1;
    				   
    		}
    	};
    	TreeViewerEditor.create(viewer, null, strategy, ColumnViewerEditor.DEFAULT);

        TreeViewerColumn viewerColumn = new TreeViewerColumn(viewer, SWT.NONE);
        viewerColumn.getColumn().setWidth(300);
        viewerColumn.getColumn().setText("Key");
        keyLabelProvider = new PrefsLabelProvider(Column.KEY);
		viewerColumn.setLabelProvider(keyLabelProvider);
        
        viewerColumn = new TreeViewerColumn(viewer, SWT.NONE);
        viewerColumn.getColumn().setWidth(300);
        viewerColumn.getColumn().setText("Value");
        viewerColumn.setLabelProvider(new PrefsLabelProvider(Column.VALUE));
        viewerColumn.setEditingSupport(new PrefValueEditingSupport(viewer));
        
        initializeInput();

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(
					SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				preferenceView.updateActions(selection);
			}
		});
		
		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "com.onpositive.prefeditor.viewer");
		hookContextMenu();
	}

	protected abstract void initializeInput();

	protected void createViewer() {
		viewer = new TreeViewer(this, SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        contentProvider = new PrefsContentProvider();
		viewer.setContentProvider(contentProvider);
        viewer.getTree().setHeaderVisible(true);
        viewer.getTree().setLinesVisible(true);
	}
	
	public void removeSelected() {
		Object input = viewer.getInput();
		if (input instanceof IPreferenceProvider) {
			ISelection selection = viewer.getSelection();
			Object element = ((StructuredSelection) selection).getFirstElement();
			if (element instanceof KeyValue) {
				((IPreferenceProvider) input).remove(((KeyValue) element));
			} else if (element instanceof String) {
				if (MessageDialog.openQuestion(preferenceView.getSite().getShell(), "Remove preference file", "Remove whole preference file " + String.valueOf(element) + ". Are you sure?")) {
					((IPreferenceProvider) input).removeCategory(String.valueOf(element));	
				}
			}
			viewer.refresh();
		}
	}
	
	public void setHierarchical(boolean hierarchical) {
		contentProvider.setTreeMode(hierarchical);
		keyLabelProvider.setTreeMode(hierarchical);
		viewer.refresh();
	}
	
	public void addPreference() {
		String parent = null;
		ISelection selection = viewer.getSelection();
		if (!selection.isEmpty()) {
			Object element = ((StructuredSelection) selection).getFirstElement();
			if (element instanceof KeyValue) {
				parent = ((KeyValue) element).getParentNode();
			} else {
				parent = String.valueOf(element);
			}
		}
		Object input = viewer.getInput();
		if (input instanceof IPreferenceProvider) {
			NewPreferenceDialog dialog = new NewPreferenceDialog(preferenceView.getSite().getShell(), parent, ((IPreferenceProvider) input).getNodeNames());
			if (dialog.open() == Dialog.OK) {
				parent = dialog.getParent();
				String name = dialog.getName();
				String value = dialog.getValue();
				KeyValue newElement = new KeyValue(parent, name, value);
				((IPreferenceProvider) input).add(newElement);
				viewer.refresh();
			}
		}
	}
	
	public void copyKey() {
		String text = null;
		ISelection selection = viewer.getSelection();
		if (!selection.isEmpty()) {
			Object element = ((StructuredSelection) selection).getFirstElement();
			if (element instanceof KeyValue) {
				text = ((KeyValue) element).getKey();
			} else {
				text = String.valueOf(element);
			}
			textToClipboard(text);
		}		
	}
	
	public void copyValue() {
		String text = null;
		ISelection selection = viewer.getSelection();
		if (!selection.isEmpty()) {
			Object element = ((StructuredSelection) selection).getFirstElement();
			if (element instanceof KeyValue) {
				text = ((KeyValue) element).getValue();
				textToClipboard(text);
			} 
		}
	}
	
	protected void textToClipboard(String text) {
		Clipboard clipboard = new Clipboard(Display.getDefault());
		TextTransfer textTransfer = TextTransfer.getInstance();
		clipboard.setContents(new String[]{text}, new Transfer[]{textTransfer});
		clipboard.dispose();
	}

	public void setTreeMode(boolean treeMode) {
		contentProvider.setTreeMode(treeMode);
		keyLabelProvider.setTreeMode(treeMode);
		viewer.refresh();
	}

	public abstract void reloadPrefs();

	public void setViewerFocus() {
		viewer.getControl().setFocus();
	}

	public ISelection getSelection() {
		return viewer.getSelection();
	}
	
	protected void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				preferenceView.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		preferenceView.getSite().registerContextMenu(menuMgr, viewer);
	}
	
	@Override
	public Point computeSize(int wHint, int hHint, boolean changed) {
		// TODO Auto-generated method stub
		return super.computeSize(wHint, hHint, changed);
	}

}
