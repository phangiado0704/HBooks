## Uploading seeded books to Firestore

1. Make sure you have an emulator or device connected with Google services configured for the `hbooks-b6c94` Firebase project.
2. Run the one-off instrumentation helper:

   ```bash
   ./gradlew connectedAndroidTest \
     -Pandroid.testInstrumentationRunnerArguments.class=com.example.hbooks.UploadSeedBooksInstrumentedTest \
     -Pandroid.testInstrumentationRunnerArguments.seedBooks=true
   ```

   This invokes `BookRepository.uploadInitialBooks()` which writes the 36 seeded titles (book001–book036) to Firestore using the storage URLs described in the repository.
3. After the command reports success (look for `BUILD SUCCESSFUL`), delete `app/src/androidTest/java/com/example/hbooks/UploadSeedBooksInstrumentedTest.kt` to avoid accidentally re-running the upload on future CI or developer test runs.

### In-app developer shortcut (debug builds only)
1. Build and run a debug build.
2. Open the Profile tab and scroll to the **Developer Tools** section.
3. Tap **Upload Seed Books**. A snackbar confirms success or surfaces any Firestore errors.
4. Once Firestore contains the seeded catalog, remove `ProfileViewModel`, the developer button, and this doc to keep production builds clean.
