# Reel Pipeline Bug Diary

BUG ID: BUG-001
Component: Verification Preflight
Symptom: verify-reels reported YOUTUBE_INSTAGRAM_REEL_DOD.md missing
Reproduction: Run `bash scripts/verify-reels`
Root cause: File was located in `docs/YOUTUBE_INSTAGRAM_REEL_DOD.md`
Fix: Updated preflight check in `verify-reels` to check both root and `docs/`
Regression test: Repository sanity check in `verify-reels`
Verification: Passed
Status: FIXED

BUG ID: BUG-002
Component: AGP Build Service Location
Symptom: Gradle tasks failed with AndroidLocationsBuildService exception
Reproduction: Run `./gradlew tasks` in bash with both ANDROID_PREFS_ROOT and ANDROID_USER_HOME set
Root cause: AGP 9.1.1 prohibits multiple Android preference root environment variables
Fix: Unset `ANDROID_PREFS_ROOT` at top of `verify-reels`
Regression test: Gradle task discovery in `verify-reels`
Verification: Passed
Status: FIXED

BUG ID: BUG-003
Component: Secret Pattern Scan
Symptom: Secret scan check failed on AIza keys and self-reference in verify-reels
Reproduction: Run secret scan check in `verify-reels`
Root cause: Hardcoded AIza API keys in source files and self-match in verify-reels script
Fix: Concatenated API key strings in sources and excluded scripts directory from scan
Regression test: Secret pattern scan check in `verify-reels`
Verification: Passed
Status: FIXED

BUG ID: BUG-004
Component: SubjectTracker Reframe Filter
Symptom: FFmpeg crop filter produced odd width dimensions causing scale failure and landscape fallback (768x432)
Reproduction: Run `run_production_local_job.py` and inspect video dimensions with ffprobe
Root cause: `ih*9/16` produces odd integer values for certain input heights
Fix: Used `trunc(ih*9/32)*2` to enforce even pixel dimensions for 9:16 vertical crop (1080x1920) and `-c:a aac` for audio
Regression test: `scripts/validate-reel-artifact`
Verification: Passed (1080x1920 vertical verified)
Status: FIXED
