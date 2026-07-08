# SPEC-0005: Internationalization

> Product/behavioral spec. Describes the user-facing promise about languages. The technical realization
> (resource bundles, `DynamicBundle`) lives in
> [ADR-0004](../adrs/adr-0004-internationalization-implementation.md). Keep this in sync with the code
> (see [`docs/README.md`](../README.md)).

* **Status**: Accepted
* **Last updated**: 2026-07-08
* **Owners**: Project maintainers

## 1. Summary

All user-facing text in the plugin — settings labels, tooltips, hints, buttons, and plugin metadata — is
localized. The plugin automatically shows text in the user's IDE language when a translation exists, and falls
back to English otherwise. English and French ship today.

## 2. Context & motivation

The plugin has users in different language regions. Hardcoding English would exclude non-English speakers and
make every future translation a code change. Localization from the start keeps the UI accessible and lets new
languages be added as drop-in translations.

## 3. Behavior

- **Automatic language selection.** The plugin displays its text in the IDE's configured language. There is no
  language setting inside the plugin — it follows the IDE.
- **Fallback.** If a string is not translated for the active language, the English text is shown. English is
  the base language and is always complete.
- **Currently shipped languages.** English (default) and French.
- **Scope of translation.** Everything the user reads from the plugin is translatable: the two settings page
  titles, every field label, the help tooltips (including the `${branch}` tooltip from
  [SPEC-0004](spec-0004-branch-placeholder.md)), reset labels/buttons, the "opacity/position lives in the IDE"
  hint, and the plugin's name/description in the marketplace and Plugins list.

## 4. Non-goals / out of scope

- No in-plugin language override — language is inherited from the IDE.
- No locale-specific number/date/plural formatting is required today (the strings are simple labels).
- Translation *completeness* across languages is a quality goal enforced by tests, not a runtime behavior — an
  untranslated key simply falls back to English.

## 5. Reflected in code

- `src/main/resources/messages/MyBundle.properties` — English (base) strings.
- `src/main/resources/messages/MyBundle_fr.properties` — French strings.
- `src/main/kotlin/.../MyBundle.kt` — the bundle accessor used everywhere UI text is needed.
- Tests: `src/test/kotlin/.../i18n/I18nResourceBundleTest.kt` (locales load; keys stay in sync),
  `.../i18n/PluginXmlI18nTest.kt` (manifest strings are localized).

## 6. Related decisions

- **Realized by**: [ADR-0004 — Internationalization implementation](../adrs/adr-0004-internationalization-implementation.md).
- **See also**: [SPEC-0003 — Settings & scopes](spec-0003-settings-and-scopes.md) (the UI that consumes these
  strings).

## 7. Change history

- 2025-10-07 — Internationalization introduced with English and French.
