package com.onpositive.prefeditor.ui.iternal;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.widgets.Display;

public class ViewerRefreshJob extends ReschedulableJob {

	private Viewer viewer;

	public ViewerRefreshJob(Viewer viewer) {
		super("Viewer refresh", 2000);
		this.viewer = viewer;
	}

	@Override
	protected IStatus execute() {
		Display.getDefault().asyncExec(() -> {
			if (!viewer.getControl().isDisposed()) {
				viewer.refresh();
			}
		});
		return Status.OK_STATUS;
	}

}
