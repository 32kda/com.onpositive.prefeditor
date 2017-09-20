package com.onpositive.prefeditor.model;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.core.runtime.preferences.BundleDefaultsScope;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.DefaultScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.INodeChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.NodeChangeEvent;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.core.runtime.preferences.IScopeContext;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.osgi.service.prefs.BackingStoreException;
import org.osgi.service.prefs.Preferences;

import com.onpositive.prefeditor.PrefEditorPlugin;
import com.onpositive.prefeditor.ui.iternal.PrefUIUtil;

public class PlatformPreferenceProvider implements IPreferenceProvider {
	
	public static IScopeContext[] READ_ONLY_CONTEXTS = {BundleDefaultsScope.INSTANCE, DefaultScope.INSTANCE}; 
	
	private static IScopeContext[] SCOPE_CONTEXTS = {BundleDefaultsScope.INSTANCE, ConfigurationScope.INSTANCE, DefaultScope.INSTANCE, InstanceScope.INSTANCE}; 
	
	private Map<String, List<KeyValue>> preferenceEntries = new HashMap<>();

	private boolean tracking = true;
	
	private IPreferenceUpdateCallback updateCallback;

	private IPreferenceChangeListener changeListener = new IPreferenceChangeListener() {
		
		@Override
		public void preferenceChange(PreferenceChangeEvent event) {
			if (tracking) {
				String id = event.getSource().toString();
				List<KeyValue> preferenceList = preferenceEntries.get(id);
				if (null == preferenceList) {
					preferenceList = new ArrayList<>();
					preferenceEntries.put(id, preferenceList);
				}
				Optional<KeyValue> first = preferenceList.stream().filter(val -> val.getKey().equals(event.getKey())).findFirst();
				if (first.isPresent()) {
					first.get().setValue(PrefUIUtil.emptyIfNull(event.getNewValue()));
				} else {
					KeyValue toAdd = new KeyValue(id, event.getKey(), PrefUIUtil.emptyIfNull(event.getNewValue()));
					preferenceList.add(toAdd); 
				}
				firePreferencesUpdated(id);
			}
		}
	};

	private INodeChangeListener nodeListener = new INodeChangeListener() {
		
		@Override
		public void removed(NodeChangeEvent event) {
			if (event.getChild() instanceof IEclipsePreferences) { 
				((IEclipsePreferences) event.getChild()).removePreferenceChangeListener(changeListener);
			}
			if (tracking) {
				preferenceEntries.remove(event.getChild().absolutePath());
				firePreferencesUpdated("");
			}
		}
		
		@Override
		public void added(NodeChangeEvent event) {
			if (tracking && event.getChild() instanceof IEclipsePreferences) {
				((IEclipsePreferences) event.getChild()).addPreferenceChangeListener(changeListener);
				loadPrefsFromNode(event.getChild());
				firePreferencesUpdated("");
			}
		}
	};
	
	public PlatformPreferenceProvider() {
		loadPrefs();
		addNodeListeners();
	}

	protected void firePreferencesUpdated(String categoryId) {
		if (updateCallback != null) {
			updateCallback.preferencesUpdated(categoryId);
		}
	}

	protected void addNodeListeners() {
		for (IScopeContext scopeContext : SCOPE_CONTEXTS) {
			try {
				IEclipsePreferences node = scopeContext.getNode("");
				node.accept((curNode) -> {
						curNode.addPreferenceChangeListener(changeListener);
						return true;
				});
				node.addNodeChangeListener(nodeListener);
			} catch (BackingStoreException e) {
				PrefEditorPlugin.log(e);
			}
		}
	}
	
	protected void removeNodeListeners() {
		for (IScopeContext scopeContext : SCOPE_CONTEXTS) {
			try {
				IEclipsePreferences node = scopeContext.getNode("");
				node.removeNodeChangeListener(nodeListener);
				node.accept((curNode) -> {
						curNode.removePreferenceChangeListener(changeListener);
						return true;
				});
			} catch (BackingStoreException e) {
				PrefEditorPlugin.log(e);
			}
		}
	}

	protected void loadPrefs() {
		try {
			for (IScopeContext scopeContext : SCOPE_CONTEXTS) {
				IEclipsePreferences node = scopeContext.getNode("");
				node.accept((curNode) -> {
											loadPrefsFromNode(curNode);
											return true;
										 });
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
			String preffix = "/" + scopeContext.getName() + "/";
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

	public boolean isTracking() {
		return tracking;
	}

	public void setTracking(boolean tracking) {
		this.tracking = tracking;
		if (tracking) {
			addNodeListeners();
		} else {
			removeNodeListeners();
		}
	}

	protected void loadPrefsFromNode(Preferences preferences) {
		try {
			String[] keys = preferences.keys();
			if (keys.length <= 0) {
				return;
			}
			List<KeyValue> preferenceList = preferenceEntries.get(preferences.absolutePath());
			if (null == preferenceList) {
				preferenceList = new ArrayList<>();
				preferenceEntries.put(preferences.absolutePath(), preferenceList);
			}
			for (String key : keys) {
				String value = preferences.get(key, "*default*");
				KeyValue current = new KeyValue(preferences.absolutePath(), key, value);
				preferenceList.add(current);
			}
		} catch (org.osgi.service.prefs.BackingStoreException e) {
			PrefEditorPlugin.log(e);
		}
	}

	public IPreferenceUpdateCallback getUpdateCallback() {
		return updateCallback;
	}

	public void setUpdateCallback(IPreferenceUpdateCallback updateCallback) {
		this.updateCallback = updateCallback;
	}

}
