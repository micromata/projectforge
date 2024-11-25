plugins {
    id("buildlogic.pf-module-conventions")
}

dependencies {
    api(project(":projectforge-common"))
    api(libs.com.fasterxml.jackson.core)
    api(libs.com.fasterxml.jackson.core.databind)
    api(libs.com.fasterxml.jackson.core.annotations)
    api(libs.org.springframework.spring.context)
    api (libs.io.dropwizard.metrics.core)
    api(libs.org.apache.jackrabbit.oak.jcr)
    api(libs.jakarta.annotation.api)
    api(libs.net.lingala.zip4j.zip4j)
    api(libs.org.apache.jackrabbit.oak.jcr)
    api(libs.org.apache.jackrabbit.oak.segment.tar)
    api(libs.org.jetbrains.kotlin.kotlin.stdlib)
    testImplementation(project(":projectforge-commons-test"))
}

description = "projectforge-jcr"
