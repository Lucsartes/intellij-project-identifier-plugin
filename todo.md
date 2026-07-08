I pulled both the template's known-good set (guaranteed to work together — exactly your
point) and the absolute latest of each. Here's the full picture as of July 2026.

Version comparison

┌───────────────────────┬──────────────────┬─────────────────┬────────────────────────┐
│      Dependency       │   Your project   │    Template     │    Absolute latest     │
│                       │                  │  (known-good)   │                        │
├───────────────────────┼──────────────────┼─────────────────┼────────────────────────┤
│ IntelliJ Platform     │ 2024.3.6 ·       │ 2025.2.6.2      │ 2026.1.4 (2026.2 EAP)  │
│ (IC)                  │ sinceBuild 243   │                 │                        │
├───────────────────────┼──────────────────┼─────────────────┼────────────────────────┤
│ IntelliJ Platform     │ 2.10.5           │ 2.16.0          │ 2.17.0                 │
│ Gradle Plugin         │                  │                 │                        │
├───────────────────────┼──────────────────┼─────────────────┼────────────────────────┤
│ Gradle                │ 9.0.0            │ 9.5.0           │ 9.6.1                  │
├───────────────────────┼──────────────────┼─────────────────┼────────────────────────┤
│ Kotlin                │ 2.2.21           │ 2.1.20          │ 2.4.0                  │
├───────────────────────┼──────────────────┼─────────────────┼────────────────────────┤
│ Kover                 │ 0.9.3            │ (removed)       │ 0.9.8                  │
├───────────────────────┼──────────────────┼─────────────────┼────────────────────────┤
│ Changelog plugin      │ 2.5.0            │ 2.5.0           │ 2.5.0                  │
├───────────────────────┼──────────────────┼─────────────────┼────────────────────────┤
│ Qodana plugin         │ 2025.2.2         │ (removed)       │ dependabot-tracked     │
├───────────────────────┼──────────────────┼─────────────────┼────────────────────────┤
│ JUnit                 │ 4.13.2           │ 4.13.2          │ (JUnit 5 exists, not   │
│                       │                  │                 │ adopted)               │
├───────────────────────┼──────────────────┼─────────────────┼────────────────────────┤
│ foojay-resolver       │ 1.0.0            │ 1.0.0           │ 1.0.0                  │
├───────────────────────┼──────────────────┼─────────────────┼────────────────────────┤
│ JVM toolchain         │ 21               │ 21 (JBR)        │ —                      │
└───────────────────────┴──────────────────┴─────────────────┴────────────────────────┘

The important nuances (this is where "latest" is a trap)

1. Don't target the newest IDE — target the template's. Latest IntelliJ is 2026.1.4, but
   JetBrains' own template deliberately pins 2025.2.6.2. Plugin authors target a well-baked
   platform, because sinceBuild sets your minimum supported IDE. Matching the template (→
   sinceBuild 252) still covers 2025.2 → 2026.1 users; jumping to 2026.1 (sinceBuild 261) would
   strand everyone on 2025.x for no real gain.
2. Kotlin tracks the platform, not the changelog. Latest Kotlin is 2.4.0, but the template
   pairs platform 2025.2 with Kotlin 2.1.20 — because the IDE bundles a specific Kotlin runtime
   and you avoid stdlib mismatches by staying close to it. Your 2.2.21 is already ahead of the
   template and fine; do not chase 2.4.0.
3. JUnit 5 is not pushed by JetBrains — I need to correct my earlier framing. The template
   still ships JUnit 4.13.2, so migrating to JUnit 5 is purely optional modernization with zero
   compatibility pressure. I'd now deprioritize it.
4. The template dropped Kover and Qodana from the build entirely (they're CI-only now). You
   can keep yours — just know they're no longer "template-standard."
5. The template restructured (optional to adopt): platform version moved out of
   gradle.properties into build.gradle.kts via a typed intellijIdea("2025.2.6.2") accessor;
   plugin versions moved to settings.gradle.kts pluginManagement; gradle.properties slimmed
   down; the libs.versions.toml catalog is gone. You don't need to adopt this — your
   version-catalog setup is cleaner and the old create("IC", platformVersion) API still works.
   Just bump the numbers.

Recommended target (template-aligned, known-good)

Keeping your current structure, bump only the numbers:
- platformVersion 2024.3.6 → 2025.2.6 · pluginSinceBuild 243 → 252
- intelliJPlatform (gradle plugin) 2.10.5 → 2.16.0
- gradleVersion 9.0.0 → 9.5.0 (+ wrapper)
- kover 0.9.3 → 0.9.8
- Kotlin: keep 2.2.21; JUnit/changelog/foojay/toolchain: no change

The one real risk in this set is the platform bump touching the internal IdeBackgroundUtil
background API (the exact thing ADR-0001 flags) — so it needs a runIde smoke test, not just
green unit tests.

Sources

- IntelliJ IDEA 2026.1.4 (JetBrains blog) · template repo (intellijIdea("2025.2.6.2"),
  Kotlin 2.1.20, Gradle 9.5.0, JUnit 4.13.2)
- Kotlin 2.4.0 released
- IntelliJ Platform Gradle Plugin (plugins.gradle.org) — 2.17.0
- Gradle 9.6.1 release notes
- Kover 0.9.8 (Maven Repository)