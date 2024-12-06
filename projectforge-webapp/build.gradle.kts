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
    delete(layout.buildDirectory)
}

tasks {
    // Configure the existing npmInstall task instead of registering a new one
    named<com.github.gradle.node.npm.task.NpmTask>("npmInstall") {
        group = "build"
        description = "Installs npm dependencies"
        args.set(listOf("install"))
        // Skip task if node_modules exists
        val nodeModulesDir = layout.projectDirectory.dir("node_modules")
        onlyIf {
            !nodeModulesDir.asFile.exists()
        }
        outputs.dir(project.layout.projectDirectory.dir("node_modules"))
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
        // Use the provider API to configure paths
        val buildDirProvider = layout.projectDirectory.dir("build")
        val outputDirProvider = layout.buildDirectory.dir("resources/main/static")
        // Define dependencies
        dependsOn("npmBuild")
        mustRunAfter("processResources", "processTestResources")
        from(buildDirProvider) {
            exclude("resources/main/static/**")
        }
        from(layout.projectDirectory.dir("src")) {
            include("index.html")
        }
        into(outputDirProvider)
        // Declare inputs and outputs correctly
        inputs.dir(buildDirProvider)
        outputs.dir(outputDirProvider)
        // Check when the task is up-to-date
        outputs.upToDateWhen {
            outputDirProvider.get().asFile.exists()
        }
        // Only run if necessary
        onlyIf {
            val outputDir = outputDirProvider.get().asFile
            val sourceDir = buildDirProvider.asFile

            !outputDir.exists() || outputDir.lastModified() < sourceDir.lastModified()
        }
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
