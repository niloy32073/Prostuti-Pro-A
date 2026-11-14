# Deep links

The app opens from two kinds of link. Both use the **same paths** and are parsed in one place
(`navigation/AppLink.kt`); `OpenAppLinks` (navigation/FeatureEntries.kt) opens the screen in its tab with a
real back stack behind it.

| Link | Who uses it |
|---|---|
| `prostuti://open/<path>` | the home screen widget, notifications, the website's "open in app" buttons |
| `https://prostuti.com.bd/app/<path>` (also `www.`) | verified App Links: links on the website, links people share |

Only `/app/...` on the website belongs to the app. Every other page of prostuti.com.bd keeps opening in the
browser (the website will not run exams; it may have other pages, and those must stay reachable).

## Paths

| Path | Opens |
|---|---|
| `home` | Home |
| `live` | Live exams (Home tab) |
| `live/<examId>` | One live exam (Home tab: list → detail) |
| `group/<groupId>` | A group's routine calendar (Home tab: groups → group → calendar) |
| `routine` | Routine tab |
| `routine/create` | Personal routine builder (Routine tab) |
| `leaderboard` | Leaderboard tab |

Rules:
- A link only **navigates**. It never takes an action (join, pay, submit): anyone can build a `prostuti://` link.
  A screen opened with a wrong or foreign id shows its own error.
- Signed out: the link waits and opens after sign-in.
- An exam in progress is never interrupted: a link that arrives during an exam is dropped.
- Unknown paths open nothing (the app just comes up).

## Website setup (needed for the https links)

Without this file Android (12+) opens `https://prostuti.com.bd/app/...` in the browser, not the app.
Serve it at `https://prostuti.com.bd/.well-known/assetlinks.json` (and the same on `www.`), `Content-Type:
application/json`, no redirect:

```json
[{
  "relation": ["delegate_permission/common.handle_all_urls"],
  "target": {
    "namespace": "android_app",
    "package_name": "com.prostuti.student",
    "sha256_cert_fingerprints": [
      "RELEASE_SHA256_HERE",
      "25:DD:22:81:57:B9:30:27:62:33:CA:F4:CD:97:3A:F3:5A:EB:C7:18:F1:3A:DA:C0:89:C5:87:AF:B4:CD:F7:46"
    ]
  }
}]
```
- `RELEASE_SHA256_HERE`: the release key's SHA-256 (Play Console → App integrity → App signing key certificate,
  when Play App Signing is on). The second line is this machine's debug key, for testing only.
- The website should also have a page at each `/app/...` path (for people without the app): e.g. a short page
  with a Play Store button.

## Planned: payment on the website

The website may take payments and send the student back to the app. Keep that inside the same scheme:
the payment page returns to `https://prostuti.com.bd/app/payment/<result>?...` (or `prostuti://open/payment/...`).
When that is built:
- add a `payment` path to `AppLink` and its screen;
- the app must **not trust the link** for the payment result: on arrival it asks the server for the payment
  status (the link only says "go to the payment screen");
- if the payment flow needs data from the app's domain (cookies, a session), decide that with the backend first.

## Testing

```bash
adb shell am start -W -a android.intent.action.VIEW -d "prostuti://open/live" com.prostuti.student
adb shell am start -W -a android.intent.action.VIEW -d "https://prostuti.com.bd/app/routine" com.prostuti.student
adb shell pm get-app-links com.prostuti.student
```
Before assetlinks.json is live, let a test device treat the domain as verified:
`adb shell pm set-app-links-user-selection --user cur --package com.prostuti.student true prostuti.com.bd`.
Test cold start, warm start (app open: singleTop → onNewIntent), signed out (opens after sign-in), during an exam
(dropped), rotation after a link (not re-applied).

## Not done yet (android-navigation-3 skill §11)
- Matching uses `AppLink.parse`, not Nav3's `UriDeepLinkMatcher`: one parser for both schemes, already mapped to
  tab stacks. Move to matchers if links grow complex.
- A pending link lives in memory (`AppLinkRequests`): if Android kills the app before it is applied (e.g. while
  waiting for sign-in), it is lost.
- Opened inside another app's task (no NEW_TASK), Up does not yet go to the app's own task (skill §11.7).
