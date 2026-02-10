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
    api(project(":projectforge-wicket"))
    api(project(":projectforge-rest"))
    testImplementation(project(":projectforge-business"))
    testImplementation(libs.jakarta.servlet.api)
}

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from("src/main/java") {
        include( "**/*.html") // Wicket pages.
    }
}

description = "org.projectforge.plugins.marketing"
