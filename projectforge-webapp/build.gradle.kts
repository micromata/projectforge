plugins {
    id("com.github.node-gradle.node") version "7.1.0"
}

node {
    // Configure the Node.js and npm versions
    // version.set("22.11.0")
    version.set("16.15.0") // Used for maven version.
    // Version of npm to use
    // If specified, installs it in the npmWorkDir
    // If empty, the plugin will use the npm command bundled with Node.js
    npmVersion.set("")
    download.set(true) // Downloads Node.js and npm instead of using a globally installed version

    // Set the directories for Node.js and npm installations using the modern Gradle API
    // Use a persistent directory outside the `build` folder
    workDir.set(layout.projectDirectory.dir(".gradle/nodejs")) // Directory for Node.js installation
    npmWorkDir.set(layout.projectDirectory.dir(".gradle/npm")) // Directory for npm installation
    // Explicitly set the Node.js binary path
    nodeProjectDir.set(file(layout.projectDirectory.dir("nodejs").asFile.absolutePath))
}

tasks {
    // Configure the existing npmInstall task instead of registering a new one
    named<com.github.gradle.node.npm.task.NpmTask>("npmInstall") {
        group = "build"
        description = "Installs npm dependencies"
        args.set(listOf("install"))
    }

    // Task to build the React project
    register<com.github.gradle.node.npm.task.NpmTask>("npmBuild") {
        group = "build"
        description = "Builds the React project"
        args.set(listOf("run", "build"))
        dependsOn("npmInstall") // Ensures `npm install` is executed before the build
    }

    // Task to copy the built React files
    register<Copy>("copyReactBuild") {
        group = "build"
        description = "Copies built React files to the target directory"
        dependsOn("npmBuild") // Depends on the React build process
        from(file("build")) // Directory where React outputs the build
        into(layout.buildDirectory.dir("resources/main/static")) // Target directory in the Gradle project
    }

    // Include the React build in the Gradle build process
    named("build") {
        dependsOn("copyReactBuild") // Makes React build a part of the Gradle build
    }
}

description = "projectforge-webapp"
