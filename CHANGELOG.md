<!-- Keep a Changelog guide -> https://keepachangelog.com -->

# intellij-project-identifier-plugin Changelog

## [Unreleased]

## [0.0.8] - 2026-07-13
### Fixed
- Clicking **Apply** in the settings now updates the editor and empty-frame background watermark immediately, with the settings window still open. Previously the change often appeared only after clicking OK or Cancel.

## [0.0.7] - 2026-07-10
### Added
- Live preview in the per-project settings page, shown beside the controls: the rendered identifier updates as you change the text, font, size or color, so you can compare options without leaving the dialog. It shows the text content at full opacity over the current IDE background; on-screen opacity and position are still set on the IDE Background Image page.

## [0.0.6] - 2026-07-08
### Changed
- Internal code refactoring and project documentation restructure — no user-facing changes.
- Updated build tooling: Kotlin 2.2.21, IntelliJ Platform Gradle Plugin 2.10.5, Kover 0.9.3, Qodana 2025.2.2.

## [0.0.5] - 2026-07-07
### Added
- Dynamic `${branch}` placeholder in the per-project identifier override: embed the current Git branch in the watermark (for example `XXX - ${branch}`). The watermark refreshes automatically when you switch branches - instantly via the bundled Git plugin, or shortly after by reading `.git/HEAD` when it is unavailable. `${branch}` becomes an empty string when the project has no branch (not a Git repository, or a detached HEAD). A help tooltip on the field documents the syntax (English and French).
### Fixed
- The editor and empty-frame background could briefly point at a deleted image when the watermark was refreshed several times in quick succession, leaving it blank until the next change.
- A project's watermark cleanup could delete another open project's watermark when their names shared a prefix (for example "shop" and "shop-api").
- Very long identifier or branch text is no longer silently dropped when it would exceed the filesystem file-name limit.
### Changed
- The build resolves Gradle plugins from public repositories, so it works without access to an internal repository mirror.

## [0.0.4] - 2025-10-09
### Fixed
- Verify task functionality
### Changed
- Project identifier PNG now displays also on empty frames

## [0.0.3] - 2025-10-08
### Added
- Global settings menu for application-wide configuration
### Changed
- Project-specific settings menu as child menu for per-project customization

## [0.0.2] - 2025-10-01
### Changed
- Bump gradle action from 4 to 5
- Light refactor code proposed by sonnet 4.5

## [0.0.1] - 2025-10-01
### Added
- Automatic text watermark generation on the editor background from the project name
- Transparent PNG image generation and application
- Customizable identifier rules in Settings (Appearance & Behavior | Appearance)
- Auto-refresh when settings change

[unreleased]: https://github.com/Lucsartes/intellij-project-identifier-plugin/compare/v0.0.7...HEAD
[0.0.7]: https://github.com/Lucsartes/intellij-project-identifier-plugin/compare/v0.0.6...v0.0.7
[0.0.6]: https://github.com/Lucsartes/intellij-project-identifier-plugin/compare/v0.0.5...v0.0.6
[0.0.5]: https://github.com/Lucsartes/intellij-project-identifier-plugin/compare/v0.0.4...v0.0.5
[0.0.4]: https://github.com/Lucsartes/intellij-project-identifier-plugin/compare/v0.0.3...v0.0.4
[0.0.3]: https://github.com/Lucsartes/intellij-project-identifier-plugin/compare/v0.0.2...v0.0.3
[0.0.2]: https://github.com/Lucsartes/intellij-project-identifier-plugin/compare/v0.0.1...v0.0.2
[0.0.1]: https://github.com/Lucsartes/intellij-project-identifier-plugin/releases/tag/v0.0.1
