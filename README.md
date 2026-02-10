# Android + Build Files View Plugin

An IntelliJ IDEA / Android Studio plugin that adds a new **"Android + Build"** project view, combining the familiar Android view structure with immediate access to build configuration files under each module.

## Features

- **Custom Project View**: A new "Android + Build" option in the project view dropdown
- **Android-Style Structure**: Shows modules hierarchically with familiar Android folders (kotlin+java, res, assets, manifests)
- **Build Files Visible**: Display `build.gradle.kts`, `proguard-rules.pro`, and `consumer-rules.pro` directly under each module
- **No More Scrolling**: Access module build files without navigating to the separate "Gradle Scripts" section
- **Smart Module Detection**: Automatically detects Android app and library modules with appropriate icons
- **Customizable Visibility**: Settings to show/hide different file types (manifests, build config, etc.)
- **Tool Window**: Browse all modules and their configuration files in a dedicated tool window

## Installation

### From Source

1. Clone this repository
2. Open the project in IntelliJ IDEA
3. Run `./gradlew buildPlugin`
4. Install the plugin from `build/distributions/Android + Build Files View-1.0.0.zip`

### From JetBrains Marketplace

Coming soon...

## Usage

### Switching to Android + Build View

1. Open the Project tool window (usually on the left side)
2. Click the dropdown at the top (where it shows "Android", "Project", etc.)
3. Select **"Android + Build"**
4. Your project structure will now show build files under each module

### What You'll See

Each module displays:
- **kotlin+java** - Source folders containing Kotlin and Java code
- **res** - Android resource directories
- **assets** - Android asset directories (if present)
- **AndroidManifest.xml** - Module manifest file
- **build.gradle.kts** (or `build.gradle`) - Build configuration
- **proguard-rules.pro** - ProGuard rules (if present)
- **consumer-rules.pro** - Consumer rules for libraries (if present)
- **BuildConfig.java** - Generated build configuration (if enabled)

At the bottom of the view, you'll find a **Gradle Scripts** section with root project files like `settings.gradle.kts` and project-level `build.gradle.kts`.

### Tool Window

1. Look for the "Module Files" tool window (typically on the right sidebar)
2. Browse all modules and their configuration files in a tree structure
3. Double-click any file to open it

## Development

### Building

```bash
# Build the plugin
./gradlew buildPlugin

# Run the plugin in a sandbox IDE
./gradlew runIde

# Verify plugin compatibility
./gradlew verifyPlugin
```

### Project Structure

- `AndroidBuildViewPane.kt` - Custom project view pane implementation
- `AndroidBuildTreeStructureProvider.kt` - Tree structure provider for organizing modules hierarchically
- `AndroidBuildTreeNodes.kt` - Custom tree nodes for modules, files, and folders with smart sorting
- `ModuleBuildFilesToolWindow.kt` - Tool window for browsing module configuration files
- `ModuleFilesSettings.kt` - Persistent settings for file visibility customization

## Requirements

- IntelliJ IDEA 2024.1 or later (build 241+)
- Android Studio Jellyfish or later (for Android development)
- Java 17 or later

## Why This Plugin?

In standard Android Studio, build configuration files are separated from modules in the "Gradle Scripts" section, requiring constant scrolling and context switching. The Android view hides these files for a cleaner look, but makes them harder to access.

This plugin solves that by:
- Keeping the clean Android view structure you love
- Making build files immediately accessible under each module
- Eliminating the need to scroll to "Gradle Scripts"
- Maintaining proper hierarchical organization for multi-module projects

## License

MIT License
