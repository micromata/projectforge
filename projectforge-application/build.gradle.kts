import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot")
    id("io.spring.dependency-management")
    java
}

/*tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveBaseName.set("ProjectForge-application")
    archiveVersion.set("8.0.0")
    archiveClassifier.set("")
}*/

springBoot {
    mainClass.set("org.projectforge.start.ProjectForgeApplication")
}

val projectVersion = libs.versions.org.projectforge.get() // Current version.

tasks.named<BootJar>("bootJar") {
    dependsOn(":projectforge-webapp:copyReactBuild") // Integrate react.build in fat jar.
    // Doesn't work:
    /*requiresUnpack {
        val name = it.name
        val matches = name.startsWith("kotlin-scripting-jsr223") ||
                name.startsWith("kotlin-compiler-embeddable")
        if (matches) {
            println("****** Hurzel: $name: $matches")
        }
        matches
    }*/
    archiveFileName.set("projectforge-application.$projectVersion.jar")
}

//val kotlinCompilerDependency = configurations.create("kotlinCompilerDependency")

/*
tasks.register<Copy>("unpackKotlinDependencies") {
    from(zipTree(file("path/to/kotlin-compiler-embeddable-2.0.21.jar")))
    into(layout.buildDirectory.dir("unpacked-libs"))
}

tasks.named<BootJar>("bootJar") {
    dependsOn("unpackKotlinDependencies")
    from(layout.buildDirectory.dir("unpacked-libs")) {
        into("BOOT-INF/lib")
    }
}*/

/*tasks.register("install") {
    group = "build" // Optional: set a group for tasks.
    description = "Alias for bootJar"
    dependsOn("bootJar")
}*/

