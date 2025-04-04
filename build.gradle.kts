import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version libs.versions.org.jetbrains.kotlin.get() apply false
    id("org.sonarqube") version "5.1.0.4882"
}

allprojects {
    group = "org.projectforge"
    version = "8.1-SNAPSHOT" // Update version string here (nowhere else)

    repositories {
        mavenCentral()
        gradlePluginPortal() // Spring Boot Plugins are here.
        maven {
            url = uri("https://oss.sonatype.org/content/repositories/public/")
        }
        maven {
            url = uri("https://repo.maven.apache.org/maven2/")
        }
    }
}

allprojects {
    configurations.all {
        exclude(group = "org.slf4j", module = "slf4j-jul")
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude(group = "com.sun.mail", module = "jakarta.mail")
        exclude(group = "jakarta.mail", module = "jakarta.mail-api")
        exclude(group = "org.apache.logging.log4j", module = "log4j-core")
        exclude(group = "commons-logging", module = "commons-logging")
        resolutionStrategy {
            preferProjectModules() // Prioritize local modules.
        }
    }
}
