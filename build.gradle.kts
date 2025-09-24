plugins {
    id("java")
    id("org.jetbrains.intellij") version "1.17.3"
}

group = "com.dennis"
version = "1.0"

repositories {
    mavenCentral()
}

intellij {
    version.set("2025.2")
    type.set("IU")
    plugins.set(listOf("com.intellij.java"))
}

tasks {
    patchPluginXml {
        sinceBuild.set("252")
        untilBuild.set("252.*")
    }
    withType<JavaCompile> {
        sourceCompatibility = "17"
        targetCompatibility = "17"
    }
}

tasks.named("buildSearchableOptions").configure {
    enabled = false
}
