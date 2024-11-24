plugins {
    id("buildlogic.pf-module-conventions")
}

dependencies {
    api(project(":projectforge-business"))
    api(project(":projectforge-rest"))
    api(libs.jakarta.annotation.api)
    api(libs.jakarta.validation.api)
    testApi(project(":projectforge-commons-test"))
    testApi(testFixtures(project(":projectforge-business")))
    testApi(libs.org.mockito.core)
    testApi(libs.org.springframework.boot.starter.test)
}

description = "org.projectforge.plugins.datatransfer"
