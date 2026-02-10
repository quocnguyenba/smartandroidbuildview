# Android + Build Files View Plugin

[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)
[![Build](https://github.com/quocnguyenba/smartandroidbuildview/actions/workflows/build.yml/badge.svg)](https://github.com/quocnguyenba/smartandroidbuildview/actions/workflows/build.yml)

An IntelliJ IDEA / Android Studio plugin that adds a new **"Android + Build"** project view, combining the familiar Android view structure with immediate access to build configuration files under each module.

## Features

- **Custom Project View**: A new "Android + Build" option in the project view dropdown
- **Android-Style Structure**: Shows modules hierarchically with familiar Android folders (kotlin+java, res, assets, manifests)
- **Build Files Visible**: Display `build.gradle.kts`, `proguard-rules.pro`, and `consumer-rules.pro` directly under each module
- **No More Scrolling**: Access module build files without navigating to the separate "Gradle Scripts" section
- **Smart Module Detection**: Automatically detects Android app and library modules with appropriate icons
- **Customizable Visibility**: Settings to show/hide different file types (manifests, build config, etc.)

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

## Contributing

Contributions are welcome! Please feel free to submit a Pull Request. For major changes, please open an issue first to discuss what you would like to change.

## License

```
Copyright 2026 Quoc Nguyen

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

See the [LICENSE](LICENSE) file for details.
