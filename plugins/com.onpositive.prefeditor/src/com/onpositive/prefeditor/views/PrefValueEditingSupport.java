package com.onpositive.prefeditor.views;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.widgets.Composite;

import com.onpositive.prefeditor.model.KeyValue;
import com.onpositive.prefeditor.model.PreferenceProvider;

public class PrefValueEditingSupport extends EditingSupport {
	
	protected TextCellEditor cellEditor = new TextCellEditor();

	public PrefValueEditingSupport(ColumnViewer viewer) {
		super(viewer);
		cellEditor = new TextCellEditor((Composite) viewer.getControl());
	}

	@Override
	protected boolean canEdit(Object element) {
		return element instanceof KeyValue;
	}

	@Override
	protected CellEditor getCellEditor(Object element) {
		return cellEditor;
	}

	@Override
	protected Object getValue(Object element) {
		if (element instanceof KeyValue) {
			return ((KeyValue) element).getValue();
		}
		return null;
	}

	@Override
	protected void setValue(Object element, Object userInputValue) {
		if (element instanceof KeyValue) {
			((KeyValue) element).setValue(String.valueOf(userInputValue));
			Object input = getViewer().getInput();
			if (input instanceof PreferenceProvider) {
				((PreferenceProvider) input).updateValue(((KeyValue) element));
			}
			getViewer().update(element, null);
		}
	}

}
