# ProjectForge Development Guide

## Build Commands
- **Backend (Gradle)**: `./gradlew build`, `./gradlew clean`
- **Run Tests**: `./gradlew test`, `./gradlew <module>:test`
- **Single Test**: `./gradlew <module>:test --tests <TestClass.testMethod>`
- **Frontend (Next.js)**: `cd projectforge-next && npm run dev` (dev server)
- **Lint Frontend**: `cd projectforge-next && npm run lint`

## Code Style Guidelines
- **Java/Kotlin**: Use IntelliJ formatter (`misc/IntelliJ/CodeStyle.xml`)
- **Indentation**: 2 spaces (not tabs), line length: 120 chars
- **Naming**: Domain objects use `DO` suffix, DAOs use `Dao` suffix, REST endpoints use `Rest` suffix
- **TypeScript**: Use strict type checking, follow ESLint rules
- **Frontend**: Follow Next.js App Router pattern, use shadcn/ui components with Tailwind CSS

## Project Architecture
- **Backend**: Modular structure with separate modules for business logic, REST API
- **Frontend**: Next.js 15+ with React 19, TypeScript, and Tailwind CSS v4
- **Java/Kotlin**: Target JVM 17, BaseDO classes must be `open` for JPA compatibility
- **Translation**: Use `@PropertyInfo` with `i18nKey` for i18n properties