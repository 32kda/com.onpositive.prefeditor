package com.onpositive.prefeditor.views;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.Viewer;

import com.onpositive.prefeditor.model.IPreferenceProvider;
import com.onpositive.prefeditor.model.KeyValue;

public class PrefsContentProvider implements ITreeContentProvider {
	
	protected IPreferenceProvider provider;
	
	protected boolean treeMode = true;

	@Override
	public void dispose() {
		// do nothing
	}

	@Override
	public void inputChanged(Viewer paramViewer, Object oldInput, Object newInput) {
		if (newInput instanceof IPreferenceProvider) {
			provider = (IPreferenceProvider) newInput;
		}
	}

	@Override
	public Object[] getChildren(Object element) {
		if (treeMode && provider != null && element instanceof String) {
			return provider.getPrefsFor(element.toString());
		}
		return new Object[0];
	}

	@Override
	public Object[] getElements(Object paramObject) {
		if (provider != null) { 
			if (treeMode) {
				return provider.getNodeNames();
			} else {
				List<KeyValue> result = new ArrayList<>();
				String[] fileNames = provider.getNodeNames();
				for (String name : fileNames) {
					KeyValue[] prefs = provider.getPrefsFor(name);
					for (KeyValue keyValue : prefs) {
						result.add(keyValue); 
					}
				}
				return result.toArray();
			}
		}
		return null;
	}

	@Override
	public Object getParent(Object element) {
		if (element instanceof KeyValue) {
			return ((KeyValue) element).getParentNode();
		}
		return null;
	}

	@Override
	public boolean hasChildren(Object element) {
		if (treeMode && provider != null && element instanceof String) {
			return provider.getPrefsFor(element.toString()).length > 0;
		}
		return false;
	}

	public boolean isTreeMode() {
		return treeMode;
	}

	public void setTreeMode(boolean treeMode) {
		this.treeMode = treeMode;
	}

}
