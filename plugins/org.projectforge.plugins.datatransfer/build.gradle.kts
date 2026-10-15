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
    api(libs.jakarta.annotation.api)
    api(libs.jakarta.validation.api)
    testImplementation(project(":projectforge-commons-test"))
    testImplementation(testFixtures(project(":projectforge-business")))
    testImplementation(libs.org.mockito.core)
    testImplementation(libs.org.springframework.boot.starter.test)
}

description = "org.projectforge.plugins.datatransfer"
