package com.onpositive.prefeditor.ui.iternal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.widgets.Display;

import com.onpositive.prefeditor.views.PreferenceFilter;

public class SetFilterJob extends ReschedulableJob {

	private PreferenceFilter preferenceFilter;
	private String filterText;
	private TreeViewer viewer;

	public SetFilterJob(TreeViewer viewer, PreferenceFilter preferenceFilter) {
		super("Set pref filter", 300);
		this.viewer = viewer;
		this.preferenceFilter = preferenceFilter;
		setUser(false);
		setSystem(true);
	}
	
	public void setFilterText(String text) {
		this.filterText = text.trim();
		if (filterText.length() < PreferenceFilter.MIN_FILTER_CHARS) {
			filterText = "";
		} 
		checkReschedule();
	}

	@Override
	protected IStatus execute() {
		preferenceFilter.setFilterText(filterText);
		Display.getDefault().asyncExec(() -> {
			viewer.refresh();
		});
		return Status.OK_STATUS;
	}

}
