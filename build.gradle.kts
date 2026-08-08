import org.jetbrains.intellij.platform.gradle.IntelliJPlatformType

plugins {
    id("java")
    id("org.jetbrains.intellij.platform") version "2.18.1"
}

group = providers.gradleProperty("pluginGroup").get()
version = providers.gradleProperty("pluginVersion").get()

repositories {
    mavenCentral()

    intellijPlatform {
        defaultRepositories()
    }
}

dependencies {
    intellijPlatform {
        // Zielt auf die lokal installierte WebStorm-Version (Build WS-262.*)
        webstorm(providers.gradleProperty("platformVersion"))

        pluginVerifier()
        zipSigner()
    }
}

intellijPlatform {
    buildSearchableOptions = false

    pluginConfiguration {
        ideaVersion {
            // 2026.2 und neuer; kein until-build, damit kommende Releases nicht ausgesperrt werden
            sinceBuild = providers.gradleProperty("pluginSinceBuild")
            untilBuild = provider { null }
        }

        changeNotes = """
            <ul>
              <li><b>1.3.0</b>: Erster Marketplace-Release. Tool-Window-Subscription haengt jetzt am
                  Lifecycle des Tool Windows (kein Listener-Leak mehr), Hover- und Auswahlfarben nutzen
                  die Theme-API und funktionieren damit auch in dunklen Themes, Enter oeffnet die
                  markierte Datei, MIT-Lizenz.</li>
              <li><b>1.2</b>: Auf IntelliJ Platform 2026.2 aktualisiert (Gradle IntelliJ Platform Plugin 2.x).</li>
              <li><b>1.1</b>: ToolWindow mit Liste der zuletzt aktivierten Editor-Tabs.</li>
            </ul>
        """.trimIndent()
    }

    signing {
        certificateChainFile = layout.file(providers.environmentVariable("CERTIFICATE_CHAIN_FILE").map { file(it) })
        privateKeyFile = layout.file(providers.environmentVariable("PRIVATE_KEY_FILE").map { file(it) })
        password = providers.environmentVariable("PRIVATE_KEY_PASSWORD")
    }

    publishing {
        token = providers.environmentVariable("PUBLISH_TOKEN")
    }

    pluginVerification {
        ides {
            // Bewusst nur die lokal vorhandene Ziel-IDE, damit der Lauf ohne
            // GB-weise IDE-Downloads durchlaeuft. Der Marketplace verifiziert beim
            // Upload zusaetzlich gegen sein eigenes IDE-Set.
            create(IntelliJPlatformType.WebStorm, providers.gradleProperty("platformVersion"))
        }
    }
}

java {
    // IntelliJ Platform 2026.2 laeuft auf JBR 25
    sourceCompatibility = JavaVersion.VERSION_25
    targetCompatibility = JavaVersion.VERSION_25
}

tasks.withType<JavaCompile>().configureEach {
    options.release = 25
    options.encoding = "UTF-8"
}
