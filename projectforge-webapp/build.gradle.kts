plugins {
    id("com.github.node-gradle.node") version "7.1.0"
}

node {
    // Configure the Node.js and npm versions
    // version.set("22.11.0")
    version.set("16.15.0") // Used by maven version (ProjectForge 7.5).
    // Version of npm to use
    // If specified, installs it in the npmWorkDir
    npmVersion.set("8.5.4") // Used by maven version (ProjectForge 7.5).
    // npmVersion.set("") // If empty, the plugin will use the npm command bundled with Node.js
    download.set(true) // Downloads Node.js and npm instead of using a globally installed version

    // Set the directories for Node.js and npm installations using the modern Gradle API
    // Use a persistent directory outside the `build` folder
    workDir.set(layout.projectDirectory.dir("node/nodejs")) // Directory for Node.js installation
    npmWorkDir.set(layout.projectDirectory.dir("node/npm")) // Directory for npm installation
    // Explicitly set the Node.js binary path
    nodeProjectDir.set(file(layout.projectDirectory.dir(".").asFile.absolutePath))
}

tasks.named<Delete>("clean") {
    delete(
        file("node"),  // Delete download directory of node and npm.
        file("node_modules")
    )
}

tasks {
    // Configure the existing npmInstall task instead of registering a new one
    named<com.github.gradle.node.npm.task.NpmTask>("npmInstall") {
        group = "build"
        description = "Installs npm dependencies"
        args.set(listOf("install"))
        // Skip task if node_modules exists
        /*onlyIf {
            !file("node_modules").exists()
        }
        outputs.dir("node_modules") // Mark node_modules as output*/
    }

    // Task to build the React project
    register<com.github.gradle.node.npm.task.NpmTask>("npmBuild") {
        group = "build"
        description = "Builds the React project"
        args.set(listOf("run", "build"))
        dependsOn("npmInstall") // Ensures `npm install` is executed before the build
        // Skip task if the React build directory is up-to-date
        inputs.files(fileTree("src")) // All source files as input
        outputs.dir("build") // React output directory as output
    }

    // Task to copy the built React files
    register<Copy>("copyReactBuild") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        group = "build"
        description = "Copies built React files to the target directory"
        dependsOn("npmBuild") // Depends on the React build process
        dependsOn("compileJava") // Ensure Java compilation is complete
        dependsOn("processResources") // Ensure resources are processed
        dependsOn("processTestResources") // Ensure test resources are processed
        mustRunAfter("processResources", "processTestResources") // Ensure resources are processed before copying
        from(file("build")) // Directory where React outputs the build
        into(layout.buildDirectory.dir("resources/main/static")) // Target directory in the Gradle project
        // Skip task if target directory is up-to-date
        inputs.dir("build") // React build directory as input
        outputs.dir(layout.buildDirectory.dir("resources/main/static")) // Static resources directory as output
    }

    // Include the React build in the Gradle build process
    named("build") {
        dependsOn("copyReactBuild") // Makes React build a part of the Gradle build
    }

    // Add explicit dependencies to fix task ordering issues
    named<Jar>("jar") {
        dependsOn("copyReactBuild") // Ensure the jar task waits for copyReactBuild
    }

    named("build") {
        dependsOn("copyReactBuild") // Ensure build task includes copyReactBuild
    }
}

description = "projectforge-webapp"
