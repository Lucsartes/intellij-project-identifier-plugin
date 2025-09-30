# intellij-project-identifier-plugin

Disclaimer: this plugin was vibe coded by Jetbrains Junie coding assistant.

<!-- Plugin description -->
Project Identifier helps you tell multiple IDE projects apart by adding a subtle, low-opacity text watermark to the editor background.

## 🌟 What It Does

The IntelliJ Project Identifier plugin solves a simple problem for developers who work with many projects at once: telling them apart at a glance.

When you open a project, this plugin automatically adds a unique text identifier—derived from the project's name—as a subtle, low-opacity background watermark. This allows you to quickly distinguish between different IDE windows when using a task switcher like the Windows key or `Alt+Tab`.

## ⚙️ How It Works

The plugin automates the process of creating and applying a visual identifier. It works by:

1.  Listening for project open events.
2.  Taking the project's name and applying your chosen rules (e.g., first letter of each word) to generate a short text string.
3.  Programmatically creating a transparent PNG image with this text and saving it to a local directory.
4.  Using the IDE's internal API to set this newly created image as the editor background for the current project.

Any changes to the plugin's settings will trigger this process again, creating a new image and updating the background automatically. The generated image file is stored locally, but you are free to change its location and manually edit the background image setting at any time.

## 📋 Plugin Settings

The plugin provides a simple configuration panel to customize the identifier's behavior. You can access it via **File | Settings | Appearance & Behavior | Appearance**.

<!-- Plugin description end -->

## Installation

- Using the IDE built-in plugin system:
  
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>Marketplace</kbd> > <kbd>Search for "intellij-project-identifier-plugin"</kbd> >
  <kbd>Install</kbd>
  
- Using JetBrains Marketplace:

  Go to [JetBrains Marketplace](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID) and install it by clicking the <kbd>Install to ...</kbd> button in case your IDE is running.

  You can also download the [latest release](https://plugins.jetbrains.com/plugin/MARKETPLACE_ID/versions) from JetBrains Marketplace and install it manually using
  <kbd>Settings/Preferences</kbd> > <kbd>Plugins</kbd> > <kbd>⚙️</kbd> > <kbd>Install plugin from disk...</kbd>

---
Plugin based on the [IntelliJ Platform Plugin Template][template].

[template]: https://github.com/JetBrains/intellij-platform-plugin-template
[docs:plugin-description]: https://plugins.jetbrains.com/docs/intellij/plugin-user-experience.html#plugin-description-and-presentation
