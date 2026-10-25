# cleanyourtv

A light, ad-free home screen for a Fire TV Stick (2nd gen, Fire OS 5). Rows for apps and settings, a "continue" banner for the last app you opened, and a hide-list that keeps shopping, demo and ad apps off the screen.

No root. Nothing is uninstalled.

## Get the APK

1. Open the **Actions** tab of this repo.
2. Click the latest **Build APK** run.
3. Download `clean-home-apk` from Artifacts and unzip it. You get `app-debug.apk`.

## Install on the TV

```
adb connect TV_IP:5555
adb install -r app-debug.apk
```

## Make it the home screen

Fire OS keeps Amazon's launcher as the system home, so a small accessibility service puts Clean Home on top whenever Amazon's home shows up.

```
adb shell settings put secure enabled_accessibility_services com.cleanyourtv.launcher/com.cleanyourtv.launcher.HomeRedirectService
adb shell settings put secure accessibility_enabled 1
```

Then press Home on the remote.

## Undo

```
adb shell settings put secure enabled_accessibility_services ""
adb shell settings put secure accessibility_enabled 0
adb uninstall com.cleanyourtv.launcher
```

That brings back the stock Fire TV home exactly as before.

## Change what is hidden

Edit the `HIDDEN` list at the top of `app/src/main/java/com/cleanyourtv/launcher/MainActivity.java`. Edit `PINNED` to change which apps come first.
