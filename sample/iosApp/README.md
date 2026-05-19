# iOS sample

A thin SwiftUI host that embeds the shared Compose UI from `:sample:composeApp`.

## Run it

1. Build the shared framework at least once:
   ```
   ./gradlew :sample:composeApp:embedAndSignAppleFrameworkForXcode
   ```
2. Open `iosApp.xcodeproj` in Xcode.
3. Set your signing **Team** in `Configuration/Config.xcconfig` (`TEAM_ID=...`), or pick a team in
   Xcode's *Signing & Capabilities* tab.
4. Select an iOS Simulator and press **Run**.

The *Compile Kotlin Framework* build phase re-invokes Gradle on every build, so changes to the
Kotlin/Compose code are picked up automatically. Compose entry point: `MainViewController()` in
`composeApp/src/iosMain/.../MainViewController.kt`, surfaced to Swift as `MainViewControllerKt`.

No LLM SDK is wired up in this sample — a coroutine emits pre-canned markdown responses into the
`StreamingTypewriter` so the live markdown + syntax-highlighted code-block reveal works end-to-end
without any external API.
