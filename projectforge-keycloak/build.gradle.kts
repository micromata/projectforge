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
    api(project(":projectforge-business"))
    api(project(":projectforge-rest"))
    api(libs.org.springframework.spring.web)
    api(libs.com.fasterxml.jackson.core.databind)
    api(libs.com.fasterxml.jackson.module.kotlin)
    api(libs.jakarta.annotation.api)
    testImplementation(libs.org.mockito.core)
    testImplementation(libs.org.springframework.boot.starter.test)
}

description = "projectforge-keycloak"
