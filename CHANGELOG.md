# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [0.1.24] - 2025-07-02
### Fixed
- Fix `:className` concatenation. Thanks to @valerauko!

## [0.1.23] - 2025-05-13
### Changed
- Fix are-props-equal? for component macro

## [0.1.22] - 2025-05-13
### Changed
- Add a new arity to `make-widget-async` to provide a different widget shape.

## [0.1.1] - 2025-01-11
### Changed
- Documentation on how to make the widgets.

### Removed
- `make-widget-sync` - we're all async, all the time.

### Fixed
- Fixed widget maker to keep working when daylight savings switches over.

## 0.1.0 - 2025-01-11
### Added
- Files from the new template.
- Widget maker public API - `make-widget-sync`.
