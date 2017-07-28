package com.onpositive.prefeditor.ui.iternal;

import static com.onpositive.prefeditor.PrefConstants.CONFIGURATION_SETTINGS_PATH;
import static com.onpositive.prefeditor.PrefConstants.WORKSPACE_SETTINGS_PATH;

import org.eclipse.core.runtime.Path;

public class PrefUIUtil {
	
	public static String getFolderLabel(String folderPath, int maxLength) {
		if (folderPath != null) {
			folderPath = folderPath.trim().replace('\\','/');
			if (folderPath.endsWith("/")) {
				folderPath = folderPath.substring(0, folderPath.length() - 1);
			}
			if (folderPath.endsWith(WORKSPACE_SETTINGS_PATH)) {
				String path = folderPath.substring(0, folderPath.length() - WORKSPACE_SETTINGS_PATH.length());
				String name = new Path(path).lastSegment();
				return "Workspace: " + name;
			} else if (folderPath.endsWith(CONFIGURATION_SETTINGS_PATH)) {
				String path = folderPath.substring(0, folderPath.length() - CONFIGURATION_SETTINGS_PATH.length());
				String name = new Path(path).lastSegment();
				return "Installation: " + name;
			} else {
				String title = folderPath;
				int len = title.length();
				if (len > maxLength) {
					title = "..." + title.substring(len - maxLength, len);
				}
				return title;
			}
		}
		return folderPath;
	}
	
}
