package com.onpositive.prefeditor.views;

import static com.onpositive.prefeditor.PrefConstants.CONFIGURATION_SETTINGS_PATH;
import static com.onpositive.prefeditor.PrefConstants.WORKSPACE_SETTINGS_PATH;

import java.io.File;
import java.net.URL;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.ConfigurationScope;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.services.IDisposable;
import org.osgi.service.prefs.BackingStoreException;

import com.onpositive.prefeditor.PrefEditorPlugin;
import com.onpositive.prefeditor.model.FolderPreferenceProvider;
import com.onpositive.prefeditor.ui.iternal.PrefUIUtil;

public class FolderViewerPage extends ViewerPage {
	
	private static final String CHOOSED_FOLDER_PREF = "choosedFolder";
	
	private static final int MAX_TITLE_LENGTH = 40;

	private Label titleLabel;

	private String folderPath;

	public FolderViewerPage(Composite parent, PreferenceView preferenceView) {
		super(parent, preferenceView);
	}
	
	protected void createTopArea(Composite con) {
		titleLabel = new Label(con, SWT.NONE);
		GridDataFactory.fillDefaults().grab(true,false).span(3,1).applyTo(titleLabel);
		createFilterControls(con);
	}

	private void setViewerTitle(String folderPath) {
		titleLabel.setToolTipText("");
		if (folderPath == null || folderPath.isEmpty()) {
			titleLabel.setText("Select folder to view preferences");
			return;
		} 
		titleLabel.setText(PrefUIUtil.getFolderLabel(folderPath, MAX_TITLE_LENGTH));
		titleLabel.setToolTipText(folderPath);
	}
	
	protected String getDefaultFolder() {
		File folder = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toOSString(), WORKSPACE_SETTINGS_PATH);
		if (folder.isDirectory()) {
			return folder.getAbsolutePath();
		}
		folder = toFile(Platform.getInstallLocation().getURL());
		if (folder.isDirectory()) {
			return new File(folder, CONFIGURATION_SETTINGS_PATH).getAbsolutePath();
		}
		return "";
	}

	@Override
	protected void initializeInput() {
		folderPath = getInitialFolder();
		setupPreferenceProvider(true);
		setViewerTitle(folderPath);
	}
	
	protected String getInitialFolder() {
		return ConfigurationScope.INSTANCE.getNode(PrefEditorPlugin.PLUGIN_ID).get(CHOOSED_FOLDER_PREF, getDefaultFolder());
	}
	
	public void folderChoosed(String folderPath) {
		this.folderPath = folderPath;
		doResetProvider();
		IEclipsePreferences node = ConfigurationScope.INSTANCE.getNode(PrefEditorPlugin.PLUGIN_ID);
		node.put(CHOOSED_FOLDER_PREF, folderPath);
		try {
			node.flush();
		} catch (BackingStoreException e) {
			PrefEditorPlugin.log(e);
		}
		setViewerTitle(folderPath);
	}
	
	public void reloadPrefs() {
		TreePath[] paths = viewer.getExpandedTreePaths();
		doResetProvider();
		viewer.setExpandedTreePaths(paths);
	}

	protected void doResetProvider() {
		boolean tracking = true;
		Object input = viewer.getInput();
		if (input instanceof FolderPreferenceProvider) {
			((IDisposable) input).dispose();
			tracking = ((FolderPreferenceProvider) input).isTracking();
		}
		setupPreferenceProvider(tracking);
	}

	protected void setupPreferenceProvider(boolean tracking) {
		FolderPreferenceProvider folderPreferenceProvider = new FolderPreferenceProvider(folderPath);
		folderPreferenceProvider.setTracking(tracking);
		folderPreferenceProvider.setUpdateCallback((id) -> {
			Display.getDefault().asyncExec(() -> {
				if (!viewer.getControl().isDisposed()) {
					viewer.refresh();
				}
			});
		});
		viewer.setInput(folderPreferenceProvider);
	}

	public String getFolderPath() {
		return folderPath;
	}

	public void copyPath() {
		textToClipboard(folderPath);		
	}
	
	@Override
	public void dispose() {
		Object input = viewer.getInput();
		if (input instanceof IDisposable) {
			((IDisposable) input).dispose();
		}
		super.dispose();
	}
	
	//-----------------------------------------------------------------------
	//Taken from Apache Commons to avoid this dependency because of single method
    /**
     * Convert from a <code>URL</code> to a <code>File</code>.
     * <p>
     * From version 1.1 this method will decode the URL.
     * Syntax such as <code>file:///my%20docs/file.txt</code> will be
     * correctly decoded to <code>/my docs/file.txt</code>. Starting with version
     * 1.5, this method uses UTF-8 to decode percent-encoded octets to characters.
     * Additionally, malformed percent-encoded octets are handled leniently by
     * passing them through literally.
     *
     * @param url the file URL to convert, {@code null} returns {@code null}
     * @return the equivalent <code>File</code> object, or {@code null}
     * if the URL's protocol is not <code>file</code>
     */
    public static File toFile(final URL url) {
        if (url == null || !"file".equalsIgnoreCase(url.getProtocol())) {
            return null;
        } else {
            String filename = url.getFile().replace('/', File.separatorChar);
            filename = decodeUrl(filename);
            return new File(filename);
        }
    }

    /**
     * Decodes the specified URL as per RFC 3986, i.e. transforms
     * percent-encoded octets to characters by decoding with the UTF-8 character
     * set. This function is primarily intended for usage with
     * {@link java.net.URL} which unfortunately does not enforce proper URLs. As
     * such, this method will leniently accept invalid characters or malformed
     * percent-encoded octets and simply pass them literally through to the
     * result string. Except for rare edge cases, this will make unencoded URLs
     * pass through unaltered.
     *
     * @param url The URL to decode, may be {@code null}.
     * @return The decoded URL or {@code null} if the input was
     * {@code null}.
     */
    static String decodeUrl(final String url) {
        String decoded = url;
        if (url != null && url.indexOf('%') >= 0) {
            final int n = url.length();
            final StringBuilder buffer = new StringBuilder();
            final ByteBuffer bytes = ByteBuffer.allocate(n);
            for (int i = 0; i < n; ) {
                if (url.charAt(i) == '%') {
                    try {
                        do {
                            final byte octet = (byte) Integer.parseInt(url.substring(i + 1, i + 3), 16);
                            bytes.put(octet);
                            i += 3;
                        } while (i < n && url.charAt(i) == '%');
                        continue;
                    } catch (final RuntimeException e) {
                        // malformed percent-encoded octet, fall through and
                        // append characters literally
                    } finally {
                        if (bytes.position() > 0) {
                            bytes.flip();
                            buffer.append(StandardCharsets.UTF_8.decode(bytes).toString());
                            bytes.clear();
                        }
                    }
                }
                buffer.append(url.charAt(i++));
            }
            decoded = buffer.toString();
        }
        return decoded;
    }

}
