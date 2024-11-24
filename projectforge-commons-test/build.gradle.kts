plugins {
    id("buildlogic.pf-module-conventions")
}

dependencies {
    api(project(":projectforge-common"))
    api(libs.org.junit.jupiter.api) // Must be part of implementation, because it is used in the test code.
}

description = "projectforge-commons-test"
