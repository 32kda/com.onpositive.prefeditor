package com.onpositive.prefeditor.views;

import org.eclipse.jface.viewers.ColumnLabelProvider;

import com.onpositive.prefeditor.model.KeyValue;

public class PrefsLabelProvider extends ColumnLabelProvider {
	
	public static enum Column {
		KEY,
		VALUE
	}
	
	protected final Column column;
	
	protected boolean treeMode = true;

	public PrefsLabelProvider(Column column) {
		super();
		this.column = column;
	}
	
	@Override
	public String getText(Object element) {
		if (column == Column.KEY) {
			if (element instanceof String) {
				return (String) element;
			}
			if (element instanceof KeyValue) {
				String key = ((KeyValue) element).getKey();
				return treeMode? key : ((KeyValue) element).getParentNode() + "/" + key;
			}
		}
		if (column == Column.VALUE) {
			if (element instanceof String) {
				return "";
			}
			if (element instanceof KeyValue) {
				return ((KeyValue) element).getValue();
			}
		}
		return super.getText(element);
	}

	public boolean isTreeMode() {
		return treeMode;
	}

	public void setTreeMode(boolean treeMode) {
		this.treeMode = treeMode;
	}
	

}
