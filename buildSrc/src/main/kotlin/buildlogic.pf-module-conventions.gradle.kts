import org.gradle.api.artifacts.VersionCatalogsExtension

plugins {
    id("java-library")
    //`maven-publish`
}

repositories {
    mavenLocal()
    gradlePluginPortal() // Spring Boot Plugins are here.
    maven {
        url = uri("https://oss.sonatype.org/content/repositories/public/")
    }
    maven {
        url = uri("https://repo.maven.apache.org/maven2/")
    }
}

val libs = project.extensions.getByType<VersionCatalogsExtension>().named("libs")

// libs.versions etc. not available in buildSrc. Must use findVersion and findLibrary instead.

group = "org.projectforge"
version = libs.findVersion("org.projectforge").get().requiredVersion
java.sourceCompatibility = JavaVersion.VERSION_17

tasks.withType<JavaCompile>() {
    options.encoding = "UTF-8"
}

tasks.withType<Javadoc>() {
    options.encoding = "UTF-8"
}

dependencies {
    api(libs.findLibrary("org-jetbrains-kotlin-kotlin-stdlib").get())
    api(libs.findLibrary("io-github-microutils-kotlin-logging").get())
    api(libs.findLibrary("logback-classic").get()) {
        exclude(group = "org.slf4j", module = "slf4j-jul")
        exclude(group = "org.apache.logging.log4j", module = "log4j-to-slf4j")
    }
    testImplementation(libs.findLibrary("org-junit-jupiter-api").get())
    testImplementation(libs.findLibrary("org-junit-jupiter-engine").get())
    testImplementation(libs.findLibrary("org-junit-platform-launcher").get())

    /*    testImplementation("org.junit.vintage:junit-vintage-engine:5.10.5")
    testImplementation("org.mockito:mockito-core:5.12.0")
    testImplementation("org.mockito:mockito-junit-jupiter:5.12.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.4.0")
    compileOnly("org.slf4j:log4j-over-slf4j:2.0.16")*/
}
/*
publishing {
    publications.create<MavenPublication>("maven") {
        from(components["java"])
    }
}*/
