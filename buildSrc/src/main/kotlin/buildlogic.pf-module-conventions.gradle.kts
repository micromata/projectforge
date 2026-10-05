plugins {
    id("java-library")
}

repositories {
    mavenLocal()
    gradlePluginPortal() // Spring Boot Plugins are here.
    maven { url = uri("https://oss.sonatype.org/content/repositories/public/") }
    maven { url = uri("https://repo.maven.apache.org/maven2/") }
    maven { url = uri("https://raw.githubusercontent.com/gephi/gephi/mvn-thirdparty-repo/") }
}

val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

// libs.versions etc. not available in buildSrc. Must use findVersion and findLibrary instead.

group = "org.projectforge"

extensions.configure<JavaPluginExtension> {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.isIncremental = true
}
/*
tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}*/

tasks.withType<Test> {
    useJUnitPlatform() // JUnit Jupiter
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}

configurations.all {
    // Spring Boot 4 starters pull in Jackson 3 (tools.jackson). ProjectForge stays on Jackson 2 (spring-boot-jackson2)
    // until the Jackson 3 migration, so Spring MVC doesn't switch the JSON mapper of the REST API.
    exclude(group = "org.springframework.boot", module = "spring-boot-starter-jackson")
    resolutionStrategy {
        preferProjectModules() // Prioritize local modules.
        // Force all Jackson module versions to match project's explicit Jackson version.
        // Transitive dependencies (e.g. ez-vcard, groovy-yaml, flyway) may pull in newer Jackson versions.
        val jacksonVersion = libs.findVersion("com.fasterxml.jackson").get().requiredVersion
        force("com.fasterxml.jackson:jackson-bom:$jacksonVersion")
        force("com.fasterxml.jackson.core:jackson-core:$jacksonVersion")
        force("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
        force("com.fasterxml.jackson.core:jackson-annotations:${libs.findVersion("com.fasterxml.jackson.annotations").get().requiredVersion}")
    }
}

dependencies {
    api(libs.findLibrary("org-jetbrains-kotlin-stdlib").get())
    api(libs.findLibrary("io-github-oshai-kotlin-logging").get())
    testImplementation(libs.findLibrary("org-junit-jupiter-api").get())
    testImplementation(libs.findLibrary("org-junit-jupiter-engine").get())
    testImplementation(libs.findLibrary("org-junit-platform-launcher").get())
}
