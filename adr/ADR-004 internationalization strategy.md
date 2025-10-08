# ADR-004: Internationalization Strategy

* **Status**: Accepted
* **Date**: 2025-10-07
* **Authors**: Project maintainers

## 1. Context

The plugin provides user-facing text in multiple areas: settings pages, tooltips, button labels, and plugin metadata. As the plugin's user base grows, we need to make these strings accessible to non-English speaking users.

We must decide how to manage localized text in a way that:
- Supports multiple languages without code changes
- Makes it easy to add new language translations in the future
- Follows IntelliJ Platform best practices
- Maintains code quality and testability

Currently, the plugin has users from different language regions, and we have identified French and English as the initial languages to support.

---

## 2. Decision Drivers

* **Maintainability**: Adding new languages or updating existing translations should not require changes to business logic or UI code.
* **Extensibility**: The solution must make it straightforward to add additional languages in the future without architectural changes.
* **User Experience**: Users should automatically see the plugin in their preferred language based on their IDE locale settings.
* **Platform Consistency**: The solution should follow IntelliJ Platform conventions for internationalization to ensure consistent behavior with other plugins and IDE features.
* **Code Quality**: Externalized strings make code more readable and testable by separating presentation concerns from logic.
* **Testing**: The i18n implementation should be testable to ensure translation completeness and correctness.

---

## 3. Considered Options

* **Option A: Hardcoded English Strings**
    * **Description**: Embed all user-facing text directly in Kotlin code as English string literals.
    * **Pros**:
        * Simplest initial implementation
        * No additional files or infrastructure needed
        * Direct and immediate to read in code
    * **Cons**:
        * Excludes non-English speaking users entirely
        * Adding any language support requires modifying every file containing user-facing strings
        * Violates separation of concerns (presentation text mixed with logic)
        * Poor maintainability: updating text requires code changes and recompilation
        * Cannot leverage IDE locale detection
        * No scalability path for multilingual support

* **Option B: Resource Bundle Files**
    * **Description**: Use Java/Kotlin ResourceBundle mechanism with separate `.properties` files for each language. Load strings dynamically based on user's locale.
    * **Pros**:
        * Standard i18n approach in the JVM ecosystem and IntelliJ Platform
        * Clean separation: code references keys, translations live in resource files
        * Adding a new language requires only creating a new properties file (e.g., `MyBundle_de.properties` for German)
        * Automatic locale detection and fallback handled by the platform
        * IntelliJ provides `DynamicBundle` class specifically for plugin i18n
        * Enables community contributions of translations without code knowledge
        * Testable: can verify translation completeness and correctness
        * Supports parameterized messages (e.g., "Selected {0} items")
    * **Cons**:
        * Slightly more initial setup (bundle infrastructure, properties files)
        * Requires maintaining key consistency across all language files
        * IDE support for property files is not as rich as for code (but IntelliJ provides @PropertyKey annotation for validation)

* **Option C: Centralized Translation Service/Database**
    * **Description**: Store all translations in an external service or database and fetch them at runtime.
    * **Pros**:
        * Could enable dynamic translation updates without plugin redeployment
        * Centralized management of translations across multiple plugins
    * **Cons**:
        * Massive complexity overhead for a single plugin
        * Requires network connectivity and external infrastructure
        * Performance implications (network latency, caching)
        * Poor offline experience
        * Overkill for plugin scope

---

## 4. Decision

We have decided to adopt **Option B: Resource Bundle Files** using the IntelliJ Platform's `DynamicBundle` class.

This decision is driven primarily by:
1. **Platform alignment**: IntelliJ provides explicit support for this pattern through `DynamicBundle` and related APIs
2. **Extensibility**: Adding French support today and Italian or German tomorrow requires zero code changes—only new `.properties` files
3. **Maintainability**: Translators can work independently in resource files without touching Kotlin code
4. **User Experience**: Automatic locale detection provides seamless experience for users worldwide

