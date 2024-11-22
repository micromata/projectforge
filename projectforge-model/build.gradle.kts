plugins {
    id("buildlogic.java-conventions")
}

dependencies {
    api(project(":projectforge-common"))
    api(libs.com.fasterxml.jackson.core.databind)
}

description = "projectforge-model"
