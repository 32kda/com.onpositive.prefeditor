package com.onpositive.prefeditor.views;

import static com.onpositive.prefeditor.PrefConstants.CONFIGURATION_SETTINGS_PATH;
import static com.onpositive.prefeditor.PrefConstants.WORKSPACE_SETTINGS_PATH;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.osgi.service.prefs.BackingStoreException;

import com.onpositive.prefeditor.PrefEditorPlugin;
import com.onpositive.prefeditor.model.FolderPreferenceProvider;
import com.onpositive.prefeditor.ui.iternal.PrefUIUtil;

public class FolderViewerPage extends ViewerPage {
	
	private static final String CHOOSED_FOLDER_PREF = "choosedFolder";
	
	private static final int MAX_TITLE_LENGTH = 40;

	private Label titleLabel;

	private String folderPath;

	public FolderViewerPage(Composite parent, PreferenceView preferenceView) {
		super(parent, preferenceView);
	}
	
	@Override
	protected void createViewer() {
		FormLayout formLayout = new FormLayout();
		setLayout(formLayout);
		titleLabel = new Label(this, SWT.NONE);
		FormData labelData = new FormData();
		labelData.left = new FormAttachment(0,0);
		labelData.top = new FormAttachment(0,0);
		titleLabel.setLayoutData(labelData);
		viewer = new TreeViewer(this, SWT.FULL_SELECTION | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL);
        contentProvider = new PrefsContentProvider();
		viewer.setContentProvider(contentProvider);
        viewer.getTree().setHeaderVisible(true);
        viewer.getTree().setLinesVisible(true);
        FormData viewerData = new FormData();
        viewerData.top = new FormAttachment(titleLabel, 5);
        viewerData.bottom = new FormAttachment(100,0);
        viewerData.left = new FormAttachment(0,0);
        viewerData.right = new FormAttachment(100,0);
        viewer.getTree().setLayoutData(viewerData);
	}
	
	private void setViewerTitle(String folderPath) {
		titleLabel.setToolTipText("");
		if (folderPath == null || folderPath.isEmpty()) {
			titleLabel.setText("Select folder to view preferences");
			return;
		} 
		titleLabel.setText(PrefUIUtil.getFolderLabel(folderPath, MAX_TITLE_LENGTH));
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

	@Override
	protected void initializeInput() {
		folderPath = getInitialFolder();
		viewer.setInput(new FolderPreferenceProvider(folderPath));
		setViewerTitle(folderPath);
	}
	
	protected String getInitialFolder() {
		return ConfigurationScope.INSTANCE.getNode(PrefEditorPlugin.PLUGIN_ID).get(CHOOSED_FOLDER_PREF, getDefaultFolder());
	}
	
	public void folderChoosed(String folderPath) {
		this.folderPath = folderPath;
		viewer.setInput(new FolderPreferenceProvider(folderPath));
		IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(PrefEditorPlugin.PLUGIN_ID);
		node.put(CHOOSED_FOLDER_PREF, folderPath);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			PrefEditorPlugin.log(e);
		}
		setViewerTitle(folderPath);
	}
	
	public void reloadPrefs() {
		TreePath[] paths = viewer.getExpandedTreePaths();
		viewer.setInput(new FolderPreferenceProvider(folderPath));
		viewer.setExpandedTreePaths(paths);
	}

	public String getFolderPath() {
		return folderPath;
	}

}
