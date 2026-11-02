# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [Unreleased]

## [2.2.0] - 2026-11-02
### Added
- Select multiple in an app's menu: pick several apps, then move them to a tab or folder in one go
- Favourites can show as a row above the tabs (with a maximum row count and swipeable pages) or as the first tab
- Settings screens use separate rounded cards

### Changed
- The favourites panel uses the opposite theme to the rest of the drawer
- The drawer search button sits to the left of the tabs
- Tabs have more inner padding

### Fixed
- The favourites row no longer flickers when switching tabs
- Sideways swipes on the favourites row no longer trigger the pull-down-to-close gesture

## [2.1.0] - 2026-10-26
### Added
- Tabs in the app drawer: create them with the + button, move apps with Move to tab, add several apps at once from a tab's long-press menu, and rename or delete tabs

## [2.0.0] - 2026-10-12
### Changed
- Renamed the app to Le Launcher, with the new application ID `com.russelldickenson.lelauncher`.
  This installs as a separate app, so settings and widgets do not carry over
- Replaced the shared Fossify commons library with code in this project
- The app now follows the Material 3 theme, including dynamic colors, for its colors
- Hidden icons are unhidden by tapping icons to select them, then using the toolbar button
- Only English is included; translations were removed

### Removed
- The language settings, which no longer apply without translations
- Upstream links, CI configuration, store metadata and the Fossify branding

### Fixed
- Widgets that were saved but did not appear until the home screen was rebuilt
- The app drawer briefly showing colored circles instead of icons after a cold start
