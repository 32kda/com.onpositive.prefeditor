package com.onpositive.prefeditor.views;


import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.ActionContributionItem;
import org.eclipse.jface.action.IMenuCreator;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.CTabItem;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchCommandConstants;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.service.prefs.BackingStoreException;

import com.onpositive.prefeditor.PrefEditorPlugin;
import com.onpositive.prefeditor.dialogs.FolderSelectionDialog;
import com.onpositive.prefeditor.model.KeyValue;
import com.onpositive.prefeditor.ui.iternal.PrefUIUtil;


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
	
	private class ChooseFolderAction extends Action {

		private String folderPath;

		public ChooseFolderAction(String folderPath) {
			this.folderPath = folderPath;
			setText(PrefUIUtil.getFolderLabel(folderPath, 100));
		}
		
		@Override
		public void run() {
			folderChoosed(folderPath);
		}

	}

	private static final String CHOOSED_PAGE_PREF = "choosedPage";
	
	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "com.onpositive.prefeditor.views.PreferenceView";

	private Action chooseFolderAction;
	private Action viewModeAction;
	private Action removeAction;
	private Action addAction;
	
	private Action copyAction;
	
	private Action reloadAction;
	
	private Action copyValueAction;

	private IHandlerActivation copyHandlerActivation;

	private CTabFolder tabFolder;
	
	private ViewerPage activePage;

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
		parent.setLayout(new FillLayout());
		
		this.tabFolder = new CTabFolder(parent, SWT.BOTTOM);
        createFSTab(tabFolder);
        createPlatformTab(tabFolder);
        tabFolder.addSelectionListener(new SelectionAdapter() {
        	
        	@Override
        	public void widgetSelected(SelectionEvent e) {
        		CTabItem selection = tabFolder.getSelection();
        		if (selection != null) {
        			setActiveViewerPage((ViewerPage) selection.getControl());
        		}
        		saveChoosedPage(tabFolder.getSelectionIndex());
        	}
        	
		});
        int choosedPage = loadChoosedPage();
        tabFolder.setSelection(choosedPage);
        activePage = (ViewerPage) tabFolder.getItem(choosedPage).getControl();
        
//        GridData gridData = new GridData(SWT.FILL,SWT.TOP,true,false);
//        tabFolder.setLayoutData(gridData);
//        GridDataFactory.fillDefaults().grab(true, false).applyTo(tabFolder);
		// Create the help context id for the viewer's control
//		PlatformUI.getWorkbench().getHelpSystem().setHelp(viewer.getControl(), "com.onpositive.prefeditor.viewer");
		makeActions();
