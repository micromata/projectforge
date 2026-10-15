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
    api(project(":projectforge-rest"))
    testImplementation(project(":projectforge-business"))
    testImplementation(libs.org.mockito.core)
    testImplementation(libs.org.mockito.junit.jupiter)
    testImplementation(libs.org.mockito.kotlin)
}

description = "org.projectforge.plugins.liquidityplanning"
