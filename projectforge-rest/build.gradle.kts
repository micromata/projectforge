plugins {
    id("buildlogic.pf-module-conventions")
}

dependencies {
    api(project(":projectforge-business"))
    api(libs.org.springframework.boot.starter.webflux)
    api(libs.jakarta.annotation.api)
    api(libs.jakarta.ws.rs.api)
    api(libs.jakarta.validation.api)
    api(libs.com.google.zxing.core)
    api(libs.com.google.zxing.javase)
    testImplementation(project(":projectforge-commons-test"))
    testImplementation(testFixtures(project(":projectforge-business")))
    testImplementation(libs.com.google.code.gson)
    testImplementation(libs.org.mockito.core)
}

description = "projectforge-rest"
