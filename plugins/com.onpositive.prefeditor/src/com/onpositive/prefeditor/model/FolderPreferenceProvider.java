package com.onpositive.prefeditor.model;

import java.io.BufferedInputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.StandardWatchEventKinds;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.eclipse.ui.services.IDisposable;

import com.onpositive.prefeditor.PrefEditorPlugin;

public class FolderPreferenceProvider implements IPreferenceProvider, IDisposable {
	
	protected static final String EXT = ".prefs";
	
	protected Map<String, Properties> propFiles = new HashMap<>();
	
	protected WatchService watcher; 
	
	protected ScheduledExecutorService scheduledExecutorService = Executors.newScheduledThreadPool(1);
	
	final protected File preferenceFolder;

	private boolean tracking;

	private Runnable watchRunnable = new Runnable() {
		
		@Override
		public void run() {
			WatchKey key;
		    try {
		        // wait for a key to be available
		        key = watcher.take();
		    } catch (InterruptedException ex) {
		        return;
		    }
		    List<WatchEvent<?>> events = key.pollEvents();
		    if (events.stream().anyMatch(event -> event.kind() == StandardWatchEventKinds.OVERFLOW)) {
		    	propFiles.clear();
		    	loadPrefs();
		    } else {
		    	for (WatchEvent<?> watchEvent : events) {
		    		@SuppressWarnings("unchecked")
		    		WatchEvent<Path> ev = (WatchEvent<Path>) watchEvent;
					if (watchEvent.kind() == StandardWatchEventKinds.ENTRY_DELETE) {
						String name = ev.context().getFileName().toString();
						if (name.endsWith(EXT)) {
							name = name.substring(0, name.length() - EXT.length());
						}
						propFiles.remove(name);
					} else {
						File file = ev.context().toFile();
						if (!ev.context().isAbsolute()) {
							file = new File(preferenceFolder, file.getName());
						}
						loadPrefsFromFile(file);
					}
				}
		    }
		    if (events.size() > 0) {
		    	firePrefsChanged();
		    }
		 
		    // IMPORTANT: The key must be reset after processed
		    boolean valid = key.reset();
		    if (!valid) {
		        return;
		    }
			
		}
	};

	private ScheduledFuture<?> scheduledFuture;

	private IPreferenceUpdateCallback updateCallback;
	
	public FolderPreferenceProvider(String prefsFolderPath) {
		this(new File(prefsFolderPath));
	}
	
	public FolderPreferenceProvider(File preferenceFolder) {
		super();
		this.preferenceFolder = preferenceFolder;
		loadPrefs();
		try {
			watcher = FileSystems.getDefault().newWatchService();
			preferenceFolder.toPath().register(watcher, StandardWatchEventKinds.ENTRY_CREATE, StandardWatchEventKinds.ENTRY_DELETE, StandardWatchEventKinds.ENTRY_MODIFY);
		} catch (IOException e) {
			PrefEditorPlugin.log(e);
		}
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
			loadPrefsFromFile(file);
		}
	}

	protected void loadPrefsFromFile(File file) {
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

	public void setUpdateCallback(IPreferenceUpdateCallback updateCallback) {
		this.updateCallback = updateCallback;
	}

	public IPreferenceUpdateCallback getUpdateCallback() {
		return updateCallback;
	}

	public void setTracking(boolean tracking) {
		this.tracking = tracking;
		if (tracking) {
			scheduledFuture = scheduledExecutorService.scheduleAtFixedRate(watchRunnable , 0L, 2L, TimeUnit.SECONDS);
		} else {
			scheduledFuture.cancel(true);
		}
	}

	public boolean isTracking() {
		return tracking;
	}

	protected void firePrefsChanged() {
		updateCallback.preferencesUpdated("");
	}

	@Override
	public void dispose() {
		scheduledFuture.cancel(false);
		scheduledExecutorService.shutdown();
	}

}
