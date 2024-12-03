plugins {
    id("buildlogic.pf-module-conventions")
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
