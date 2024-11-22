plugins {
    id("java-library")
    id("org.jetbrains.kotlin.jvm")
    id("buildlogic.java-conventions")
}

dependencies {
    api(project(":projectforge-business"))
    api(project(":projectforge-rest"))
    api(libs.jakarta.annotation.api)
    api(libs.jakarta.validation.api)
    testImplementation(project(":projectforge-commons-test"))
    testImplementation(testFixtures(project(":projectforge-business")))
}

description = "org.projectforge.plugins.datatransfer"
