# SPEC-0005: Internationalization

* **Status**: Accepted
* **Last updated**: 2026-09-24

## 1. Summary

All the text the plugin shows inside the IDE (settings page titles, labels, tooltips, hints, buttons) is
localized. The plugin shows it in the IDE's language when a translation exists, and in English otherwise.
English and French ship today.

## 2. Motivation

The plugin has users in different language regions. Hard-coded English would shut out non-English speakers
and turn every translation into a code change. Localizing from the start keeps the settings readable for
everyone, and new languages can be added as plain translations.

## 3. Behavior

- **Follows the IDE.** The plugin uses the IDE's configured language. It has no language setting of its own.
- **English fallback.** Any text without a translation for the active language appears in English. English is
  always complete.
- **Languages shipped.** English (default) and French.
- **What is translated.** Everything the plugin shows in the IDE: both settings page titles, every label, the
  help tooltips (including the `${branch}` one from [SPEC-0004](spec-0004-branch-placeholder.md)), the Reset
  labels and buttons, the preview, and the hint pointing to the IDE's Background Image page.
- **What stays in English.** The plugin's name ("Project Identifier") and its Marketplace / Plugins-list
  description.

## 4. Out of scope

- A language override inside the plugin: the language always comes from the IDE.
- Locale-specific formatting of numbers, dates or plurals. The texts are simple labels.
- Translating the product name or the Marketplace listing.

## 5. Related

- **Realized by**: [ADR-0004 — Internationalization implementation](../adrs/adr-0004-internationalization-implementation.md).
- **See also**: [SPEC-0003 — Settings & scopes](spec-0003-settings-and-scopes.md) (the pages that show these
  texts).
