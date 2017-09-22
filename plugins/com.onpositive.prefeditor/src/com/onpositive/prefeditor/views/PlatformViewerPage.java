package com.onpositive.prefeditor.views;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.services.IDisposable;

import com.onpositive.prefeditor.model.IPreferenceProvider;
import com.onpositive.prefeditor.model.KeyValue;
import com.onpositive.prefeditor.model.PlatformPreferenceProvider;
import com.onpositive.prefeditor.ui.iternal.ViewerRefreshJob;

public class PlatformViewerPage extends ViewerPage {

	private ReadOnlyFilter readOnlyFilter;
	
	private ViewerRefreshJob refreshJob;

	public PlatformViewerPage(Composite parent, PreferenceView preferenceView) {
		super(parent, preferenceView);
		readOnlyFilter = new ReadOnlyFilter();
		viewer.addFilter(readOnlyFilter);
		refreshJob = new ViewerRefreshJob(viewer);
	}

	@Override
	protected void initializeInput() {
		IPreferenceProvider platformPreferenceProvider = new PlatformPreferenceProvider();
		platformPreferenceProvider.setTracking(true);
		platformPreferenceProvider.setUpdateCallback((id) -> {
			refreshJob.checkReschedule();
		});
		viewer.setInput(platformPreferenceProvider);
	}
	
	@Override
	protected void refreshTree() {
		reloadPrefs();
	}

	@Override
	public void reloadPrefs() {
		TreePath[] paths = viewer.getExpandedTreePaths();
		PlatformPreferenceProvider provider = (PlatformPreferenceProvider) viewer.getInput();
		provider.reload();
		viewer.refresh();
		viewer.setExpandedTreePaths(paths);
	}

	@Override
	protected void createTopArea(Composite con) {
		createFilterControls(con);
	}
	
	public void setShowReadOnly(boolean showReadOnly) {
		readOnlyFilter.setEnabled(!showReadOnly);
		TreePath[] paths = viewer.getExpandedTreePaths();
		viewer.refresh();
		viewer.setExpandedTreePaths(paths);
	}
	
	@Override
	public boolean isSelectionEditable() {
		ISelection selection = getSelection();
		if (!selection.isEmpty()) {
			Object firstElement = ((StructuredSelection) selection).getFirstElement();
			String nodeId = "";
			if (firstElement instanceof KeyValue) {
				KeyValue keyValue = (KeyValue) firstElement;
				nodeId = keyValue.getParentNode();
			} else {
				nodeId = firstElement.toString();
			}
			return (!nodeId.startsWith("/default/") && (!nodeId.startsWith("/bundle_defaults/")));
		}
		return false;
	}
	
	@Override
	public void dispose() {
		Object input = viewer.getInput();
		if (input instanceof IDisposable) {
			((IDisposable) input).dispose();
		}
		super.dispose();
	}
}
