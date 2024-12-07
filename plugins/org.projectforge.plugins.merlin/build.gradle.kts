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
    api(project(":org.projectforge.plugins.datatransfer"))
    api(libs.fr.opensagres.xdocrepor.poi.xwpf.converter.pdf)
    testImplementation(project(":projectforge-business"))
}

description = "org.projectforge.plugins.merlin"
