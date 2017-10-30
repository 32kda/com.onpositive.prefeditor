# Preference Editor

Eclipse Preference Editor would be useful for developers debugging Eclipse/RCP applications - it allows to view/edit preferences from the workspace, configuration area, debugged configuration area or some arbitrary folder.

### What is preferences?

You are supposed to know [Eclipse](https://www.eclipse.org/) Plug-in/RCP development basics, if you need to edit preference. I can recommend [this tutorial](http://www.vogella.com/tutorials/EclipsePreferences/article.html) if you want to read more about Eclipse preferences, their scopes and other related details.

## Editing modes
Since version 1.1, both "offline" (preference files) and "online" (running platform) editing modes are supported. Only "Offline" was supported before.

### "Folder"/"Offline" editing
This mode allows changing preferences by editing special files containing them prior to launching particular IDE or workspace, changes are saved to corresponding files immediately.
 
### "Platform"/"Online" editing
"Online" editing allows you to edit preferences from currently running platform. Since 1.3.0, plugin can listen to preference changes and refresh view automaticaly. Alternatively, you can turn off tracking and use "reload" button to refresh view content.

## Important!

The main purpose of this plugin is debugging Eclipse plugins you create. While changing preferences for someone else's plugins, please be sure you know what you do - preferences are inner stuff for plugins, and their values are usually assumed to be correct, with no extra checks performed. 

This editor is provided *"AS IS"*, with absolutely no warranty. Author is not responsible for any software problems, data loss etc. caused by it's usage.

## Installation
Drag "Install" button from [Eclipse Marketplace page](https://marketplace.eclipse.org/content/preference-editor) into your IDE. 

OR

Go to your Eclipse/Eclipse-based IDE, open _Help_-> _Install new Software..._ Paste the following URL into "Work with:" text field: https://32kda.github.io/com.onpositive.prefeditor/

## Usage
To open the view, choose _Window > Show View > Other_, from the list select category _Preferences_ and view _Preferences_

Please refer [Usage](https://github.com/32kda/com.onpositive.prefeditor/wiki/Usage) page for more details

## License
All the code is licensed under [Eclipse Public License - v 1.0](https://www.eclipse.org/legal/epl-v10.html)

## To Be Done

 * Simple content assist, like "true/false" suggestion for some props, multi-line editor window for long text
 * Possibly - importing preferences from file and opening pref file location (does someone need such a features?)
 * Better filtering configuration, like filtering off selected scopes for platform preferences

<a href="http://with-eclipse.github.io/" target="_blank">
<img alt="with-Eclipse logo" src="http://with-eclipse.github.io/with-eclipse-0.jpg" />
</a>
