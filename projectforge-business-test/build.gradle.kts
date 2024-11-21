plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    api(project(":projectforge-commons-test"))
    implementation(project(":projectforge-business")) // included by projectforge-business
    implementation(libs.org.springframework.spring.test)
    implementation(libs.org.jetbrains.kotlin.kotlin.stdlib)
    implementation(libs.org.junit.jupiter.api)
    implementation(libs.org.mockito.core)
    implementation(libs.org.mockito.junit.jupiter)
    implementation(libs.org.mockito.kotlin)
}

description = "projectforge-business-test"
