package com.onpositive.prefeditor.views;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.onpositive.prefeditor.model.KeyValue;
import com.onpositive.prefeditor.model.PlatformPreferenceProvider;

public class ReadOnlyFilter extends ViewerFilter {
	
	private boolean enabled;
	
	private String[] readOnlyPrefixes;
	
	public ReadOnlyFilter() {
		readOnlyPrefixes = new String[PlatformPreferenceProvider.READ_ONLY_CONTEXTS.length];
		for (int i = 0; i < readOnlyPrefixes.length; i++) {
			readOnlyPrefixes[i] = "/" + PlatformPreferenceProvider.READ_ONLY_CONTEXTS[i].getName();
		}
	}

	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}

	@Override
	public boolean select(Viewer viewer, Object parentElement, Object element) {
		if (enabled) {
			String key = "";
			if (element instanceof String) {
				key = element.toString().toLowerCase();
			} else if (element instanceof KeyValue) {
				key = ((KeyValue) element).getKey().toLowerCase();
			}
			for (int i = 0; i < readOnlyPrefixes.length; i++) {
				if (key.startsWith(readOnlyPrefixes[i])) {
					return false;
				}
			}
		}
		return true;
	}

}
