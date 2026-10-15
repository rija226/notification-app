# Podsjetnik app icon (option A, "Zvono u petlji")

A white bell inside a light blue repeat loop on the brand blue (#2E55A5). The loop says the app reminds again and again until the task is done.

## Files

| Path | What |
|---|---|
| `res/drawable/ic_launcher_background.xml` | background layer, solid #2E55A5 (108dp) |
| `res/drawable/ic_launcher_foreground.xml` | foreground layer: loop #ADC4FF, bell #FFFFFF (108dp, logo 50dp, inside the 66dp safe zone) |
| `res/drawable/ic_launcher_monochrome.xml` | monochrome layer for themed icons (Android 13+) |
| `res/mipmap-anydpi-v26/ic_launcher.xml` | adaptive icon (background + foreground + monochrome) |
| `res/mipmap-anydpi-v26/ic_launcher_round.xml` | same, for `android:roundIcon` |
| `res/drawable/ic_stat_podsjetnik.xml` | 24dp white bell, notification small icon |
| `playstore/ic_launcher-playstore-512.png` | Google Play listing icon: 512 x 512, 32-bit PNG, full square (Play adds the rounded corners and shadow) |
| `svg/` | master SVGs (full 108 canvas, visible 72 area, foreground, monochrome, notification icon) |
| `preview-opcije.png` | the three options compared (A, B, C) |

All vectors, no PNG mipmaps needed: the app's minSdk is 26, where adaptive icons are supported.

## Install (Claude Code)

1. Copy everything under `res/` into `app/src/main/res/`, replacing the default launcher icon files.
2. Delete the template's `mipmap-*dpi/ic_launcher*.webp` files if present (minSdk 26 uses only `mipmap-anydpi-v26`).
3. In `AndroidManifest.xml`:
   ```xml
   <application
       android:icon="@mipmap/ic_launcher"
       android:roundIcon="@mipmap/ic_launcher_round"
       ... >
   ```
4. In `NotificationFactory`: `setSmallIcon(R.drawable.ic_stat_podsjetnik)`.
5. Keep `playstore/ic_launcher-playstore-512.png` for a future Play listing; it is not part of the APK.

Prompt for Claude Code:
```
Install the app icon from docs/handoff/icon/ following its README.md
(adaptive icon with monochrome layer, round icon, notification small icon).
```
