package com.onpositive.prefeditor.views;


import java.io.File;

import org.apache.commons.io.FileUtils;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.*;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.viewers.*;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.jface.action.*;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.ui.*;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.SWT;
import org.osgi.service.prefs.BackingStoreException;

import com.onpositive.prefeditor.PrefEditorPlugin;
import com.onpositive.prefeditor.dialogs.FolderSelectionDialog;
import com.onpositive.prefeditor.dialogs.NewPreferenceDialog;
import com.onpositive.prefeditor.model.KeyValue;
import com.onpositive.prefeditor.model.PreferenceProvider;
import com.onpositive.prefeditor.views.PrefsLabelProvider.Column;

import static com.onpositive.prefeditor.PrefConstants.*;


/**
 * This sample class demonstrates how to plug-in a new
 * workbench view. The view shows data obtained from the
 * model. The sample creates a dummy model on the fly,
 * but a real implementation would connect to the model
 * available either in this or another plug-in (e.g. the workspace).
 * The view is connected to the model using a content provider.
 * <p>
 * The view uses a label provider to define how model
 * objects should be presented in the view. Each
 * view can present the same model objects using
 * different labels and icons, if needed. Alternatively,
 * a single label provider can be shared between views
 * in order to ensure that objects of the same type are
 * presented in the same way everywhere.
 * <p>
 */

public class PreferenceView extends ViewPart {

	private static final String CHOOSED_FOLDER_PREF = "choosedFolder";
	
	private static final int MAX_TITLE_LENGTH = 40;

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "com.onpositive.prefeditor.views.PreferenceView";

	private TreeViewer viewer;
	private Action chooseFolderAction;
	private Action viewModeAction;
	private Action removeAction;
	private Action addAction;
	
	private Action copyAction;
	
	private Action reloadAction;
	
	private Action copyValueAction;

	private String folderPath;

	private Label titleLabel;

	private PrefsContentProvider contentProvider;

	private PrefsLabelProvider keyLabelProvider;

	private IHandlerActivation copyHandlerActivation;

	class NameSorter extends ViewerSorter {
	}

	/**
	 * The constructor.
	 */
	public PreferenceView() {
	}

