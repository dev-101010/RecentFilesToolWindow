plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.dennis"
version = "1.1"

repositories {
    mavenCentral()
}

// Use local Java 25 as toolchain, but compile targeting Java 23 bytecode (required by IntelliJ Platform 2025.3)
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

intellij {
    version.set("2025.3")
    type.set("IU")
    plugins.set(listOf("com.intellij.java"))
}

tasks {
    patchPluginXml {
        sinceBuild.set("253")
        untilBuild.set("253.*")
    }
    withType<JavaCompile> {
        sourceCompatibility = "23"
        targetCompatibility = "23"
        options.release.set(23)
    }
}

tasks.named("buildSearchableOptions").configure {
    enabled = false
}
