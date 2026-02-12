# Android + Build View Settings Guide

## Overview
The Android + Build View now includes a comprehensive settings panel that allows you to customize exactly what appears in your project view.

## Accessing Settings

### Quick Access (Recommended) 🚀
1. Switch to **Android + Build** view in the Project tool window
2. Look for the **gear icon (⚙️)** in the Project View toolbar (top-right area)
3. Click it to open the settings popup
4. Toggle any options you want
5. Close the popup - changes apply automatically!

### Via Settings Menu
1. Open **Settings/Preferences** (⌘, on Mac / Ctrl+Alt+S on Windows/Linux)
2. Navigate to **Tools → Android + Build View**
3. Check/uncheck options to customize your view
4. Click **Apply** to see changes immediately

## Available Settings

### Source Folders
- **Show kotlin+java sources** - Display main Kotlin and Java source directories (src/main/java, src/main/kotlin)
- **Show res folder** - Display Android resource directories (src/main/res)
- **Show assets folder** - Display Android assets directories (src/main/assets)
- **Show test sources** - Display unit test source directories (src/test/java, src/test/kotlin)
- **Show androidTest sources** - Display instrumented test source directories (src/androidTest/java, src/androidTest/kotlin)

### Configuration Files
- **Show AndroidManifest.xml** - Display Android manifest files
- **Show build.gradle files** - Display Gradle build scripts (build.gradle / build.gradle.kts)
- **Show proguard-rules.pro** - Display ProGuard configuration files
- **Show consumer-rules.pro** - Display consumer ProGuard rules (for libraries)

### Generated Content
- **Show BuildConfig.java** - Display generated BuildConfig files
- **Show generated folders** - Display all build-related directories (build, .gradle, .idea)

### Other
- **Show other files and folders** - Display additional files and directories not in standard categories
- **Show external libraries** - Display external dependencies and libraries

## Default Settings

By default, the following are **enabled**:
- kotlin+java sources
- res folder
- assets folder
- AndroidManifest.xml
- build.gradle files
- proguard-rules.pro
- consumer-rules.pro
- BuildConfig.java

By default, the following are **disabled** (to keep the view clean):
- test sources
- androidTest sources
- generated folders
- other files and folders
- external libraries

## Tips

1. **Clean View**: Keep test sources and generated folders disabled for a cleaner, Android-view-like structure
2. **Development**: Enable test sources when actively working on tests
3. **Debugging**: Enable generated folders when you need to inspect generated code
4. **Full View**: Enable all options for a complete project overview similar to the Project view

## Live Updates

Changes are applied immediately when you click **Apply**. The project view will refresh automatically to show your updated preferences.
