# ERP Music diagnostic development gates

Scope: continue `feature/erp-music-media-diagnostics`. Never modify the stable
main branch, stable tags/installers, PSA CarPlay 1.2.0, ROM or MCU components.

Before editing, read the actual source/build files, confirm the current branch
head and inspect recent Actions failures. Record implemented versus pending
features, affected files, public API28 compatibility and permission/lifecycle
effects. Use the smallest justified change; do not rewrite the player.

The debug APK is a passive diagnostic application. Do not activate a player
MediaSession, grab media buttons, record audio or request focus automatically.
Focus probes require an explicit UI click and must release on timeout, manual
release and service destruction. OEM data that is unavailable must be labelled
as a permission/interface visibility limitation, never fabricated.

Required local gates (JDK17 / Gradle8.5 / SDK28+34 / build-tools34.0.0):

1. `python3 tools/check_diagnostics.py` and `git diff --check`.
2. `gradle --no-daemon --stacktrace assembleDebug :app:verifyDiagnosticApi28 :app:testDebugUnitTest`.
3. `gradle --no-daemon :app:lintDebug`.
4. Print the packaged manifest with apkanalyzer, run `tools/verify_apk.py`,
   verify the APK signature and compute SHA-256.

Do not disable checks, add blanket Lint suppressions or skip failed tests to
make a build green. Read full error logs and the affected code before fixing.

Push one checked group of changes, then wait for that exact commit's Actions
conclusion. Never queue speculative commits. On failure inspect complete job
logs before another controlled fix. Do not merge/release on failure.

Delivery must state commit, changed files, local/CI results, downloadable APK,
SHA-256, actual signer, confirmed fixes, unverified risks and rollback steps.
Code, compilation, automated tests, APK generation and real-car acceptance are
separate statuses. API28 JVM tests are not PSA hardware tests.

Stop for permission/access blockers, unavailable necessary source/environment,
important data deletion, stable version overwrite, sensitive authentication
materials or any change beyond the requested diagnostic scope.