//		hookDoubleClickAction();
		contributeToActionBars();
	}

	protected void setActiveViewerPage(ViewerPage viewerPage) {
		activePage = viewerPage;
		updateActions(activePage.getSelection());
	}
	
	protected ViewerPage getActiveViewerPage() {
		return activePage;
	}

	private void createPlatformTab(CTabFolder tabFolder) {
		CTabItem item = new CTabItem(tabFolder, SWT.NONE);
		item.setText("Platform");
		PlatformViewerPage page = new PlatformViewerPage(tabFolder, this);
		item.setControl(page);
	}

	private void createFSTab(CTabFolder tabFolder) {
		CTabItem item = new CTabItem(tabFolder, SWT.NONE);
		item.setText("Folder");
		FolderViewerPage page = new FolderViewerPage(tabFolder, this);
		item.setControl(page);
	}

	public void folderChoosed(String folderPath) {
		if (activePage instanceof FolderViewerPage) {
			((FolderViewerPage) activePage).folderChoosed(folderPath);
		}
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

	public void fillContextMenu(IMenuManager manager) {
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

	protected void saveChoosedPage(int selectionIndex) {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(PrefEditorPlugin.PLUGIN_ID);
		node.putInt(CHOOSED_PAGE_PREF, selectionIndex);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			PrefEditorPlugin.log(e);
		}
	}
	
	protected int loadChoosedPage() {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(PrefEditorPlugin.PLUGIN_ID);
		return node.getInt(CHOOSED_PAGE_PREF, 0);
	}


	private void makeActions() {
		chooseFolderAction = new Action("Choose pref folder", Action.AS_DROP_DOWN_MENU) {
			public void run() {
				if (activePage instanceof FolderViewerPage) {
					String folderPath = ((FolderViewerPage) activePage).getFolderPath();
					FolderSelectionDialog dialog = new FolderSelectionDialog(getViewSite().getShell(), folderPath);
					if (dialog.open() == Window.OK) {
						ViewerPage activeViewerPage = getActiveViewerPage();
						if (activeViewerPage instanceof FolderViewerPage) {
							((FolderViewerPage) activeViewerPage).folderChoosed(dialog.getValue());
						}
					}
				}
			}
		};
		chooseFolderAction.setText("Open preferences");
		chooseFolderAction.setToolTipText("Open preferences list from workspace, installation or particular folder");
		chooseFolderAction.setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().
			getImageDescriptor(ISharedImages.IMG_OBJ_FOLDER));
		
		chooseFolderAction.setMenuCreator(new IMenuCreator() {
			
	        private Menu menu;

			private List<ActionContributionItem> createDropDownMenuItems() {
	                return Arrays.asList(PrefEditorPlugin.getPrevFolderChoices()).stream()
	                	.map(folderPath -> new ActionContributionItem(new ChooseFolderAction(folderPath)))
	                	.collect(Collectors.toList()); 
	        }

	        /* (non-Javadoc)
	         * @see org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets.Control)
	         */
	        public Menu getMenu(Control parent) {
	        	if (menu != null) {
	        		menu.dispose();
	        	}
	        	menu = new Menu(parent);
	        	createDropDownMenuItems().stream().forEach(
	        			item -> item.fill(menu,-1));
	            return menu;
	        }

	        /* (non-Javadoc)
	         * @see org.eclipse.jface.action.IMenuCreator#getMenu(org.eclipse.swt.widgets.Menu)
	         */
	        public Menu getMenu(Menu parent) {
	        	if (menu != null) {
	        		menu.dispose();
	        	}
	        	menu = new Menu(parent);
	            createDropDownMenuItems().stream().forEach(
	        			item -> item.fill(menu,-1));
	            return menu;
	        }

	        /* (non-Javadoc)
	         * @see org.eclipse.jface.action.IMenuCreator#dispose()
	         */
	        public void dispose() {
	        	if (menu != null) {
	        		menu.dispose();
	        	}
	        }
		});
		
		viewModeAction = new Action("Hierarchical view", SWT.TOGGLE) {
			public void run() {
				getActiveViewerPage().setTreeMode(viewModeAction.isChecked());
			}
		};
		viewModeAction.setChecked(true);
		viewModeAction.setText("View mode");
		viewModeAction.setToolTipText("Toggle hierarchical/flat view");
		viewModeAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(PrefEditorPlugin.PLUGIN_ID,"icons/hierarchicalLayout.gif"));
		
		removeAction = new Action() {
			@Override
			public void run() {
				getActiveViewerPage().removeSelected();
			}
			
		};
		removeAction.setText("Remove");
		removeAction.setToolTipText("Remove preference");
		removeAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(PrefEditorPlugin.PLUGIN_ID,"icons/remove.gif"));
		
		addAction = new Action() {
			@Override
			public void run() {
				getActiveViewerPage().addPreference();
			}
			
		};
		addAction.setText("Add preference");
		addAction.setToolTipText("add preference");
		addAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(PrefEditorPlugin.PLUGIN_ID,"icons/add.png"));
		
		reloadAction = new Action() {
			
			@Override
			public void run() {
				getActiveViewerPage().reloadPrefs();
			}
			
		};
		reloadAction.setText("Reload prefs");
		reloadAction.setToolTipText("Reload preferences from current folder");
		reloadAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(PrefEditorPlugin.PLUGIN_ID,"icons/refresh.gif"));
		
		copyAction = new Action() {
			@Override
			public void run() {
				getActiveViewerPage().copyKey();
			}
		};
		
		copyAction.setText("Copy key/name");
		copyAction.setToolTipText("Copy key or node name");
		copyAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(PrefEditorPlugin.PLUGIN_ID,"icons/copy.png"));
		
		copyValueAction = new Action() {
			@Override
			public void run() {
				getActiveViewerPage().copyValue();
			}
		};
		
		copyValueAction.setText("Copy value");
		copyValueAction.setToolTipText("Copy preference value");
		copyValueAction.setImageDescriptor(AbstractUIPlugin.imageDescriptorFromPlugin(PrefEditorPlugin.PLUGIN_ID,"icons/copy.png"));
		
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	public void setFocus() {
		getActiveViewerPage().setViewerFocus();
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

	public void updateActions(ISelection selection) {
		chooseFolderAction.setEnabled(activePage instanceof FolderViewerPage);
		
		removeAction.setEnabled(!selection.isEmpty());
		copyAction.setEnabled(!selection.isEmpty());
		copyValueAction.setEnabled(!selection.isEmpty() && ((StructuredSelection) selection).getFirstElement() instanceof KeyValue);
	}

	public void viewerFocusLost(FocusEvent event) {
		final IHandlerService handlerService = getHandlerService();
		if (copyHandlerActivation != null) {
			handlerService.deactivateHandler(copyHandlerActivation);
		}
	}

	public void viewerFocusGained(FocusEvent event) {
		final IHandlerService handlerService = getHandlerService();
        copyHandlerActivation = handlerService
        		.activateHandler(IWorkbenchCommandConstants.EDIT_COPY,
        		new ActionHandler(copyAction));
	}
	

}