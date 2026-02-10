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
    api(project(":projectforge-business"))
    api(project(":projectforge-rest"))
    api(libs.jakarta.annotation.api)
    api(libs.jakarta.validation.api)
    testApi(project(":projectforge-commons-test"))
    testApi(testFixtures(project(":projectforge-business")))
    testApi(libs.org.mockito.core)
    testApi(libs.org.springframework.boot.starter.test)
}

description = "org.projectforge.plugins.datatransfer"