dependencies {
    implementation(project(":projectforge-wicket"))
    implementation(project(":projectforge-rest"))
    implementation(project(":projectforge-caldav"))
    implementation(project(":projectforge-webapp"))
    implementation(project(":org.projectforge.plugins.datatransfer"))
    implementation(project(":org.projectforge.plugins.ihk"))
    implementation(project(":org.projectforge.plugins.licensemanagement"))
    implementation(project(":org.projectforge.plugins.liquidityplanning"))
    implementation(project(":org.projectforge.plugins.marketing"))
    implementation(project(":org.projectforge.plugins.memo"))
    implementation(project(":org.projectforge.plugins.merlin"))
    implementation(project(":org.projectforge.plugins.banking"))
    implementation(project(":org.projectforge.plugins.skillmatrix"))
    implementation(project(":org.projectforge.plugins.todo"))
    testImplementation(project(":projectforge-commons-test"))
    testImplementation(libs.org.mockito.core)

   /* kotlinCompilerDependency(libs.org.jetbrains.kotlin.compiler.embeddable) {
        isTransitive = false // Load only this dependency, not its dependencies.
    }*/

    // Force the following dependencies to avoid downgrade:
    implementation(libs.com.fasterxml.jackson.core.annotations)
    implementation(libs.com.fasterxml.jackson.core)
    implementation(libs.com.fasterxml.jackson.core.databind)
    implementation(libs.com.fasterxml.jackson.datatype.jsr310)
    implementation(libs.com.fasterxml.jackson.module.kotlin)
    implementation(libs.com.google.code.gson)
    implementation(libs.com.google.zxing.core)
    implementation(libs.com.google.zxing.javase)
    implementation(libs.com.googlecode.ez.vcard)
    implementation(libs.com.googlecode.json.simple)
    implementation(libs.com.googlecode.lanterna)
    implementation(libs.com.itextpdf)
    implementation(libs.com.thoughtworks.xstream)
    implementation(libs.com.webauthn4j.core)
    implementation(libs.com.webauthn4j.spring.security.core)
    implementation(libs.com.zaxxer.hikaricp)
    implementation(libs.commons.beanutils)
    implementation(libs.commons.io)
    implementation(libs.de.micromata.merlin.core)
    implementation(libs.fr.opensagres.xdocrepor.poi.xwpf.converter.pdf)
    implementation(libs.io.dropwizard.metrics.core)
    implementation(libs.io.milton.server.ent)
    implementation(libs.io.github.microutils.kotlin.logging)
    implementation(libs.jakarta.activation.api)
    implementation(libs.jakarta.annotation.api)
    implementation(libs.jakarta.mail.jakarta.mail.api)
    implementation(libs.jakarta.persistence.api)
    implementation(libs.jakarta.servlet.api)
    implementation(libs.jakarta.servlet.jsp.api)
    implementation(libs.jakarta.validation.api)
    implementation(libs.jakarta.ws.rs.api)
    implementation(libs.jakarta.xml.bind.api)
    implementation(libs.javax.jcr)
    implementation(libs.jboss.logging)
    implementation(libs.joda.time.joda.time)
    implementation(libs.logback.classic)
    implementation(libs.net.lingala.zip4j.zip4j)
    implementation(libs.net.sourceforge.mpxj)
    implementation(libs.org.apache.commons.collections4)
    implementation(libs.org.apache.commons.lang3)
    implementation(libs.org.apache.commons.text)
    implementation(libs.org.apache.groovy.all)
    implementation(libs.org.apache.httpcomponents.client5.httpclient5)
    implementation(libs.org.apache.jackrabbit.oak.jcr)
    implementation(libs.org.apache.jackrabbit.oak.segment.tar)
    implementation(libs.org.apache.poi)
    implementation(libs.org.apache.poi.ooxml)
    implementation(libs.org.apache.wicket.myextensions)
    implementation(libs.org.apache.wicket.spring)
    implementation(libs.org.apache.xmlgraphics.batik.codec)
    implementation(libs.org.apache.xmlgraphics.batik.constants)
    implementation(libs.org.apache.xmlgraphics.batik.dom)
    implementation(libs.org.apache.xmlgraphics.batik.svg.dom)
    implementation(libs.org.apache.xmlgraphics.fop)
    implementation(libs.org.apache.xmlgraphics.commons)
    implementation(libs.org.aspectj.aspectjtools)
    implementation(libs.org.bouncycastle.bcprov.jdk18on)
    implementation(libs.org.dom4j)
    implementation(libs.org.flywaydb.core)
    implementation(libs.org.flywaydb.database.postgresql)
    implementation(libs.org.glassfish.jaxb.runtime)
    implementation(libs.org.glassfish.jaxb.xjc)
    implementation(libs.org.hibernate.orm.core)
    implementation(libs.org.hibernate.search.backend.lucene)
    implementation(libs.org.hibernate.search.mapper.orm)
    implementation(libs.org.hsqldb.hsqldb)
    implementation(libs.org.jetbrains.kotlin.reflect)
    implementation(libs.org.jetbrains.kotlin.scripting.common)
    implementation(libs.org.jetbrains.kotlin.scripting.compiler.embeddable)
    implementation(libs.org.jetbrains.kotlin.scripting.jsr223)
    implementation(libs.org.jetbrains.kotlin.scripting.jvm)
    implementation(libs.org.jetbrains.kotlin.scripting.jvm.host)
    implementation(libs.org.jetbrains.kotlin.stdlib)
    implementation(libs.org.jetbrains.kotlinx.coroutines.slf4j)
    implementation(libs.org.jfree.jfreechart)
    implementation(libs.org.mnode.ical4j.ical4j)
    implementation(libs.org.mozilla.rhino)
    implementation(libs.org.postgresql)
    implementation(libs.org.reflections)
    implementation(libs.org.slf4j.jcl.over.slf4j)
    implementation(libs.org.springframework.boot.starter)
    implementation(libs.org.springframework.boot.starter.data.jpa)
    implementation(libs.org.springframework.boot.starter.jersey)
    implementation(libs.org.springframework.boot.starter.logging)
    implementation(libs.org.springframework.boot.starter.web)
    implementation(libs.org.springframework.boot.starter.webflux)
    implementation(libs.org.springframework.spring.context)
    implementation(libs.org.springframework.spring.orm)
    implementation(libs.org.springframework.spring.tx)
    implementation(libs.org.springframework.spring.webmvc)
    implementation(libs.org.wicketstuff.html5)
    implementation(libs.org.wicketstuff.select2)
    implementation(libs.se.sawano.java.alphanumeric.comparator)
}

description = "projectforge-application"
