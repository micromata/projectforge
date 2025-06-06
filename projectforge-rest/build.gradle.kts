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
    api(libs.org.springframework.boot.starter.webflux)
    api(libs.jakarta.annotation.api)
    api(libs.jakarta.ws.rs.api)
    api(libs.jakarta.validation.api)
    api(libs.com.google.zxing.core)
    api(libs.com.google.zxing.javase)
    api(libs.org.springframework.spring.webmvc)
    testImplementation(project(":projectforge-commons-test"))
    testImplementation(testFixtures(project(":projectforge-business")))
    testImplementation(libs.com.googlecode.gson)
    testImplementation(libs.org.mockito.core)
}

description = "projectforge-rest"
