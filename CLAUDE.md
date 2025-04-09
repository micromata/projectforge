# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build/Test Commands
- Build all: `./gradlew build`
- Build skipping tests: `./gradlew build -x test`
- Run application: `./gradlew bootRun`
- Single test: `./gradlew test --tests "org.projectforge.package.ClassName.methodName"`
- Package tests: `./gradlew test --tests "org.projectforge.package.*"`
- Run specific module tests: `./gradlew :projectforge-business:test`

## Code Style Guidelines
- Use Kotlin JVM target 17 for all code; legacy code is in Java
- Follow standard Kotlin naming conventions (camelCase for variables/functions, PascalCase for classes)
- Include standard ProjectForge license header in all new files
- Organize imports with Kotlin stdlib first, followed by domain/project imports
- Use non-null types by default; use Kotlin's nullable types (Type?) when needed
- Use JUnit 5 for tests with descriptive method names
- Prefer Kotlin extension functions for utility methods
- Use SpringBoot annotations for dependency injection
- Use Kotlin Coroutines for async operations
- Handle exceptions with appropriate logging using kotlin-logging
- Format code with 4-space indentation