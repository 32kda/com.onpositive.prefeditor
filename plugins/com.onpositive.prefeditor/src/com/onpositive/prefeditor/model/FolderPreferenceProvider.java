package com.onpositive.prefeditor.model;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import com.onpositive.prefeditor.PrefEditorPlugin;

public class FolderPreferenceProvider implements IPreferenceProvider {
	
	protected static final String EXT = ".prefs";
	
	protected Map<String, Properties> propFiles = new HashMap<>();
	
	protected File preferenceFolder;
	
	public FolderPreferenceProvider(String prefsFolderPath) {
		this(new File(prefsFolderPath));
	}
	
	public FolderPreferenceProvider(File preferenceFolder) {
		super();
		this.preferenceFolder = preferenceFolder;
		loadPrefs();
	}

	public File getPreferenceFolder() {
		return preferenceFolder;
	}
	
	protected void loadPrefs() {
		if (!preferenceFolder.isDirectory()) {
			return;
		}
		File[] prefFiles = preferenceFolder.listFiles((parent, name) -> name.endsWith(EXT));
		for (File file : prefFiles) {
			String name = file.getName();
			if (name.endsWith(EXT)) {
				name = name.substring(0, name.length() - EXT.length());
			}
			try (InputStream stream = new BufferedInputStream(new FileInputStream(file))) {
				Properties properties = new Properties();
				properties.load(stream);
				propFiles.put(name, properties);
			} catch (FileNotFoundException e) {
				PrefEditorPlugin.log(e);
			} catch (IOException e) {
				PrefEditorPlugin.log(e);
			}
		}
	}
	
	public String[] getNodeNames() {
		return propFiles.keySet().stream().sorted().toArray(String[]::new);
	}
	
	/* (non-Javadoc)
	 * @see com.onpositive.prefeditor.model.IPreferenceProvider#getPrefsFor(java.lang.String)
	 */
	@Override
	public KeyValue[] getPrefsFor(String fileName) {
		Properties properties = propFiles.get(fileName);
		if (properties == null) {
			return new KeyValue[0];
		}
		return properties.keySet().stream().sorted().
			map(key -> new KeyValue(fileName, key.toString(), properties.getProperty(key.toString()))).
			toArray(KeyValue[]::new);
	}

	/* (non-Javadoc)
	 * @see com.onpositive.prefeditor.model.IPreferenceProvider#updateValue(com.onpositive.prefeditor.model.KeyValue)
	 */
	@Override
	public void updateValue(KeyValue keyValue) {
		String parentNode = keyValue.getParentNode();
		Properties properties = propFiles.get(parentNode);
		if (properties != null) {
			properties.put(keyValue.getKey(), keyValue.getValue());

			storeProps(parentNode + EXT, properties);
		}
		
	}

	protected void storeProps(String fileName, Properties properties) {
		preferenceFolder.mkdirs();
		try (BufferedWriter writer = new BufferedWriter(new FileWriter(new File(preferenceFolder, fileName)))) {
			properties.store(writer, null);
		} catch (IOException e) {
			PrefEditorPlugin.log(e);
		}
	}

	/* (non-Javadoc)
	 * @see com.onpositive.prefeditor.model.IPreferenceProvider#remove(com.onpositive.prefeditor.model.KeyValue)
	 */
	@Override
	public void remove(KeyValue keyValue) {
		String parentNode = keyValue.getParentNode();
		Properties properties = propFiles.get(parentNode);
		if (properties != null) {
			properties.remove(keyValue.getKey());
			storeProps(parentNode + EXT, properties);
		}
	}

	/* (non-Javadoc)
	 * @see com.onpositive.prefeditor.model.IPreferenceProvider#add(com.onpositive.prefeditor.model.KeyValue)
	 */
	@Override
	public void add(KeyValue newElement) {
		String parentNode = newElement.getParentNode();
		Properties properties = propFiles.get(parentNode);
		if (properties == null) {
			properties = new Properties();
			propFiles.put(parentNode, properties);
		}
		properties.put(newElement.getKey(), newElement.getValue());
		storeProps(parentNode + EXT, properties);
	}

	/* (non-Javadoc)
	 * @see com.onpositive.prefeditor.model.IPreferenceProvider#removeCategory(java.lang.String)
	 */
	@Override
	public void removeCategory(String category) {
		propFiles.remove(category);
		File file = new File(preferenceFolder, category + EXT);
		if (!file.delete()) {
			PrefEditorPlugin.log("File " + category + EXT + " can't be deleted");
		}
	}
}
