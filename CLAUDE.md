# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Build/Test Commands
- Build all: `./gradlew build`
- Build skipping tests: `./gradlew build -x test`
- Run application: `./gradlew bootRun`
- Single test: `./gradlew test --tests "org.projectforge.package.ClassName.methodName"`
- Package tests: `./gradlew test --tests "org.projectforge.package.*"`
- Run specific module tests: `./gradlew :projectforge-business:test`

## Language
- Write all code comments, KDoc/JavaDoc, commit messages and documentation in English

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

## graphify

This project can use a knowledge graph at graphify-out/ with god nodes, community structure, and cross-file relationships. Both the graph and the tool are local and untracked: install graphify into a project-local venv, then run `graphify update .` to build graphify-out/. Until then the rules below simply do not apply — work from the sources as usual.

The graphify binary is installed here at `venv/bin/graphify` (project-local venv, never on `$PATH`); invoke it as `venv/bin/graphify query "..."` etc.

Rules:
- For codebase questions, first run `graphify query "<question>"` when graphify-out/graph.json exists. Use `graphify path "<A>" "<B>"` for relationships and `graphify explain "<concept>"` for focused concepts. These return a scoped subgraph, usually much smaller than GRAPH_REPORT.md or raw grep output.
- If graphify-out/wiki/index.md exists, use it for broad navigation instead of raw source browsing.
- Read graphify-out/GRAPH_REPORT.md only for broad architecture review or when query/path/explain do not surface enough context.
- After modifying code, run `graphify update .` to keep the graph current (AST-only, no API cost).
