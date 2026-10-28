<div align="center">

# NotePilot 

**An intelligent, on-device AI vault and bookmarking companion.**

</div>

## Overview

NotePilot is your personal AI-powered vault designed to help you save, categorize, and recall the content that matters most. Whether you are archiving WhatsApp messages, bookmarking Instagram Reels, or saving code snippets for later, NotePilot leverages the Gemini API and local Machine Learning models to automatically organize your digital life.

### Key Features
*   🧠 **Smart Categorization & Auto-Tagging:** Uses a locally trained Naive Bayes and TF-IDF model to automatically suggest tags and organize your saved content.
*   🔎 **Semantic "Necessity Match" Search:** Find exactly what you are looking for—even if you don't remember the exact words—using context-aware semantic search powered by local ML and Gemini.
*   ⏰ **Automatic Deadline Detection:** Extracts actionable tasks and deadlines from your saved notes and messages, seamlessly integrating with Google Calendar.
*   🔗 **Rich Link Previews:** Automatically fetches OpenGraph metadata to display beautiful, visual previews for any saved URLs.
*   💬 **"Read It For Me" (Text-to-Speech):** Hands-free content consumption. Have your long articles and notes read out loud to you using Android's native TTS engine.
*   📥 **Seamless Chat Archiver:** Share messages directly from apps like WhatsApp or Telegram. NotePilot intelligently parses the text, extracts URLs, and identifies potential action items automatically.

---

## 🛠️ Run Locally

This repository contains everything you need to build and run the NotePilot Android app locally.

**Prerequisites:**
*   [Android Studio](https://developer.android.com/studio) (Latest version recommended)
*   An active Google Gemini API Key. Get yours at [Google AI Studio](https://aistudio.google.com/).

### Installation Steps

1. **Open the Project:** Launch Android Studio, select **Open**, and choose the directory containing this project. Allow Android Studio to sync Gradle and fix any initial incompatibilities.
2. **Configure API Keys:**
   - Create a file named `.env` in the root project directory.
   - Add your Gemini API key to the file:
     ```env
     GEMINI_API_KEY=your_actual_api_key_here
     ```
     *(See `.env.example` for reference).*
3. **Debug Configuration:**
   - Open `app/build.gradle.kts`.
   - If you are building a debug build without the provided release keystore, you may need to remove or comment out the `signingConfig` line in the release/debug blocks depending on your local setup.
4. **Run the App:** Click the **Run** button (▶) in Android Studio to deploy NotePilot to your emulator or physical Android device.

---

*View the original scaffold in AI Studio: [https://ai.studio/apps/9f506afe-8f60-414b-8cbb-e2f40f4e66ef](https://ai.studio/apps/9f506afe-8f60-414b-8cbb-e2f40f4e66ef)*
