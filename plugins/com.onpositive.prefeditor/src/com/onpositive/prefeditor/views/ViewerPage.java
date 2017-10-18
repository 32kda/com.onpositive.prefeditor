package com.onpositive.prefeditor.views;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Properties;

import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
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
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.onpositive.prefeditor.PrefEditorPlugin;
import com.onpositive.prefeditor.dialogs.NewPlatformPreferenceDialog;
import com.onpositive.prefeditor.dialogs.NewPreferenceDialog;
import com.onpositive.prefeditor.model.IPreferenceProvider;
import com.onpositive.prefeditor.model.KeyValue;
import com.onpositive.prefeditor.model.PlatformPreferenceProvider;
import com.onpositive.prefeditor.ui.iternal.SetFilterJob;
import com.onpositive.prefeditor.views.PrefsLabelProvider.Column;

public abstract class ViewerPage extends Composite {

	protected TreeViewer viewer;
	protected PrefsContentProvider contentProvider;
	private PrefsLabelProvider keyLabelProvider;
	private PreferenceView preferenceView;
	private PreferenceFilter viewerFilter;
	
	private SetFilterJob filterJob;

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
    				   ((ViewerCell) event.getSource()).getColumnIndex() == 1 && isSelectionEditable();
    				   
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

	public boolean isSelectionEditable() {
		return true;
	}

	protected abstract void initializeInput();

	protected void createViewer() {
		FormLayout formLayout = new FormLayout();
		setLayout(formLayout);
		Composite con = new Composite(this, SWT.NONE);
		FormData topData = new FormData();
		topData.left = new FormAttachment(0,0);
		topData.right = new FormAttachment(100,0);
		topData.top = new FormAttachment(0,0);
		con.setLayoutData(topData);
		
		con.setLayout(new GridLayout(3,false));
		createTopArea(con);
		
		viewer = new TreeViewer(this, SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        contentProvider = new PrefsContentProvider();
		viewer.setContentProvider(contentProvider);
        viewer.getTree().setHeaderVisible(true);
        viewer.getTree().setLinesVisible(true);
        FormData viewerData = new FormData();
        viewerData.top = new FormAttachment(con, 5);
        viewerData.bottom = new FormAttachment(100,0);
        viewerData.left = new FormAttachment(0,0);
        viewerData.right = new FormAttachment(100,0);
        viewer.getTree().setLayoutData(viewerData);
        viewerFilter = new PreferenceFilter();
		viewer.addFilter(viewerFilter);
		filterJob = new SetFilterJob(viewer, viewerFilter);
	}
	
	protected abstract void createTopArea(Composite con);

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
			refreshTree();
		}
	}

	protected void refreshTree() {
		viewer.refresh();
	}
	
	public void addPreference() {
		String parent = "";
		ISelection selection = viewer.getSelection();
		Object element = null;
		if (!selection.isEmpty()) {
			element = ((StructuredSelection) selection).getFirstElement();
		}
		if (element == null) {
			Object[] elements = contentProvider.getElements("");
			if (elements.length > 0) {
				element = elements[0];
			}
		}
		
		if (element != null) {		
			if (element instanceof KeyValue) {
				parent = ((KeyValue) element).getParentNode();
			} else {
				parent = String.valueOf(element);
			}
		}
		Object input = viewer.getInput();
		if (input instanceof IPreferenceProvider) {
			NewPreferenceDialog dialog =
				input instanceof PlatformPreferenceProvider?
					new NewPlatformPreferenceDialog(preferenceView.getSite().getShell(), parent, ((IPreferenceProvider) input).getNodeNames())
					:
					new NewPreferenceDialog(preferenceView.getSite().getShell(), parent, ((IPreferenceProvider) input).getNodeNames());
			if (dialog.open() == Dialog.OK) {
				parent = dialog.getParent();
				String name = dialog.getName();
				String value = dialog.getValue();
				KeyValue newElement = new KeyValue(parent, name, value);
				((IPreferenceProvider) input).add(newElement);
				refreshTree();
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
	
	public void collapseAll() {
		viewer.collapseAll();
	}

	protected void createFilterControls(Composite con) {
		Label filterLabel = new Label(con,SWT.NONE);
		filterLabel.setText("Filter:");
		GridDataFactory.swtDefaults().applyTo(filterLabel);
		Text filterText = new Text(con, SWT.BORDER);
		filterText.setMessage("(" + PreferenceFilter.MIN_FILTER_CHARS + " chars at least)");
		filterText.addModifyListener(event -> {
			filterChanged(filterText.getText());
		});
		GridDataFactory.fillDefaults().grab(true,false).applyTo(filterText);
		Button clearBtn = new Button(con, SWT.PUSH);
		clearBtn.setImage(AbstractUIPlugin.imageDescriptorFromPlugin(PrefEditorPlugin.PLUGIN_ID,"icons/clear.gif").createImage());
		GridDataFactory.swtDefaults().applyTo(clearBtn);
		clearBtn.addSelectionListener(new SelectionAdapter() {
			
			@Override
			public void widgetSelected(SelectionEvent e) {
				filterText.setText("");
				filterChanged("");
			}
			
		});
	}

	protected void filterChanged(String filterText) {
		filterJob.setFilterText(filterText);
	}

	public void setTracking(boolean tracking) {
		Object input = viewer.getInput();
		if (input instanceof IPreferenceProvider) {
			((IPreferenceProvider) input).setTracking(tracking);
		}
	}
	
	public boolean isTracking() {
		Object input = viewer.getInput();
		if (input instanceof IPreferenceProvider) {
			return ((IPreferenceProvider) input).isTracking();
		}
		return false;
	}

	public void exportValues(String path) {
		ISelection selection = getSelection();
		if  (!selection.isEmpty() && !(((StructuredSelection) selection).getFirstElement() instanceof KeyValue)) {
			String nodeId = ((StructuredSelection) selection).getFirstElement().toString();
			Object[] children = contentProvider.getChildren(nodeId);
			Properties properties = new Properties();
			for (Object object : children) {
				if (object instanceof KeyValue) {
					properties.put(((KeyValue) object).getKey(), ((KeyValue) object).getValue());
				}
			}
			try (BufferedOutputStream os = new BufferedOutputStream(new FileOutputStream(path))) {
				properties.store(os, "");
			} catch (IOException e) {
				PrefEditorPlugin.log(e);
			}
		}
	}

}
