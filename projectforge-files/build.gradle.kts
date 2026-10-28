import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("buildlogic.pf-module-conventions")
    id("org.jetbrains.kotlin.jvm")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    api(project(":projectforge-common"))
    api(libs.com.fasterxml.jackson.core)
    api(libs.com.fasterxml.jackson.core.databind)
    api(libs.com.fasterxml.jackson.core.annotations)
    api(libs.org.springframework.spring.context)
    api(libs.jakarta.annotation.api)
    api(libs.org.jetbrains.kotlin.stdlib)
    testImplementation(project(":projectforge-commons-test"))
    testImplementation(libs.org.hsqldb.hsqldb)
    testImplementation(libs.org.springframework.boot) // logback-test.xml includes its base.xml
}

description = "projectforge-files"
