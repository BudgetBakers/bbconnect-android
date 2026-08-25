.PHONY: lint test build

# Guarded on Gradle (same pattern as sdks/python and sdks/swift-link).
# Compiling additionally needs the Android SDK — this module's tests run in
# CI with an Android image; see README "CI handoff".
ifeq (,$(shell command -v gradle 2>/dev/null))
lint test build:
	@echo "sdks/kotlin-link: gradle not installed — skipping $@ (CI runs this with an Android image)"
else
build:
	gradle :bbconnect:assembleRelease

test:
	gradle :bbconnect:testReleaseUnitTest

lint:
	gradle :bbconnect:lintRelease
endif
