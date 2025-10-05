import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("io.spring.dependency-management") version libs.versions.io.spring.dependency.management.get()
    id("java-test-fixtures")
    id("buildlogic.pf-module-conventions")
    id("org.jetbrains.kotlin.jvm")
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

tasks.test {
    useJUnitPlatform()
}

dependencies {
    api(project(":projectforge-common"))
    api(project(":projectforge-model"))
    api(project(":projectforge-jcr"))
    api(libs.de.micromata.merlin.core)
    api(libs.com.googlecode.json.simple)
    api(libs.org.aspectj.aspectjtools)
    api(libs.jakarta.activation.api)
    api(libs.jakarta.annotation.api)
    api(libs.jakarta.persistence.api)
    api(libs.jakarta.servlet.api)
    api(libs.joda.time.joda.time)
    api(libs.org.apache.commons.collections4)
    api(libs.org.eclipse.angus.jakarta.mail)
    api(libs.org.hibernate.orm.core)
    api(libs.org.hibernate.search.mapper.orm)
    api(libs.org.hibernate.search.backend.lucene)
    api(libs.org.hibernate.validator)
    api(libs.org.jetbrains.kotlin.compiler.embeddable)
    api(libs.org.jetbrains.kotlin.scripting.compiler.embeddable)
    api(libs.org.jetbrains.kotlin.scripting.common)
    api(libs.org.jetbrains.kotlin.scripting.jvm)
    api(libs.org.jetbrains.kotlin.scripting.jvm.host)
    api(libs.org.jetbrains.kotlinx.coroutines.core)

    // Logging:
    api(libs.ch.qos.logback.classic)
    api(libs.ch.qos.logback.core)
    api(libs.org.slf4j.api)
    api(libs.org.slf4j.jcl.over.slf4j)
    api(libs.org.slf4j.jul.to.slf4j)
    api(libs.org.apache.logging.log4j.api)
    api(libs.org.apache.logging.log4j.to.slf4j)
    api(libs.org.jetbrains.kotlinx.coroutines.slf4j)

    api(libs.org.springframework.boot.starter)
    api(libs.org.springframework.boot.starter.data.jpa)
    api(libs.org.springframework.security.config)
    api(libs.org.springframework.security.core)
    api(libs.org.springframework.security.web)
    api(libs.org.springframework.spring.tx)
    api(libs.org.springframework.spring.context)
    api(libs.org.springframework.spring.orm)
    api(libs.org.springframework.spring.web)
    api(libs.commons.io)
    api(libs.org.bouncycastle.bcprov.jdk18on)
    api(libs.org.dom4j)
    api(libs.org.apache.httpcomponents.client5.httpclient5)
    api(libs.org.flywaydb.core)
    api(libs.org.flywaydb.database.hsqldb)
    api(libs.org.flywaydb.database.postgresql)
    api(libs.org.postgresql)
    api(libs.org.hsqldb.hsqldb)
    api(libs.org.mnode.ical4j.ical4j)
    api(libs.com.googlecode.ez.vcard)
    api(libs.com.thoughtworks.xstream)
    api(libs.com.itextpdf)
    api(libs.org.apache.groovy.all)
    api(libs.org.jfree.jfreechart)
    api(libs.net.sourceforge.mpxj)
    api(libs.org.apache.commons.text)
    api(libs.org.apache.poi)
    api(libs.org.apache.poi.ooxml)
    api(libs.org.apache.xmlgraphics.fop)
    api(libs.org.apache.xmlgraphics.commons)
    api(libs.org.apache.xmlgraphics.batik.codec)
    api(libs.org.apache.xmlgraphics.batik.constants)
    api(libs.org.apache.xmlgraphics.batik.dom)
    api(libs.org.apache.xmlgraphics.batik.svg.dom)
    api(libs.com.fasterxml.jackson.module.kotlin)
    api(libs.com.fasterxml.jackson.core.annotations)
    api(libs.com.fasterxml.jackson.core.databind)
    api(libs.com.fasterxml.jackson.datatype.hibernate6)
    api(libs.com.fasterxml.jackson.datatype.jsr310)
    api(libs.se.sawano.java.alphanumeric.comparator)
    api(libs.com.webauthn4j.core)
    /*api(libs.com.webauthn4j.spring.security.core) {
        exclude("org.springframework")
        exclude("org.springframework.security")
        exclude("org.springframework.boot")
    }*/

    testImplementation(libs.org.apache.directory.server.apacheds.server.integ)
    testImplementation(libs.org.mock.server.mockserver.netty.no.dependencies)
    testImplementation(libs.org.springframework.boot.starter.test)

    compileOnly(libs.com.zaxxer.hikaricp)

    testFixturesImplementation(project(":projectforge-commons-test"))
    testFixturesImplementation(libs.org.springframework.spring.test)
    testFixturesImplementation(libs.org.mockito.core)
    testFixturesImplementation(libs.org.mockito.kotlin)
    testFixturesImplementation(libs.com.googlecode.gson)

    testImplementation(project(":projectforge-commons-test"))
    testImplementation(libs.org.springframework.spring.test)
    testImplementation(libs.org.mockito.core)
    testImplementation(libs.org.mockito.junit.jupiter)
    testImplementation(libs.org.mockito.kotlin)
}

description = "projectforge-business"
