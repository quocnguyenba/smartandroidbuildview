# Module Files Quick Access Plugin

An IntelliJ IDEA / Android Studio plugin that provides quick access to module configuration files (`build.gradle`, `proguard-rules.pro`, `consumer-rules.pro`) directly from each module in the Project view.

## Features

- **Context Menu Integration**: Right-click on any module folder to access its configuration files
- **Keyboard Shortcut**: Press `Ctrl+Alt+M` to show a popup with module files
- **Tool Window**: A dedicated tool window showing all modules and their configuration files
- **Smart Detection**: Supports both Groovy (`.gradle`) and Kotlin DSL (`.gradle.kts`) build files

## Installation

### From Source

1. Clone this repository
2. Open the project in IntelliJ IDEA
3. Run `./gradlew buildPlugin`
4. Install the plugin from `build/distributions/module-files-quick-access-*.zip`

### From JetBrains Marketplace

Coming soon...

## Usage

### Context Menu

1. Right-click on a module folder in the Project view
2. Select "Module Files" from the context menu
3. Choose the file you want to open:
   - Open build.gradle
   - Open proguard-rules.pro
   - Open consumer-rules.pro
   - Open All Module Files

### Keyboard Shortcut

1. Select a file or folder within a module
2. Press `Ctrl+Alt+M` (or `Cmd+Alt+M` on macOS)
3. Select the file from the popup

### Tool Window

1. Open the "Module Files" tool window from the right sidebar
2. Browse all modules and their configuration files
3. Double-click to open any file

## Building

```bash
# Build the plugin
./gradlew buildPlugin

# Run the plugin in a sandbox IDE
./gradlew runIde

# Verify plugin compatibility
./gradlew verifyPlugin
```

## Requirements

- IntelliJ IDEA 2023.3 or later
- Android Studio Iguana or later (for Android development)

## License

MIT License