While Option A (hardcoded English) would be simpler initially, it fails the core requirement of supporting multiple languages and providing a good user experience for non-English speakers. Option C is unnecessarily complex for a single plugin's needs.

---

## 5. Implementation Details

### 5.1. Current Implementation

As of this ADR, we have implemented:

1. **Resource Bundle Files** (in `src/main/resources/messages/`):
   - `MyBundle.properties` — default (English) translations
   - `MyBundle_fr.properties` — French translations
   
2. **Bundle Access Object** (`MyBundle.kt`):
   ```kotlin
   object MyBundle : DynamicBundle("messages.MyBundle") {
       @JvmStatic
       fun message(@PropertyKey(resourceBundle = "messages.MyBundle") key: String, vararg params: Any): String
   }
   ```
   - Extends IntelliJ's `DynamicBundle` for automatic locale resolution
   - Uses `@PropertyKey` annotation to enable IDE validation of keys

3. **Usage Pattern**:
   ```kotlin
   // In UI code:
   val label = MyBundle.message("settings.title")
   val tooltip = MyBundle.message("default.annotated.value", "JetBrains Mono")
   ```

4. **Testing** (`I18nResourceBundleTest.kt`):
   - Validates that all supported locales load correctly
   - Verifies key-value pairs for correctness
   - Ensures translation completeness (all locales have same key sets)

### 5.2. Adding a New Language

To add support for a new language (e.g., German):

1. Create `src/main/resources/messages/MyBundle_de.properties`
2. Copy all keys from `MyBundle.properties`
3. Translate values to German
4. Add test cases to `I18nResourceBundleTest.kt` to verify the new locale
5. No code changes required

### 5.3. Key Naming Convention

Keys follow a hierarchical dot-notation pattern:
- `settings.title` — top-level settings page title
- `settings.identifier.override.label` — specific field label
- `settings.hint.tooltip.description` — nested tooltip content

This structure keeps keys organized and makes them easy to locate.

---

## 6. Consequences

* **Positive**:
    * Plugin is accessible to non-English speaking users
    * Current implementation supports English and French
    * Future language additions require only new properties files (no code changes)
    * Clean separation of concerns: logic in code, text in resources
    * Community can contribute translations without Kotlin knowledge
    * Testable i18n ensures translation quality and completeness
    * Automatic locale detection provides seamless UX
    * Follows IntelliJ Platform best practices and conventions

* **Negative**:
    * Initial setup overhead (bundle infrastructure, multiple files to maintain)
    * Must ensure all translations stay in sync when adding/removing keys
    * Property files are less type-safe than code (though @PropertyKey annotation helps)
    * Requires discipline to externalize all user-facing strings (easy to accidentally hardcode)

* **Neutral**:
    * Translation maintenance is ongoing work as the plugin evolves
    * Community translation contributions require review process
    * Bundle loading happens at runtime (minimal performance impact, but not compile-time)

---

## 7. Future Evolution

As the plugin grows, we anticipate:

1. **Additional Languages**: German, Spanish, Italian, Japanese, etc. can be added incrementally as community contributions or based on user demand.

2. **Translation Validation**: We may introduce automated checks (e.g., in CI/CD) to ensure:
   - All language files have identical key sets
   - No missing or extra keys in translations
   - Parameterized messages have correct placeholder counts

3. **Translation Contributors**: We may document a contribution guide for translators to make it easy for the community to submit new languages.

4. **Locale-Specific Formatting**: If needed, we can leverage `java.text.MessageFormat` for locale-aware number, date, or plural formatting.

Any future changes to the i18n strategy must:
- Preserve the clean separation between code and translation resources
- Maintain backward compatibility with existing language files
- Follow IntelliJ Platform conventions

---

## 8. Related Documents

* ADR-002 technical architecture.md — hexagonal architecture ensures core logic remains locale-agnostic
* ADR-003 settings page and scope.md — settings UI heavily relies on localized strings
* README.md — user documentation should mention language support
* `src/test/kotlin/.../i18n/I18nResourceBundleTest.kt` — i18n validation tests
