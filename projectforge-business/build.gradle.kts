import org.gradle.kotlin.dsl.testImplementation

plugins {
    id("buildlogic.java-conventions")
    id("org.springframework.boot") version libs.versions.org.springframework.boot.get()
    id("io.spring.dependency-management") version libs.versions.io.spring.dependency.management.get()
    id("java")
}

tasks.named("compileKotlin") {
    dependsOn("xjc")
}

tasks.register<JavaExec>("xjc") {
    group = "code generation"
    description = "Generate Java classes from XSD schema"

    mainClass.set("com.sun.tools.xjc.XJCFacade")
    classpath = configurations.compileClasspath.get()
    val outputDir = layout.buildDirectory.dir("generated-sources/xjc").get().asFile
    args = listOf(
        "-d", outputDir.absolutePath, // Output dir
        "-p", "org.projectforge.generated",           // Destination package
        "src/main/resources/misc/pain.001.003.03.xsd" // Schema file
    )
    doFirst {
        if (!outputDir.exists()) {
            outputDir.mkdirs()
        }
    }
}

sourceSets {
    main {
        java {
            srcDir("$buildDir/generated-sources/xjc")
        }
    }
}

//    implementation("com.sun.xml.bind:jaxb-impl:3.0.2")
//    implementation("com.sun.xml.bind:jaxb-xjc:3.0.2")

dependencies {
    api(project(":projectforge-common"))
    api(project(":projectforge-jcr"))
    api(project(":projectforge-model"))
    api(libs.de.micromata.merlin.merlin.core)
    api(libs.com.sun.xml.bind.jaxb.impl)
    api(libs.com.sun.xml.bind.jaxb.xjc)
    api(libs.com.googlecode.json.simple.json.simple)
    api(libs.org.aspectj.aspectjtools)
    api(libs.org.hibernate.search.hibernate.search.mapper.orm)
    api(libs.org.hibernate.search.hibernate.search.backend.lucene)
    api(libs.jakarta.persistence.jakarta.persistence.api)
    api(libs.jakarta.xml.bind.api)
    api(libs.org.springframework.boot.starter)
    api(libs.org.jetbrains.kotlin.kotlin.stdlib)
    api(libs.org.jetbrains.kotlin.kotlin.compiler.embeddable)
    api(libs.org.jetbrains.kotlin.kotlin.scripting.jsr223)
    api(libs.org.jetbrains.kotlin.kotlin.scripting.compiler.embeddable)
    api(libs.org.jetbrains.kotlin.kotlin.scripting.common)
    api(libs.org.jetbrains.kotlin.kotlin.scripting.jvm)
    api(libs.org.jetbrains.kotlin.kotlin.scripting.jvm.host)
    api(libs.org.jetbrains.kotlinx.kotlinx.coroutines.slf4j)
    api(libs.org.springframework.spring.tx)
    api(libs.org.springframework.spring.context)
    api(libs.org.springframework.spring.orm)
    api(libs.org.apache.commons.commons.collections4)
    api(libs.commons.io.commons.io)
    api(libs.org.bouncycastle.bcprov.jdk18on)
    api(libs.org.dom4j.dom4j)
    api(libs.org.apache.httpcomponents.client5.httpclient5)
    api(libs.org.flywaydb.flyway.core)
    api(libs.org.flywaydb.flyway.database.postgresql)
    api(libs.org.postgresql.postgresql)
    api(libs.org.hsqldb.hsqldb)
    api(libs.jakarta.mail.jakarta.mail.api)
    api(libs.jakarta.activation.jakarta.activation.api)
    api(libs.org.mnode.ical4j.ical4j)
    api(libs.com.googlecode.ez.vcard.ez.vcard)
    api(libs.com.thoughtworks.xstream.xstream)
    api(libs.com.itextpdf.itextpdf)
    api(libs.com.sun.xml.bind.jaxb.impl)
    api(libs.org.apache.groovy.groovy.all)
    api(libs.org.jfree.jfreechart)
    api(libs.net.sourceforge.mpxj)
    api(libs.org.apache.xmlgraphics.fop)
    api(libs.org.apache.xmlgraphics.xmlgraphics.commons)
    api(libs.org.apache.xmlgraphics.batik.codec)
    api(libs.org.apache.xmlgraphics.batik.constants)
    api(libs.org.apache.xmlgraphics.batik.dom)
    api(libs.org.apache.xmlgraphics.batik.svg.dom)
    api(libs.com.fasterxml.jackson.module.jackson.module.kotlin)
    api(libs.com.fasterxml.jackson.core.jackson.annotations)
    api(libs.com.fasterxml.jackson.core.jackson.databind)
    api(libs.com.fasterxml.jackson.datatype.jackson.datatype.jsr310)
    api(libs.org.apache.poi.poi)
    api(libs.org.apache.poi.poi.ooxml)
    api(libs.org.apache.commons.commons.text)
    api(libs.se.sawano.java.alphanumeric.comparator)
    api(libs.com.webauthn4j.webauthn4j.core)
    api(libs.com.webauthn4j.webauthn4j.spring.security.core)
    testImplementation(project(":projectforge-business-test"))
    testImplementation(project(":projectforge-commons-test"))
    testImplementation(libs.org.apache.directory.server.apacheds.server.integ)
    testImplementation(libs.org.mock.server.mockserver.netty.no.dependencies)
    testImplementation(libs.com.google.code.gson.gson)
    testImplementation(libs.org.springframework.boot.starter.test)
    compileOnly(libs.jakarta.servlet.jakarta.servlet.api)
    compileOnly(libs.org.springframework.boot.starter.data.jpa)
    compileOnly(libs.com.zaxxer.hikaricp)
    compileOnly(libs.jakarta.annotation.jakarta.annotation.api)
}

description = "projectforge-business"
