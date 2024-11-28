import org.springframework.boot.gradle.tasks.bundling.BootJar

plugins {
    id("org.springframework.boot") version "3.1.4"
    id("io.spring.dependency-management") version "1.1.3"
    java
}

springBoot {
    mainClass.set("org.projectforge.start.ProjectForgeApplication")
}

val projectVersion = libs.versions.org.projectforge.get() // Current version.
val kotlinVersion = libs.versions.org.jetbrains.kotlin.get() // Current version.
val kotlinxCoroutinesVersion = libs.versions.org.jetbrains.kotlinx.coroutines.core.get() // Current version.

val glassfishJaxbVersion = libs.versions.org.glassfish.jaxb.get()
val jacksonVersion = libs.versions.com.fasterxml.jackson.get()
val springVersion = libs.versions.org.springframework.spring.get()
val springBootVersion = libs.versions.org.springframework.boot.get()
val springSecurityVersion = libs.versions.org.springframework.security.get()
val apacheGroovyVersion = libs.versions.org.apache.groovy.get()
val kotlinCompilerDependency = configurations.create("kotlinCompilerDependency")
val kotlinCompilerDependencies = mutableListOf<String>()

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

    // Kotlin jars for scripting, must be extracted in fat jar.
    kotlinCompilerDependencies.add("org.jetbrains.kotlin:kotlin-reflect:$kotlinVersion")
    kotlinCompilerDependencies.add("org.jetbrains.kotlin:kotlin-stdlib:$kotlinVersion")
    kotlinCompilerDependencies.add("org.jetbrains.kotlin:kotlin-stdlib-common:$kotlinVersion")
    kotlinCompilerDependencies.add("org.jetbrains.kotlin:kotlin-compiler-embeddable:$kotlinVersion")
    kotlinCompilerDependencies.add("org.jetbrains.kotlin:kotlin-script-runtime:$kotlinVersion")
    kotlinCompilerDependencies.add("org.jetbrains.kotlin:kotlin-scripting-common:$kotlinVersion")
    kotlinCompilerDependencies.add("org.jetbrains.kotlin:kotlin-scripting-jvm:$kotlinVersion")
    kotlinCompilerDependencies.add("org.jetbrains.kotlin:kotlin-scripting-jvm-host:$kotlinVersion")
    kotlinCompilerDependencies.add("org.jetbrains.kotlin:kotlin-scripting-compiler-embeddable:$kotlinVersion")
    kotlinCompilerDependencies.add("org.jetbrains.kotlinx:kotlinx-coroutines-core-jvm:$kotlinxCoroutinesVersion")
    kotlinCompilerDependencies.forEach {
        kotlinCompilerDependency(it) // Add this dependency for handling and exclusion in bootJar task.
        implementation(it)           // Add this dependency for usage in the application.
    }

    // Force the following dependencies to avoid downgrades:
    implementation("org.apache.groovy:groovy:$apacheGroovyVersion")
    implementation("org.apache.groovy:groovy-ant:$apacheGroovyVersion")

    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-toml:$jacksonVersion")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jdk8:$jacksonVersion")
    implementation("com.fasterxml.jackson.module:jackson-module-parameter-names:$jacksonVersion")

    implementation("org.springframework.boot:spring-boot-autoconfigure:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-jdbc:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-json:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-reactor-netty:$springBootVersion")
    implementation("org.springframework.boot:spring-boot-starter-tomcat:$springBootVersion")
    implementation("org.springframework.data:spring-data-jpa:$springBootVersion") // springBoot!!!
    implementation("org.springframework.data:spring-data-commons:$springBootVersion") // springBoot!!!

    implementation("org.yaml:snakeyaml:2.3") // springboot
    implementation("com.zaxxer:HikariCP:5.0.1") // springboot
    implementation("org.springframework.security:spring-security-config:$springSecurityVersion")
    implementation("org.springframework.security:spring-security-core:$springSecurityVersion")
    implementation("org.springframework.security:spring-security-crypto:$springSecurityVersion")
    implementation("org.springframework.security:spring-security-web:$springSecurityVersion")
    implementation("org.springframework:spring-aop:$springVersion")
    implementation("org.springframework:spring-aspects:$springVersion")
    implementation("org.springframework:spring-beans:$springVersion")
    implementation("org.springframework:spring-core:$springVersion")
    implementation("org.springframework:spring-expression:$springVersion")
    implementation("org.springframework:spring-jcl:$springVersion")
    implementation("org.springframework:spring-jdbc:$springVersion")
    implementation("org.springframework:spring-web:$springVersion")
    implementation("org.springframework:spring-webflux:$springVersion")
    implementation("org.glassfish.jaxb:jaxb-core:$glassfishJaxbVersion")
    implementation("org.glassfish.jaxb:xsom:$glassfishJaxbVersion")

    implementation(libs.ch.qos.logback.classic)
    implementation(libs.ch.qos.logback.core)
    implementation(libs.com.fasterxml.jackson.core.annotations)
    implementation(libs.com.fasterxml.jackson.core)
    implementation(libs.com.fasterxml.jackson.core.databind)
    implementation(libs.com.fasterxml.jackson.dataformat.cbor)
    implementation(libs.com.fasterxml.jackson.datatype.jsr310)
    implementation(libs.com.fasterxml.jackson.module.kotlin)
    implementation(libs.com.google.zxing.core)
    implementation(libs.com.google.zxing.javase)
    implementation(libs.com.googlecode.ez.vcard)
    implementation(libs.com.googlecode.gson)
    implementation(libs.com.googlecode.json.simple)
    implementation(libs.com.googlecode.lanterna)
    implementation(libs.com.itextpdf)
    implementation(libs.com.thoughtworks.xstream)
    implementation(libs.com.webauthn4j.core)
    implementation(libs.com.webauthn4j.spring.security.core) {
        exclude("org.springframework")
        exclude("org.springframework.security")
        exclude("org.springframework.boot")
    }
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
    implementation(libs.org.jetbrains.kotlinx.coroutines.core)
    implementation(libs.org.jetbrains.kotlinx.coroutines.slf4j)
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
    implementation(libs.org.jfree.jfreechart)
    implementation(libs.org.mnode.ical4j.ical4j)
    implementation(libs.org.mozilla.rhino)
    implementation(libs.org.postgresql)
    implementation(libs.org.reflections)
    implementation(libs.org.slf4j.jcl.over.slf4j)
    implementation(libs.org.springframework.boot)
    implementation(libs.org.springframework.boot.dependencies)
    implementation(libs.org.springframework.boot.starter)
    implementation(libs.org.springframework.boot.starter.data.jpa)
    implementation(libs.org.springframework.boot.starter.json)
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

val kotlinCompilerDependencyFiles = kotlinCompilerDependency.map { it.name }
tasks.named<BootJar>("bootJar") {
    // println(kotlinCompilerDependencyFiles.joinToString())
    exclude(kotlinCompilerDependencyFiles.map { "**/$it" }) // Exclude this jar, it's extracted.
}

tasks.withType<Jar> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from({
        configurations.runtimeClasspath.get().filter {
            // println("it.name=${it.name}")
            kotlinCompilerDependencyFiles.any { file -> it.name.contains(file) }
        }.map { if (it.isDirectory) it else zipTree(it) } // Unpack the jar files.
    })
}

description = "projectforge-application"
