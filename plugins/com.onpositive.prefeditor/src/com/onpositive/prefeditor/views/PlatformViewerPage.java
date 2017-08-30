package com.onpositive.prefeditor.views;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.widgets.Composite;

import com.onpositive.prefeditor.model.KeyValue;
import com.onpositive.prefeditor.model.PlatformPreferenceProvider;

public class PlatformViewerPage extends ViewerPage {

	private ReadOnlyFilter readOnlyFilter;

	public PlatformViewerPage(Composite parent, PreferenceView preferenceView) {
		super(parent, preferenceView);
		readOnlyFilter = new ReadOnlyFilter();
		viewer.addFilter(readOnlyFilter);
	}

	@Override
	protected void initializeInput() {
		viewer.setInput(new PlatformPreferenceProvider());
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
		if (!selection.isEmpty() && ((StructuredSelection) selection).getFirstElement() instanceof KeyValue) {
			KeyValue keyValue = (KeyValue) ((StructuredSelection) selection).getFirstElement();
			String parentNode = keyValue.getParentNode();
			return (!parentNode.startsWith("/default/") && (!parentNode.startsWith("/bundle_defaults/")));
		}
		return false;
	}
}
