import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("buildlogic.pf-module-conventions")
    id("org.jetbrains.kotlin.jvm")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
    }
}

dependencies {
    api(project(":projectforge-common"))
    api(libs.com.fasterxml.jackson.core)
    api(libs.com.fasterxml.jackson.core.databind)
    api(libs.com.fasterxml.jackson.core.annotations)
    api(libs.com.zaxxer.hikaricp)
    api(libs.org.springframework.spring.context)
    api(libs.org.springframework.boot.autoconfigure)
    api(libs.io.dropwizard.metrics.core)
    api(libs.org.apache.jackrabbit.oak.jcr)
    api(libs.jakarta.annotation.api)
    api(libs.net.lingala.zip4j.zip4j)
    api(libs.org.apache.jackrabbit.oak.jcr)
    api(libs.org.apache.jackrabbit.oak.segment.tar)
    api(libs.org.apache.jackrabbit.oak.store.document)
    api(libs.org.jetbrains.kotlinx.coroutines.core)
    api(libs.org.jetbrains.kotlin.stdlib)
    testImplementation(project(":projectforge-commons-test"))
}

description = "projectforge-jcr"
