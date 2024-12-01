import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version libs.versions.org.jetbrains.kotlin.get() apply false
}

allprojects {
    group = "org.projectforge"
    version = "8.0.0"

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
        exclude(group = "org.slf4j", module = "slf4j-log4j12")
        exclude("org.apache.logging.log4j", "log4j-api")
        exclude("org.apache.logging.log4j", "log4j-core")
        exclude(group = "commons-logging", module = "commons-logging")
        resolutionStrategy {
            preferProjectModules() // Prioritize local modules.
        }
    }
}

subprojects {
    apply(plugin = "org.jetbrains.kotlin.jvm")
    apply(plugin = "java")

    configure<JavaPluginExtension> {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    tasks.withType<KotlinCompile> {
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
        }
    }

    tasks.withType<Test> {
        useJUnitPlatform()
    }
}
