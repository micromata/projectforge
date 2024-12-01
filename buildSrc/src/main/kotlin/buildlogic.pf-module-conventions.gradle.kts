plugins {
    id("java-library")
    //`maven-publish`
}

repositories {
    mavenLocal()
    gradlePluginPortal() // Spring Boot Plugins are here.
    maven { url = uri("https://oss.sonatype.org/content/repositories/public/") }
    maven { url = uri("https://repo.maven.apache.org/maven2/") }
    maven { url = uri("https://raw.githubusercontent.com/gephi/gephi/mvn-thirdparty-repo/") }
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

configurations.all {
    resolutionStrategy {
        preferProjectModules() // Prioritize local modules.
    }
}

dependencies {
    api(libs.findLibrary("org-jetbrains-kotlin-stdlib").get())
    api(libs.findLibrary("io-github-microutils-kotlin-logging").get())
    testImplementation(libs.findLibrary("org-junit-jupiter-api").get())
    testImplementation(libs.findLibrary("org-junit-jupiter-engine").get())
    testImplementation(libs.findLibrary("org-junit-platform-launcher").get())
}
