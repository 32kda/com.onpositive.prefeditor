package com.onpositive.prefeditor.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.preferences.BundleDefaultsScope;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IPreferenceNodeVisitor;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.prefs.BackingStoreException;

import com.onpositive.prefeditor.PrefEditorPlugin;

public class PlatformPreferenceProvider implements IPreferenceProvider {
	
	private static IScopeContext[] SCOPE_CONTEXTS = {BundleDefaultsScope.INSTANCE, ConfigurationScope.INSTANCE, DefaultScope.INSTANCE, InstanceScope.INSTANCE}; 
	
	private class PrefereneGatherer implements IPreferenceNodeVisitor {
	
			private Map<String, List<KeyValue>> preferenceEntries;
	
			public PrefereneGatherer(Map<String, List<KeyValue>> preferenceEntries) {
				this.preferenceEntries = preferenceEntries;
			}
	
			@Override
			public boolean visit(IEclipsePreferences node) {
				// only show nodes, which have changed keys
				try {
					String[] keys = node.keys();
					if (keys.length <= 0) {
						return true;
					}
					List<KeyValue> preferenceList = preferenceEntries.get(node.absolutePath());
					if (null == preferenceList) {
						preferenceList = new ArrayList<>();
						preferenceEntries.put(node.absolutePath(), preferenceList);
					}
					for (String key : keys) {
						String value = node.get(key, "*default*");
						KeyValue current = new KeyValue(node.absolutePath(), key, value);
						preferenceList.add(current);
					}
				} catch (org.osgi.service.prefs.BackingStoreException e) {
					PrefEditorPlugin.log(e);
				}
				return true;
			}
	}

	private Map<String, List<KeyValue>> preferenceEntries = new HashMap<>();
	
	public PlatformPreferenceProvider() {
		loadPrefs();
	}

	protected void loadPrefs() {
		try {
			for (IScopeContext scopeContext : SCOPE_CONTEXTS) {
				scopeContext.getNode("").accept(new PrefereneGatherer(preferenceEntries));
			}
		} catch (BackingStoreException e) {
			PrefEditorPlugin.log(e);
		}
	}
	
	@Override
	public String[] getNodeNames() {
		return preferenceEntries.keySet().stream().sorted().toArray(String[]::new);
	}

	@Override
	public KeyValue[] getPrefsFor(String categoryName) {
		List<KeyValue> prefs = preferenceEntries.get(categoryName);
		if (prefs != null)
			return prefs.toArray(new KeyValue[0]);
		return null;
	}

	@Override
	public void updateValue(KeyValue keyValue) {
		String parentCategory = keyValue.getParentNode();	
		IEclipsePreferences node = getNode(parentCategory);
		if (node == null) {
			PrefEditorPlugin.log("Node " + parentCategory + " can't be obtained");
			return;
		}
		node.put(keyValue.getKey(), keyValue.getValue());
		flush(node);
	}

	protected void flush(IEclipsePreferences node) {
		try {
			node.flush();
		} catch (BackingStoreException e) {
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Error saving preference", "Error saving preference value for node " + node.name() + " . Check error log for details");
			PrefEditorPlugin.log(e);
		}
	}

	@Override
	public void remove(KeyValue keyValue) {
		String parentCategory = keyValue.getParentNode();	
		IEclipsePreferences node = getNode(parentCategory);
		if (node == null) {
			PrefEditorPlugin.log("Node " + parentCategory + " can't be obtained");
			return;
		}
		node.remove(keyValue.getKey());
		flush(node);
	}

	@Override
	public void add(KeyValue newElement) {
		updateValue(newElement); //Update & add is same op for platform prefs
	}

	@Override
	public void removeCategory(String category) {
		IEclipsePreferences node = getNode(category);
		if (node == null) {
			PrefEditorPlugin.log("Node " + category + " can't be obtained");
			return;
		}
		try {
			node.clear();
		} catch (BackingStoreException e) {
			PrefEditorPlugin.log(e);
		}
	}
	
	protected IEclipsePreferences getNode(String categoryName) {
		for (IScopeContext scopeContext : SCOPE_CONTEXTS) {
			String preffix = "/" + scopeContext.getName();
			if (categoryName.startsWith(preffix)) {
				IEclipsePreferences node = scopeContext.getNode(categoryName.substring(preffix.length()));
				return node;
			}
			
		}
		return null;
	}

	public void reload() {
		preferenceEntries.clear();
		loadPrefs();
	}

}
