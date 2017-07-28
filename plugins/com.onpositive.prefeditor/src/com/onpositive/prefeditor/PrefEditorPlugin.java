package com.onpositive.prefeditor;

import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;
import org.osgi.service.prefs.BackingStoreException;

/**
 * The activator class controls the plug-in life cycle
 */
public class PrefEditorPlugin extends AbstractUIPlugin {

	// The plug-in ID
	public static final String PLUGIN_ID = "com.onpositive.prefeditor"; //$NON-NLS-1$

	// The shared instance
	private static PrefEditorPlugin plugin;
	
	/**
	 * Prev selected preference folders preference key
	 */
	private static final String PREV_FOLDERS_KEY = "prevFolders";
	
	/**
	 * The constructor
	 */
	public PrefEditorPlugin() {
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static PrefEditorPlugin getDefault() {
		return plugin;
	}

	/**
	 * Returns an image descriptor for the image file at the given
	 * plug-in relative path
	 *
	 * @param path the path
	 * @return the image descriptor
	 */
	public static ImageDescriptor getImageDescriptor(String path) {
		return imageDescriptorFromPlugin(PLUGIN_ID, path);
	}

	public static void log(Throwable e) {
		getDefault().getLog().log(new Status(IStatus.ERROR,PLUGIN_ID, e.getMessage(), e));
	}

	public static void log(String errorMessage) {
		getDefault().getLog().log(new Status(IStatus.ERROR,PLUGIN_ID, errorMessage));
	}
	
	public static String[] getPrevFolderChoices() {
		String prevFolders = InstanceScope.INSTANCE.getNode(PrefEditorPlugin.PLUGIN_ID).get(PREV_FOLDERS_KEY,"");
		String[] folders = prevFolders.split(";");
		return folders;
	}
	
	public static void savePrevFolderChoices(String[] choices) {
		IEclipsePreferences node = InstanceScope.INSTANCE.getNode(PrefEditorPlugin.PLUGIN_ID);
		node.put(PREV_FOLDERS_KEY, String.join(";", choices));
		try {
			node.flush();
		} catch (BackingStoreException e) {
			PrefEditorPlugin.log(e);
		}
	}
}
