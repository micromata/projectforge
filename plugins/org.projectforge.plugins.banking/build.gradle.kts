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
    api(project(":projectforge-rest"))
    testImplementation(project(":projectforge-business"))
    testImplementation(project(":projectforge-commons-test"))
    testImplementation(testFixtures(project(":projectforge-business")))
    testImplementation(libs.org.springframework.boot.starter.test)
}

description = "org.projectforge.plugins.banking"