	/**
	 * This is a callback that will allow us
	 * to create the viewer and initialize it.
	 */
	public void createPartControl(Composite parent) {
		parent.setLayout(new GridLayout());
		titleLabel = new Label(parent, SWT.NONE);
		GridDataFactory.swtDefaults().applyTo(titleLabel);
		
		viewer = new TreeViewer(parent, SWT.FULL_SELECTION);
        contentProvider = new PrefsContentProvider();
		viewer.setContentProvider(contentProvider);
        viewer.getTree().setHeaderVisible(true);
        viewer.getTree().setLinesVisible(true);
        GridDataFactory.fillDefaults().grab(true, true).applyTo(viewer.getControl());
        
        viewer.getTree().addFocusListener(new FocusListener() {
			
			@Override
			public void focusLost(FocusEvent arg0) {
				final IHandlerService handlerService = getHandlerService();
				if (copyHandlerActivation != null) {
					handlerService.deactivateHandler(copyHandlerActivation);
				}
			}
			
			@Override
			public void focusGained(FocusEvent arg0) {
		        final IHandlerService handlerService = getHandlerService();
		        copyHandlerActivation = handlerService
		        		.activateHandler(IWorkbenchCommandConstants.EDIT_COPY,
		        		new ActionHandler(copyAction));
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
        
        folderPath = getInitialFolder();
		viewer.setInput(new PreferenceProvider(folderPath));
		setViewerTitle(folderPath);

        viewer.addSelectionChangedListener(new ISelectionChangedListener() {
			
			@Override
			public void selectionChanged(
					SelectionChangedEvent event) {
				ISelection selection = event.getSelection();
				removeAction.setEnabled(!selection.isEmpty());
				copyAction.setEnabled(!selection.isEmpty());
				copyValueAction.setEnabled(!selection.isEmpty() && ((StructuredSelection) selection).getFirstElement() instanceof KeyValue);
			}
		});
		
		// Create the help context id for the viewer's control
		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "com.onpositive.prefeditor.viewer");
		makeActions();
		hookContextMenu();
//		hookDoubleClickAction();
		contributeToActionBars();
	}

	private void setViewerTitle(String folderPath) {
		titleLabel.setToolTipText("");
		if (folderPath == null || folderPath.isEmpty()) {
			titleLabel.setText("Select folder to view preferences");
			return;
		} 
		folderPath = folderPath.trim().replace('\\','/');
		if (folderPath.endsWith("/")) {
			folderPath = folderPath.substring(0, folderPath.length() - 1);
		}
		if (folderPath.endsWith(WORKSPACE_SETTINGS_PATH)) {
			String path = folderPath.substring(0, folderPath.length() - WORKSPACE_SETTINGS_PATH.length());
			String name = new Path(path).lastSegment();
			titleLabel.setText("Workspace: " + name);
		} else if (folderPath.endsWith(CONFIGURATION_SETTINGS_PATH)) {
			String path = folderPath.substring(0, folderPath.length() - CONFIGURATION_SETTINGS_PATH.length());
			String name = new Path(path).lastSegment();
			titleLabel.setText("Installation: " + name);
		} else {
			String title = folderPath;
			int len = title.length();
			if (len > MAX_TITLE_LENGTH) {
				titleLabel.setToolTipText(title);
				title = "..." + title.substring(len - MAX_TITLE_LENGTH, len);
			}
			titleLabel.setText(title);
		}
		
	}

	protected String getInitialFolder() {
		return ConfigurationScope.INSTANCE.getNode(PrefEditorPlugin.PLUGIN_ID).get(CHOOSED_FOLDER_PREF, getDefaultFolder());
	}

	protected String getDefaultFolder() {
		File folder = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString(), WORKSPACE_SETTINGS_PATH);
		if (folder.isDirectory()) {
			return folder.getAbsolutePath();
		}
		
		folder = FileUtils.toFile(Platform.getInstallLocation().getURL());
		if (folder.isDirectory()) {
			return new File(folder, CONFIGURATION_SETTINGS_PATH).getAbsolutePath();
		}
		return "";
	}
	
	protected void reloadPrefs() {
		TreePath[] paths = viewer.getExpandedTreePaths();
		viewer.setInput(new PreferenceProvider(folderPath));
		viewer.setExpandedTreePaths(paths);
	}
	
	protected void folderChoosed(String folderPath) {
		this.folderPath = folderPath;
		viewer.setInput(new PreferenceProvider(folderPath));
		IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(PrefEditorPlugin.PLUGIN_ID);
		node.put(CHOOSED_FOLDER_PREF, folderPath);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			PrefEditorPlugin.log(e);
		}
		setViewerTitle(folderPath);
	}

	private void hookContextMenu() {
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener() {
			public void menuAboutToShow(IMenuManager manager) {
				PreferenceView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(viewer.getControl());
		viewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, viewer);
	}

	private void contributeToActionBars() {
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager) {
		manager.add(chooseFolderAction);
		manager.add(new Separator());
		manager.add(viewModeAction);
		manager.add(addAction);
	}

	private void fillContextMenu(IMenuManager manager) {
		manager.add(addAction);
		Action[] enabledActions = new Action[] {removeAction, copyAction, copyValueAction};
		for (Action action : enabledActions) {
			if (action.isEnabled()) {
				manager.add(action);
			}
			
		}
		// Other plug-ins can contribute there actions here
//		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}
	
	private void fillLocalToolBar(IToolBarManager manager) {
		manager.add(reloadAction);
		manager.add(chooseFolderAction);
		manager.add(viewModeAction);
		manager.add(addAction);
		manager.add(removeAction);
		manager.add(copyAction);
	}

	private void makeActions() {
		chooseFolderAction = new Action() {
			public void run() {
				FolderSelectionDialog dialog = new FolderSelectionDialog(getViewSite().getShell(), folderPath);
				if (dialog.open() == Window.OK) {
					String folderPath = dialog.getValue();
					folderChoosed(folderPath);
				}
			}
		};
		chooseFolderAction.setText("Open preferences");
		chooseFolderAction.setToolTipText("Open preferences list from workspace, installation or particular folder");
		chooseFolderAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER));
		
		viewModeAction = new Action("Hierarchical view", SWT.TOGGLE) {
			public void run() {
				contentProvider.setTreeMode(viewModeAction.isChecked());
				keyLabelProvider.setTreeMode(viewModeAction.isChecked());
				viewer.refresh();
			}
		};
		viewModeAction.setChecked(true);
		viewModeAction.setText("View mode");
		viewModeAction.setToolTipText("Toggle hierarchical/flat view");
		viewModeAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(PrefEditorPlugin.PLUGIN_ID,"icons/hierarchicalLayout.gif"));
		
