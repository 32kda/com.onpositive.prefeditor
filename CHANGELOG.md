# Changelog
All notable changes to Preference Editor project will be documented in this file.

The format is based on [Keep a Changelog](http://keepachangelog.com/en/1.0.0/)
and this project adheres to [Semantic Versioning](http://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [1.3.3]

### Added

- Context action to edit preference value in dialog to simplify editing long values 

## [1.3.2]

### Added

- Export preference node to file ability 

### Changed

- Multiple I/O exception like "File not found" caused by folder tracking fix 2
- Some small UI fixes 

## [1.3.1]
 
### Changed

- Fixed multiple I/O exception like "File not found" caused by folder tracking

## [1.3.0] 

### Added

- Tracking mode to refresh both platform and folder-based preferences 

### Changed

- Fixed NPE & wrong dialog type for adding folder-based preference
- Fixed show read-only for flat view mode
- Fixed invalid Remove action disablement 
- Minor UI tweaks

## [1.2.1] 

### Added

- Show/not show read-only (default, bundle defaults) nodes action for platform preferences
- Copy folder path action for folder preferences 

### Changed

- Menu retitling and dynamic re-populating

## [1.2.0] 

### Added

- Basic filtering support
- Collapse All action 
- Keybindings for Delete & Refresh

### Changed

- Fixes for in-platform preference editing

## [1.1.0] 

### Added

- Basic support for Running Platform preferences

## [1.0.0] 

### Added

- Initial support for in-folder preferences: change value, add, remove, copy name or value
- Ability to choose folder with workspace or installation metadata automatically, when pointed to it's parent workspace or installation folder.
