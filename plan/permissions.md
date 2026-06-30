# App Permissions

Declared in `AndroidManifest.xml` plus one special-access grant.

## Runtime permissions (dialog)
- `POST_NOTIFICATIONS` — show notifications (Android 13+ prompt)
- `WAKE_LOCK` — keep CPU awake while lockscreen/service runs

## Special access (granted via system settings, not a simple dialog)
- `SYSTEM_ALERT_WINDOW` — draw over other apps (lockscreen overlay)
- `USE_FULL_SCREEN_INTENT` — launch lockscreen activity full-screen
- `TURN_SCREEN_ON` — wake the display for the lockscreen
- `REQUEST_IGNORE_BATTERY_OPTIMIZATIONS` — exempt from Doze so the service survives

## Implicit special access (no uses-permission; Settings toggle)
- Notification listener — `MediaListenerService` binds with
  `BIND_NOTIFICATION_LISTENER_SERVICE` to read media notifications.
  User must enable it in **Settings → Notification access**.
