.PHONY: help up build lint test instrumented test-all-once ci clean devices

FLAVOR ?= Dev

help:
	@echo "Available commands:"
	@echo "  make up"
	@echo "  make build"
	@echo "  make lint"
	@echo "  make test"
	@echo "  make instrumented"
	@echo "  make test-all-once"
	@echo "  make ci"
	@echo "  make clean"
	@echo "  make devices"
	@echo ""
	@echo "  Todos los targets aceptan FLAVOR=Dev|Staging|Prod (default: Dev)"

up: build

build:
	./gradlew assemble$(FLAVOR)Debug

lint:
	./gradlew lint$(FLAVOR)Debug

test:
	./gradlew test$(FLAVOR)DebugUnitTest

instrumented:
	bash scripts/run_instrumented_tests.sh $(FLAVOR)

test-all-once:
	./gradlew test$(FLAVOR)DebugUnitTest connected$(FLAVOR)DebugAndroidTest \
		-Pandroid.testInstrumentationRunnerArguments.package=com.loresuelvo.consumer.instrumented

ci: build lint test-all-once

clean:
	./gradlew clean

devices:
	adb devices
