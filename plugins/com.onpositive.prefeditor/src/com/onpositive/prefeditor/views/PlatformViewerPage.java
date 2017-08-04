package com.onpositive.prefeditor.views;

import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.widgets.Composite;

import com.onpositive.prefeditor.model.PlatformPreferenceProvider;

public class PlatformViewerPage extends ViewerPage {

	public PlatformViewerPage(Composite parent, PreferenceView preferenceView) {
		super(parent, preferenceView);
	}

	@Override
	protected void initializeInput() {
		viewer.setInput(new PlatformPreferenceProvider());
	}

	@Override
	public void reloadPrefs() {
		TreePath[] paths = viewer.getExpandedTreePaths();
		PlatformPreferenceProvider provider = (PlatformPreferenceProvider) viewer.getInput();
		provider.reload();
		viewer.refresh();
		viewer.setExpandedTreePaths(paths);
	}
}
