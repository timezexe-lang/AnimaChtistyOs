ANIMA CHRISTI OS — ANDROID PROJECT

Your supplied complete web edition is bundled at:
app/src/main/assets/index.html

The original red-and-black interface is preserved. The Android wrapper uses a WebView
and a limited native bridge for platform information, opening Android Settings,
launching the Termux app, and showing native messages.

Important: opening Termux is not the same as connecting its shell to the web terminal.
This app does not replace Android, bypass Android security, or provide unrestricted
system access. Native features require Android APIs and user-granted permissions.

BUILD:
1. Upload this entire project folder to a GitHub repository.
2. Open the repository's Actions tab.
3. Run the included Android APK build workflow (or push a commit if configured).
4. Download the generated debug APK from the workflow run's Artifacts section.

Acode edits the source files; it does not compile the APK by itself.