		removeAction = new Action() {
			@Override
			public void run() {
				Object input = viewer.getInput();
				if (input instanceof PreferenceProvider) {
					ISelection selection = viewer.getSelection();
					Object element = ((StructuredSelection) selection).getFirstElement();
					if (element instanceof KeyValue) {
						((PreferenceProvider) input).remove(((KeyValue) element));
					} else if (element instanceof String) {
						if (MessageDialog.openQuestion(getSite().getShell(), "Remove preference file", "Remove whole preference file " + String.valueOf(element) + ". Are you sure?")) {
							((PreferenceProvider) input).removeCategory(String.valueOf(element));	
						}
					}
					viewer.refresh();
				}
			}
			
		};
		removeAction.setText("Remove");
		removeAction.setToolTipText("Remove preference");
		removeAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(PrefEditorPlugin.PLUGIN_ID,"icons/remove.gif"));
		
		addAction = new Action() {
			@Override
			public void run() {
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
				if (input instanceof PreferenceProvider) {
					NewPreferenceDialog dialog = new NewPreferenceDialog(getSite().getShell(), parent, ((PreferenceProvider) input).getFileNames());
					if (dialog.open() == Dialog.OK) {
						parent = dialog.getParent();
						String name = dialog.getName();
						String value = dialog.getValue();
						KeyValue newElement = new KeyValue(parent, name, value);
						((PreferenceProvider) input).add(newElement);
						viewer.refresh();
					}
				}
			}
			
		};
		addAction.setText("Add preference");
		addAction.setToolTipText("add preference");
		addAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(PrefEditorPlugin.PLUGIN_ID,"icons/add.png"));
		
		reloadAction = new Action() {
			
			@Override
			public void run() {
				reloadPrefs();
			}
			
		};
		reloadAction.setText("Reload prefs");
		reloadAction.setToolTipText("Reload preferences from current folder");
		reloadAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(PrefEditorPlugin.PLUGIN_ID,"icons/refresh.gif"));
		
		copyAction = new Action() {
			@Override
			public void run() {
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
			
		};
		
		copyAction.setText("Copy key/name");
		copyAction.setToolTipText("Copy key or node name");
		copyAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(PrefEditorPlugin.PLUGIN_ID,"icons/copy.png"));
		
		copyValueAction = new Action() {
			@Override
			public void run() {
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
			
		};
		
		copyValueAction.setText("Copy value");
		copyValueAction.setToolTipText("Copy preference value");
		copyValueAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(PrefEditorPlugin.PLUGIN_ID,"icons/copy.png"));
		
	}

//	private void showMessage(String message) {
//		MessageDialog.openInformation(
//			viewer.getControl().getShell(),
//			"Preferences",
//			message);
//	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		viewer.getControl().setFocus();
	}

	protected IHandlerService getHandlerService() {
		final IHandlerService handlerService = (IHandlerService) (getSite()).getService(IHandlerService.class);
		return handlerService;
	}

	protected void textToClipboard(String text) {
		Clipboard clipboard = new Clipboard(Display.getDefault());
		TextTransfer textTransfer = TextTransfer.getInstance();
		clipboard.setContents(new String[]{text}, new Transfer[]{textTransfer});
		clipboard.dispose();
	}
	

}