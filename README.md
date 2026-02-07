# k18n
k18n is a simple internationalization gradle plugin that generates Kotlin code from .json files. It supports multiple languages and allows you to easily access your translations in your code.

It was developed for my Discord Bot [Emolga](https://github.com/TecToast/Emolga), but it can be used in any Kotlin Gradle project.

## Usage
To use k18n, add the following to your build.gradle.kts file:

```kotlin
plugins {
    id("de.tectoast.k18n") version "2.0"
}
```
Since this plugin is not published to the Gradle Plugin Portal, you need to add the following to your settings.gradle.kts file:

```kotlin
pluginManagement {
    repositories {
        maven("https://maven.tectoast.de/releases")
        // your other plugin repositories, e.g. gradlePluginPortal()
    }
}
```
In the default configuration, k18n looks for a `k18n` directory in the main project directory. You can change this by configuring the plugin.

You also need to add the generated source directory to your source sets:
```kotlin
kotlin {
    sourceSets {
        main {
            kotlin.srcDir(project.layout.buildDirectory.dir("generated/k18n"))
        }
    }
}
```