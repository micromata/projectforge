plugins {
    id("buildlogic.pf-module-conventions")
}

dependencies {
    api(project(":projectforge-common"))
    api(libs.jakarta.xml.bind.api)
    api(libs.com.fasterxml.jackson.core.databind)
}

description = "projectforge-model"