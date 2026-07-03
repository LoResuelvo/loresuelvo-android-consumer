.PHONY: help up build lint test e2e test-all-once ci clean devices

help:
	@echo "Available commands:"
	@echo "  make up"
	@echo "  make build"
	@echo "  make test"
	@echo "  make e2e"
	@echo "  make test-all-once"
	@echo "  make ci"
	@echo "  make clean"

up: build

build:
	./gradlew assembleDebug

test:
	./gradlew test

e2e:
	bash scripts/run_acceptance_tests.sh

test-all-once: test e2e

lint:
	./gradlew lint

ci: build lint test e2e

clean:
	./gradlew clean

devices:
	adb devices