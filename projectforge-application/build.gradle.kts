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

dependencies {
    implementation(project(":projectforge-wicket"))
    implementation(project(":projectforge-rest"))
    implementation(project(":projectforge-webapp"))
    implementation(project(":projectforge-caldav"))
    implementation(libs.org.springframework.boot.starter)
    implementation(libs.org.springframework.boot.starter.web)
    implementation(libs.com.googlecode.lanterna)
    implementation(libs.org.reflections.reflections)
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
}

springBoot {
    mainClass.set("org.projectforge.start.ProjectForgeApplication")
}

val projectVersion = libs.versions.org.projectforge.get() // Current version.

tasks.bootJar {
    dependsOn(":projectforge-webapp:copyReactBuild") // Integrate react-build in fat jar.
    archiveFileName.set("projectforge-application-$projectVersion.jar")
}

description = "projectforge-application"
