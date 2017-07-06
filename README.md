# Preference Editor

Eclipse Preference Editor would be useful for developers debugging Eclipse/RCP applications - it allows to view/edit preferences from the workspace, configuration area, debugged configuration area or some arbitrary folder.
### "Offline" editing
Editing can be done "offline", prior to launching particular IDE or workspace, changes are saved to corresponding files immediately. 
### "Online" editing
For now, no fully "online" changes possible - preferences are saved to files, which is also possible when IDE using these preferences is launched, but you can't be sure, when changed values would be picked and whether they wouldn't be rewrittten.

## Installation
Go to your Eclipse/Eclipse-based IDE, open _Help_-> _Install new Software..._ Paste the following URL into "Work with:" text field: https://32kda.github.io/com.onpositive.prefeditor/
