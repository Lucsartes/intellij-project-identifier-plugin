# ADR-0004: Internationalization implementation

> Technical decision. Serves [SPEC-0005 — Internationalization](../specs/spec-0005-internationalization.md).
> Living document — keep in sync with the code (see [`docs/README.md`](../README.md)).

* **Status**: Accepted
* **Last updated**: 2026-07-08 (originally decided 2025-10-07)
* **Authors**: Project maintainers

## 1. Context

[SPEC-0005](../specs/spec-0005-internationalization.md) requires the plugin UI to appear in the user's IDE
language with an English fallback, and new languages to be addable without code changes. The technical question
is which localization mechanism to use.

## 2. Decision drivers

* **Maintainability** — adding/updating a translation must not touch business or UI logic.
* **Extensibility** — a new language should be a drop-in file.
* **Platform consistency** — follow IntelliJ Platform i18n conventions (locale detection, fallback).
* **Testability** — translation completeness/correctness must be verifiable.

## 3. Considered options

* **A — Hardcoded English strings.** Simplest, but excludes non-English users and forces code changes for every
  translation. Rejected.
* **B — Resource bundles via IntelliJ `DynamicBundle`.** Standard JVM/IntelliJ i18n: keys in code,
  translations in per-locale `.properties` files, automatic locale detection and English fallback. A new
  language is a new `.properties` file. Testable.
* **C — External translation service/database.** Enables remote updates but adds network dependency, latency,
  and offline fragility — massive overkill for a single plugin. Rejected.

## 4. Decision

Adopt **Option B**: IntelliJ's `DynamicBundle`.

* `MyBundle` extends `DynamicBundle("messages.MyBundle")` and exposes
  `message(key, vararg params)`, with `@PropertyKey(resourceBundle = "messages.MyBundle")` so the IDE validates
  keys at author time.
* Strings live in `src/main/resources/messages/`: `MyBundle.properties` (English, the base/fallback) and
  `MyBundle_fr.properties` (French). The plugin manifest declares `<resource-bundle>messages.MyBundle</resource-bundle>`,
  and configurables reference bundle keys for their titles.
* Keys use a hierarchical dot-notation (`settings.identifier.override.label`,
  `settings.identifier.override.tooltip.description`, …). Parameterized values use `{0}`-style placeholders
  (e.g. `default.annotated.value`).
* Adding a language = copy `MyBundle.properties` to `MyBundle_<lang>.properties`, translate the values, extend
  the i18n tests. No code change.

## 5. Consequences

* **Positive**: accessible to non-English users; new languages are drop-in; clean separation of text from
  logic; automatic locale detection and fallback; testable completeness.
* **Negative**: keys must be kept in sync across locale files; `.properties` are less type-safe than code
  (mitigated by `@PropertyKey` and tests); discipline needed to externalize every user-facing string.
* **Neutral**: translation upkeep is ongoing as the UI evolves; bundle loading is at runtime (negligible cost).

## 6. Reflected in code

- `src/main/kotlin/.../MyBundle.kt` — `DynamicBundle` accessor with `@PropertyKey` validation.
- `src/main/resources/messages/MyBundle.properties` — English (base) strings.
- `src/main/resources/messages/MyBundle_fr.properties` — French strings.
- `src/main/resources/META-INF/plugin.xml` — `<resource-bundle>` declaration; configurables pass bundle keys.
- Consumers: `IntelliJSettingsConfigurable` / `IntelliJApplicationSettingsConfigurable` call
  `MyBundle.message(...)` for every label, hint and tooltip.
- Tests: `src/test/kotlin/.../i18n/I18nResourceBundleTest.kt` (all locales load; key sets stay in sync),
  `.../i18n/PluginXmlI18nTest.kt` (manifest-referenced strings are localized).

## 7. Related documents

- **Serves**: [SPEC-0005 — Internationalization](../specs/spec-0005-internationalization.md).
- **Related ADRs**: [ADR-0002](adr-0002-hexagonal-architecture.md) (core stays locale-agnostic; text lives in
  adapters/resources), [ADR-0003](adr-0003-settings-implementation.md) (the settings UI consumes these
  strings), [ADR-0005](adr-0005-branch-placeholder-implementation.md) (added the override tooltip keys).

## 8. Future evolution

Likely additions: more languages (community-contributed), CI checks for key-set parity and placeholder-count
consistency, and locale-aware formatting via `MessageFormat` if richer messages appear. Any change must
preserve the code/translation separation and backward compatibility with existing language files.
