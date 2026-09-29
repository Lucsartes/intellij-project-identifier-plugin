# ADR-0004: Internationalization with IntelliJ resource bundles

* **Status**: Accepted
* **Decided**: 2025-10-07
* **Last updated**: 2026-09-24

## 1. Context

[SPEC-0005](../specs/spec-0005-internationalization.md) requires the plugin's UI to follow the IDE language,
fall back to English, and let a new language be added without code changes. The question is which localization
mechanism to use.

## 2. Decision drivers

* **Platform consistency** — follow IntelliJ's own i18n conventions (locale detection, fallback).
* **Extensibility** — a new language should be a new file, not a code change.
* **Maintainability** — changing a translation must not touch UI logic.
* **Testability** — missing translations must be caught automatically.

## 3. Considered options

* **A — Hard-coded English.** Simplest, but excludes non-English users, and every translation becomes a code
  change.
* **B — IntelliJ `DynamicBundle` resource bundles.** Keys in code, one `.properties` file per language,
  automatic locale detection and English fallback, all handled by the platform.
* **C — An external translation service.** Remote updates, but adds a network dependency and offline fragility
  for no real gain in a small plugin.

## 4. Decision

Adopt **option B**.

* A single bundle, `messages.MyBundle`, holds every UI string: `MyBundle.properties` is English (the base and
  fallback), and `MyBundle_<lang>.properties` holds a translation. Keys are checked at author time through
  the IDE's `@PropertyKey` support.
* UI code never contains user-facing literals. It always reads the bundle, and the settings page titles are
  declared in `plugin.xml` as bundle keys.
* Adding a language means copying the base file, translating the values, and extending the i18n tests. No code
  changes.
* **Not localized:** the plugin name, and the Marketplace / Plugins-list description, which the build takes
  from `README.md` (English). Localizing the description would mean keeping a second long text in sync in every
  language, while the Marketplace listing itself is English anyway.

## 5. Consequences

* **Positive** — standard platform behavior (locale detection, fallback), drop-in languages, and a clean split
  between text and logic.
* **Negative** — every locale file must keep the same keys, and `.properties` files are less type-safe than
  code. Both are mitigated by `@PropertyKey` and by a unit test that fails when a translation is missing a key.

## 6. Code pointers

* `src/main/resources/messages/` — the bundle files.
* `src/test/kotlin/.../i18n/` — the key-parity and manifest tests.

## 7. Related

* **Serves**: [SPEC-0005 — Internationalization](../specs/spec-0005-internationalization.md).
* **Related ADRs**: [ADR-0002](adr-0002-hexagonal-architecture.md) (the core stays locale-agnostic; text
  lives in adapters and resources), [ADR-0003](adr-0003-settings-implementation.md) (the settings UI that uses
  these strings).
