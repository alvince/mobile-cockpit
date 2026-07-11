# Repository Guidelines

This guide covers how to build, test, and contribute to the Cockpit desktop project.

## Project Structure

```
cockpit/
├── composeApp/                 # Desktop application module
│   ├── src/commonMain/         # Shared UI, ViewModel, DI, theme
│   └── src/desktopMain/        # Desktop entry point and platform-specific code
├── build.gradle.kts            # Root build script
├── settings.gradle.kts         # Project settings
├── gradle.properties           # Gradle and Kotlin configuration
├── gradle/libs.versions.toml   # Version catalog (inside gradle/)
└── README.md                   # Project overview in Chinese
```

Source code is organized by Kotlin Multiplatform source sets: common shared code lives in `commonMain`, and JVM desktop-specific code lives in `desktopMain`.

## Build, Test, and Development Commands

Requires JDK 21 or newer. On macOS with Homebrew:

```bash
export JAVA_HOME=/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home
```

Common Gradle tasks:

- `./gradlew :composeApp:run` — Run the desktop application locally.
- `./gradlew :composeApp:packageDmg` — Build a macOS DMG installer.
- `./gradlew :composeApp:packageMsi` — Build a Windows MSI installer.
- `./gradlew :composeApp:packageExe` — Build a Windows EXE installer.
- `./gradlew build` — Build and test the whole project.

## Coding Style and Naming Conventions

- Kotlin official code style is enforced (`kotlin.code.style=official`).
- Indent with four spaces.
- Use `PascalCase` for classes and composables, `camelCase` for functions and properties, and `UPPER_SNAKE_CASE` for constants.
- Composables that take parameters should be named with a noun or noun phrase describing the UI.
- Place shared UI, theme, and DI modules under `composeApp/src/commonMain/kotlin/com/alvincezy/dev/cockpit/`.
- Place desktop-specific code under `composeApp/src/desktopMain/kotlin/com/alvincezy/dev/cockpit/`.

## Testing Guidelines

- Test framework: Kotlin Test (JUnit-based runner) via Gradle.
- Add tests next to the source under `commonTest/` or `desktopTest/` source sets.
- Run all tests with `./gradlew test`.
- Aim for meaningful coverage on ViewModels, utilities, and platform-specific logic rather than pure UI composables.

## Commit and Pull Request Guidelines

The repository currently has no commit history. Going forward, use these conventions:

- Write concise, imperative commit messages (e.g., `Add device discovery panel`).
- Keep commits focused on a single change.
- Open pull requests with a clear description of the change, the motivation, and any related issues.
- Include screenshots or recordings for UI changes.
- Ensure `./gradlew build` passes before requesting review.

## Security and Configuration Tips

- Never commit signing keys, certificates, or local JVM paths.
- Keep `gradle.properties` free of machine-specific values.
- The macOS bundle ID is `com.alvincezy.dev.cockpit`; verify it before publishing signed packages.
