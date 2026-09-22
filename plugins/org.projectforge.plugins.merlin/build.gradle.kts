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
    // Exclude the ancient com.lowagie:itext:2.1.7 here too, so the plugin doesn't pull it back onto
    // the classpath beside OpenPDF (which provides the same com.lowagie.text.* classes, see
    // projectforge-business build).
    api(libs.fr.opensagres.xdocrepor.poi.xwpf.converter.pdf) {
        exclude(group = "com.lowagie", module = "itext")
    }
    testImplementation(project(":projectforge-business"))
}

description = "org.projectforge.plugins.merlin"
