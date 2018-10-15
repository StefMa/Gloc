# Kt2ts
A [Gradle Plugin](https://docs.gradle.org/current/userguide/custom_plugins.html) which translates Kotlin data classes into Typescript.

> Built with TDD approach demonstrated by Stefan May's in [Gloc](https://github.com/StefMa/Gloc) Gradle Plugin.

## How to use
One is able to use/apply the plugin like this:
```kotlin
// settings.gradle.kts
pluginManagement {
  repositories {
    gradlePluginPortal()
    maven { url = uri("https://jitpack.io") }
  }
}

// build.gradle.kts
plugins {
  id("kotlin2ts") version "1.0.0"
}

kt2ts {
  enabled = true
  packs = arrayOf("org.fidget.models")
}
```

## Extension configuration
The extension provides only one property:
* packs - a array of packages where data models to be converted into typescript are situated

## Task
The plugin provides the task called **kt2ts**.