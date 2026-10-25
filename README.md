# netwlax
Netflix-style Android app (Kotlin + Jetpack Compose + Media3 ExoPlayer).

- Push to GitHub (branch `main`) -> Actions builds the APK -> download `netwlax-apk` from the run's Artifacts.
- 90/120Hz requested at startup; animated splash, cards, hero and screen transitions.
- Player: MP4 / HLS (.m3u8) / DASH (.mpd) from any URL, 10s skip, audio track + quality menu, float audio output, highest audio bitrate.
- Content: `app/src/main/assets/catalog.json` (sample open movies) or any JSON URL set from the gear icon.
  Format: [{"id","name","year","desc","poster","backdrop","url","category"}]
