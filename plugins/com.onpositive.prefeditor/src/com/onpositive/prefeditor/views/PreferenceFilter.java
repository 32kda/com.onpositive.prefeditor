package com.onpositive.prefeditor.views;

import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;

import com.onpositive.prefeditor.model.IPreferenceProvider;
import com.onpositive.prefeditor.model.KeyValue;

public class PreferenceFilter extends ViewerFilter {
	
	public static final int MIN_FILTER_CHARS = 3;

    private String searchString;

    public void setFilterText(String searchString) {
        this.searchString = searchString.toLowerCase().trim();
    }

    @Override
    public boolean select(Viewer viewer,
            Object parentElement,
            Object element) {
        if (searchString == null || searchString.length() == 0) {
            return true;
        }
        if (element instanceof String) {
        	if (((String) element).toLowerCase().indexOf(searchString) > -1) {
        		return true;
        	}
        	Object input = viewer.getInput();
        	if (input instanceof IPreferenceProvider) {
        		KeyValue[] prefs = ((IPreferenceProvider) input).getPrefsFor((String) element);
        		for (KeyValue keyValue : prefs) {
					if (keyValue.getKey().toLowerCase().indexOf(searchString) > -1) {
						return true;
					}
				}
        	}
        	return false;
        }
        if (element instanceof KeyValue) {
        	return ((KeyValue) element).getKey().toLowerCase().indexOf(searchString) > -1 ||
        			((KeyValue) element).getParentNode().toLowerCase().indexOf(searchString) > -1;
        }
        return false;
    }
    
}