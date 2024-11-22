plugins {
    id("org.springframework.boot") version libs.versions.org.springframework.boot.get()
    id("io.spring.dependency-management") version libs.versions.io.spring.dependency.management.get()
    id("java")
    id("application")
}

application {
    mainClass.set("org.projectforge.start.ProjectForgeApplication")
}

/*tasks.named<org.springframework.boot.gradle.tasks.bundling.BootJar>("bootJar") {
    archiveBaseName.set("ProjectForge-application")
    archiveVersion.set("8.0.0")
    archiveClassifier.set("")
}*/

dependencies {
    api(project(":projectforge-wicket"))
    api(project(":projectforge-rest"))
    api(project(":projectforge-webapp"))
    api(project(":projectforge-caldav"))
    api(libs.org.springframework.boot.starter)
    api(libs.org.springframework.boot.starter.web)
    api(libs.com.googlecode.lanterna)
    api(libs.org.jetbrains.kotlin.kotlin.stdlib)
    api(libs.org.reflections.reflections)
    runtimeOnly(project(":org.projectforge.plugins.datatransfer"))
    runtimeOnly(project(":org.projectforge.plugins.ihk"))
    runtimeOnly(project(":org.projectforge.plugins.licensemanagement"))
    runtimeOnly(project(":org.projectforge.plugins.liquidityplanning"))
    runtimeOnly(project(":org.projectforge.plugins.marketing"))
    runtimeOnly(project(":org.projectforge.plugins.memo"))
    runtimeOnly(project(":org.projectforge.plugins.merlin"))
    runtimeOnly(project(":org.projectforge.plugins.banking"))
    runtimeOnly(project(":org.projectforge.plugins.skillmatrix"))
    runtimeOnly(project(":org.projectforge.plugins.todo"))
    testImplementation(project(":projectforge-commons-test"))
    testImplementation(libs.org.mockito.core)
}

description = "projectforge-application"
