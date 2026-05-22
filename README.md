# NotePilot 📓



---

**NotePilot** is an Android application that helps you capture, organize, and sync your notes effortlessly. Built with modern Android Jetpack components, it offers a clean UI, offline support, and seamless cloud backup.

## ✨ Features
- 🖊️ Create, edit, and delete notes quickly.
- 📂 Organize notes with tags and folders.
- 🌐 Sync across devices via Google Drive.
- 🔒 Secure local encryption for privacy.
- 📱 Optimized for mobile: lightweight APK (~12 MB) for fast downloads.

## 📦 Download & Install (Mobile)
You can download the latest signed APK directly from the GitHub Releases page, which works on any Android device:

1. Visit the [latest release](https://github.com/LikhithBellamkonda/NotePilot/releases/latest).
2. Download `NotePilot-release.apk`.
3. On your device, enable **Install unknown apps** for your browser or file manager.
4. Open the APK to install and enjoy NotePilot instantly!

> **Tip:** The APK is signed with a debug keystore for testing. For production, generate a release keystore and upload a new APK.

## 🛠️ Build & Run Locally
### Prerequisites
- **Android Studio** (latest stable version)
- **Java 17**
- A **GitHub** account (optional for cloning)

### Steps
1. Clone the repository:
   ```bash
   git clone https://github.com/LikhithBellamkonda/NotePilot.git
   cd NotePilot
   ```
2. Open the project in Android Studio (`File > Open`).
3. Let Android Studio sync Gradle and resolve dependencies.
4. Connect an Android device or start an emulator.
5. Run the app (`Run > Run 'app'`).

### Generate a Release APK
```bash
./gradlew assembleRelease
```
The APK will be located at `app/build/outputs/apk/release/app-release.apk`. Upload this file to a new GitHub release to make it publicly downloadable.

## 🤝 Contributing
Contributions are welcome! Please fork the repo, create a feature branch, and submit a pull request. Ensure code follows the existing style and include tests where applicable.

## 📄 License
This project is licensed under the **Apache License 2.0** – see the [LICENSE](LICENSE) file for details.

---

*Generated with ❤️ by the NotePilot team*
