---
name: firebase-app-distribution
description: Configure, validate, and troubleshoot Android APK distribution through Firebase App Distribution and GitHub Actions. Use for staging tester releases, Firebase app IDs, service-account authentication, tester groups, or CI artifact delivery. Do not use for Firebase SDK integration or Google Play publishing.
---

# Firebase App Distribution

Use this skill when the task concerns distributing the Android staging APK to
testers through Firebase App Distribution or maintaining its GitHub Actions
pipeline.

## Repository contract

- Staging package: `com.loresuelvo.consumer.staging`.
- Firebase project: `loresuelvo-staging`.
- Firebase app ID is stored in the GitHub Environment variable
  `FIREBASE_APP_ID_STAGING`.
- Tester aliases are stored in `FIREBASE_TESTERS_STAGING` (currently the
  `facultad-staging` group).
- The release workflow is `.github/workflows/release.yml` and runs on tags
  matching `v*.*.*`.
- App Distribution does not require `google-services.json` when Firebase is
  used only for CLI distribution. Never add a service-account JSON file to the
  repository.

## Required GitHub configuration

Configure these in the `staging` Environment:

- Secret: `FIREBASE_SERVICE_ACCOUNT_JSON`, containing the complete downloaded
  service-account JSON.
- Variables: `FIREBASE_APP_ID_STAGING` and `FIREBASE_TESTERS_STAGING`.
- Android Auth0 variables: `AUTH0_CLIENT_ID_ANDROID_CONSUMER_STAGING` and
  `AUTH0_SCHEME_ANDROID_CONSUMER_STAGING`.

The service account needs the Firebase App Distribution Admin role. The
workflow writes the JSON only to `$RUNNER_TEMP`, exposes it through
`GOOGLE_APPLICATION_CREDENTIALS`, and never prints it.

## Release flow

1. Commit and push the workflow and app configuration to `main`.
2. Create an annotated version tag, for example `v0.22.2`.
3. Push the tag with `git push origin v0.22.2`.
4. The workflow runs staging JVM tests, builds the staging debug APK, and
   uploads it to the configured tester group with the Firebase CLI.
5. The production job runs only after staging and uses the `production`
   Environment gate. The current job exposes the generated production AAB for
   the existing manual production step.

Do not push a tag until the commit containing the pipeline change is already
on `main`; otherwise the tag can build an older workflow.

## Local upload diagnosis

For a local diagnostic upload, use a service-account file outside the repo:

```bash
export GOOGLE_APPLICATION_CREDENTIALS=/secure/path/firebase-service-account.json
firebase appdistribution:distribute app/build/outputs/apk/staging/debug/app-staging-debug.apk \
  --app "$FIREBASE_APP_ID_STAGING" \
  --groups "$FIREBASE_TESTERS_STAGING" \
  --release-notes "Local staging build"
```

Never paste the JSON, private key, access token, or its contents into logs,
issues, commits, or chat. If credentials leak, revoke and recreate the service
account key before continuing.

## Troubleshooting

- `Permission denied`: verify the service account role and that the JSON
  belongs to the same Firebase project as the app ID.
- `App not found`: verify the Firebase app ID, not the Android package name.
- No tester receives a release: verify the tester email is in the configured
  group and that `FIREBASE_TESTERS_STAGING` contains the group alias.
- APK rejected: verify the package is `com.loresuelvo.consumer.staging` and
  that the artifact path points to an APK, not the production AAB.
- Auth0 login fails after installation: check the exact staging callback scheme
  and remember that CI debug signing may not have the same SHA-256 fingerprint
  as a local keystore.

Official references:

- [Firebase App Distribution](https://firebase.google.com/docs/app-distribution)
- [Service-account authentication](https://firebase.google.com/docs/app-distribution/authenticate-service-account)
- [Android CLI distribution](https://firebase.google.com/docs/app-distribution/android/distribute-cli)
