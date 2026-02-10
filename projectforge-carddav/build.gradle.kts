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
    api(project(":projectforge-model"))
    api(project(":projectforge-business"))
    api(project(":projectforge-rest"))
    api(libs.org.springframework.spring.webmvc)
    api(libs.com.googlecode.ez.vcard)
    testImplementation(libs.org.mockito.core)
}

description = "projectforge-carddav"
