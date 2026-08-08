# Changelog

All notable changes to this project are documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.3.0] - 2026-08-08

### Added

- Pressing `Enter` opens the selected file, so the list is usable without a mouse.
- MIT license.

### Fixed

- The `FileEditorManagerListener` subscription is now bound to the tool window's
  `Disposable` instead of the project message bus alone, so it is released when the
  tool window goes away.
- Hover and selection colors come from `JBUI.CurrentTheme.List` instead of
  `UIUtil.getPanelBackground().darker()`, which produced a near-black hover row in
  dark themes.
- The selected row is preserved when the list is rebuilt after a tab switch.

### Changed

- Plugin metadata (group, version, since-build, platform version) moved to
  `gradle.properties`.
- Signing and Marketplace publishing configured via environment variables.

## [1.2] - 2026-08-08

### Changed

- Updated to IntelliJ Platform 2026.2 and the Gradle IntelliJ Platform Plugin 2.x.

## [1.1] - 2025-09-24

### Added

- Tool window listing recently activated editor tabs.
