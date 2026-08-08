# Recent Tabs ToolWindow

A JetBrains IDE plugin that adds a dedicated tool window listing your recently activated
editor tabs. The list updates automatically as you switch between files — a persistent,
always-visible alternative to the built-in `Recent Files` popup.

## Features

- Sorted by last activation, most recent on top
- File names with full path tooltips and file-type icons
- Single-click to open, or `Enter` on the selected row
- Keeps the last 50 files, including ones you have already closed
- Platform-only plugin — runs in every JetBrains IDE, no Java plugin dependency

## Requirements

- A JetBrains IDE build 262 or newer (2026.2+)

## Installation

Install from the plugin ZIP: **Settings → Plugins → ⚙ → Install Plugin from Disk…** and
pick `build/distributions/RecentFilesToolWindow-<version>.zip`.

Open the tool window via **View → Tool Windows → Recent Tabs**.

## Building

```bash
./gradlew buildPlugin      # produces build/distributions/RecentFilesToolWindow-<version>.zip
./gradlew runIde           # launches a sandbox IDE with the plugin installed
./gradlew verifyPlugin     # runs the JetBrains Plugin Verifier
```

## Publishing

Signing and publishing read their secrets from the environment, so nothing sensitive
lives in the repository:

| Variable                 | Purpose                                            |
|--------------------------|----------------------------------------------------|
| `CERTIFICATE_CHAIN_FILE` | Path to the signing certificate chain (`chain.crt`) |
| `PRIVATE_KEY_FILE`       | Path to the private key (`private.pem`)             |
| `PRIVATE_KEY_PASSWORD`   | Password for the private key                        |
| `PUBLISH_TOKEN`          | JetBrains Marketplace permanent token               |

```bash
./gradlew signPlugin
./gradlew publishPlugin
```

See [Plugin Signing](https://plugins.jetbrains.com/docs/intellij/plugin-signing.html) for
how to generate the key pair. The **first** upload of a new plugin has to go through the
[Marketplace upload form](https://plugins.jetbrains.com/plugin/add); `publishPlugin` only
works for subsequent updates.

## License

[MIT](LICENSE) © Dennis Drochmann
