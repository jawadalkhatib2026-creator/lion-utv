# 🦁 Lion UTV

Android TV IPTV player (also installs on Android phones/tablets). Native Kotlin, lion theme.

## Build the APK with GitHub (no Android Studio needed)

1. Create a new repository on GitHub and upload **everything in this folder** (keep `.github/workflows/build.yml`).
2. Open the **Actions** tab → **Build Lion UTV APK** → wait for the green check (about 5 minutes).
3. Open the finished run → **Artifacts** → download **LionUTV-apk** → unzip → `LionUTV.apk`.
4. Install it on your Android TV (USB stick, the "Send files to TV" app, or `adb install LionUTV.apk`).

If you prefer Android Studio: open this folder, let Gradle sync, and run **Build → Build APK(s)**.

## Sign-in codes

| Who | Code |
|-----|------|
| Admin | `112233` (change it in Settings → Change Admin Code) |
| Users | Created by the admin: Settings → Manage Users (or from the phone panel) |

Each user code can be disabled, renewed or deleted, and can have an expiry date.
Users and channels are stored **on the device they were added on**.

## Adding channels (admin only)

**Settings → Add Channels (QR)**: the TV shows a QR code. Scan it with your phone (same Wi-Fi), enter the admin code,
then add single channels, import an M3U playlist link, paste M3U text, manage users, or delete channels.
The page only runs while that screen is open, and the Settings tiles for admin tools are hidden from normal users.

You can also manage channels directly on the TV: Settings → Manage Channels / Manage Users.

The app ships with one public test stream (labelled Demo) so you can check playback. Add your own playlist for everything else.

## Remote controls

* Live TV / Movies / Series: categories on the left, channels in the middle, preview on the right. OK plays in the preview, OK again goes full screen.
* Full screen: UP/DOWN (or CH+/CH-) change channel, OK shows info, LEFT/RIGHT seek 10 seconds on movies.
* Settings has English / Arabic and the picture size (fit / fill / zoom).

## Free channels, movies and series

Admin: Settings → Manage Channels → **Free public channels** (or the "M3U" tab on the phone panel) imports public
lists from iptv-org (all, Arabic, Jordan, movies, series, sports, news, kids). These are free public streams; some are
offline or region-locked. For anything else, import your own M3U link.
