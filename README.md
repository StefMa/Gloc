# Gloc
A [Gradle Plugin](https://docs.gradle.org/current/userguide/custom_plugins.html) which counts the lines of code in given directories.

> **Droidcon Italy 18:**<br>
I gave a talk at the [Droidcon Italy 18](http://it.droidcon.com/2018/talks/269/) in Turin where I used that Plugin as an example to demonstrate how to write a Gradle Plugin.
Slides of my Talk are available at [Speaker Deck](https://speakerdeck.com/stefma/how-to-write-gradle-plugins-in-kotlin).

## How to use
As the Plugin isn't published anywhere you have to do so first. For *"manual testing"* you can simply push it to your local maven repo.
Just call:
```
./gradlew clean build publishToMavenLocal
```
After that you are able to use/apply that plugin (even in this project) like this:
```kotlin
// settings.gradle.kts
pluginManagement {
  repositories {
    gradlePluginPortal()
    mavenLocal()
  }
}

// build.gradke.kts
plugins {
  id("guru.stefma.gloc") version "0.0.1"
}

gloc {
  enabled = true
  dirs = arrayOf("src/main", "src/test")
}
```

## Extension configuration
The extension provide two properties:
* enabled - which will enable or disable the plugin
* dirs - a array of file pathes which defines where to count the lines of code

## Task
The plugin provides the task called **gloc**.<br>
If you can run it for the first time it will - well - run and **SUCCESS**. Meaning the task was executed.

If you run it for the second time - without cleaning the `buid/` dir and changing the input (the `dirs` property) - it will be **UP-TO-DATE**.

Running it with the [**Gradle build cache**](https://docs.gradle.org/current/userguide/build_cache.html) enabled it wil be - if you run it the second time - use the output from the cache. 
The output should be **FROM-CACHE**
