# Attendance Pro – Employee Attendance Management (Android)

Kotlin + Jetpack Compose + Firebase (Auth, Firestore, Storage). Dark/silver 3D glass UI.

## How the app works
- **User panel** opens directly, no login. It only shows the sheets, names and attendance created by the admin (view only).
- **Admin Panel**: user panel top-right 3-dot menu -> Admin Panel -> sign in (password protected). Admin can create sheets (sheet name + group name), open a sheet, add / remove names (shown A to Z), and mark Present / Absent / Late / Leave per date.
- Employee / old attendance screens were removed.
- **Republish `firestore.rules`** (public read for sheets, admin-only write).

## Status
- **Phase 2 (this commit):** employee add/edit/activate/deactivate/soft-delete (creates the worker login), daily attendance entry with auto working hours, late detection (grace period), **early-leave reason box**, office settings, live dashboard, worker panel reading only own records. All data is stored permanently in Firestore.
- **Phase 1:** project + CI, theme, splash, login (Admin/Worker tabs, show/hide password,
  forgot password, persistent session, logout), server-resolved roles, 5-tab bottom nav per role, Firestore rules.
- Phase 2: employees, 2026 calendar, attendance sheet. Phase 3: late detection + settings.
  Phase 4: reports + worker panel. Phase 5: PDF/Excel export, notifications, backup.

## Firebase setup (one time)
1. Firebase console → create project → add Android app, package `com.attendance.pro`.
2. Enable **Authentication → Email/Password**, **Firestore**, **Storage**.
3. Download `google-services.json` → `app/google-services.json` (git-ignored).
4. Firestore → Rules → paste `firestore.rules` → Publish.
5. **Create the first Admin (secure setup):**
   - Authentication → Add user (email + password).
   - Firestore → collection `admins` → document ID = that user's **UID** → field `name` (string).
   - Admin docs cannot be written from the app (rules block it).
6. Workers sign in with email, or a plain username (stored internally as `username@attendance.app`).

## Data storage
Everything is saved in Cloud Firestore (permanent) and cached on the phone. Admin changes show up in the worker app in real time.
Attendance doc id = `{employeeUid}_{yyyy-MM-dd}` so duplicates are impossible. Deleting an employee is a soft delete: history is kept.
Set office start/closing time and grace period in **Settings** before marking attendance.

## GitHub Actions
Settings → Secrets and variables → Actions:

| Secret | Value |
|---|---|
| `GOOGLE_SERVICES_JSON` | base64 of `app/google-services.json` (raw JSON text also works). If missing, CI uses a placeholder so the build still passes |
| `KEYSTORE_BASE64` (optional) | base64 of your `.jks` |
| `KEYSTORE_PASSWORD`, `KEY_ALIAS`, `KEY_PASSWORD` (optional) | signing values |

Push to `main` → debug APK appears under the run's **Artifacts**. Release APK builds only if signing secrets exist.

## Local build
JDK 17 + Gradle 8.7: `gradle assembleDebug`

## Security
No secrets in the repo. Roles come from Firestore and are enforced by security rules, not client checks.
