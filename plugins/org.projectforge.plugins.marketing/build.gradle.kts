plugins {
    id("buildlogic.pf-module-conventions")
}

dependencies {
    api(project(":projectforge-wicket"))
    api(project(":projectforge-rest"))
    testImplementation(project(":projectforge-business"))
    testImplementation(libs.jakarta.servlet.api)
}

description = "org.projectforge.plugins.marketing"
