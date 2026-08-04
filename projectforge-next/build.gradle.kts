plugins {
    id("com.github.node-gradle.node") version "7.1.0"
    id("base") // Adds the 'clean' task.
}

// projectforge-next is a Next.js app built to a static export (output: 'export').
// The build is packaged into the boot jar under /static/next and served by Spring
// side-by-side with the legacy React app (see WebApplicationConfig / Constants.NEXT_APP_PATH).
// This module contributes only build artifacts; it has no Kotlin/Java sources.

node {
    version.set("23.1.0")
    npmVersion.set("10.9.0")
    download.set(true)
    workDir.set(layout.projectDirectory.dir("node/nodejs"))
    npmWorkDir.set(layout.projectDirectory.dir("node/npm"))
    nodeProjectDir.set(file(layout.projectDirectory.dir(".").asFile.absolutePath))
}

tasks.named<Delete>("clean") {
    delete(
        file("node"),
        file("node_modules"),
        file("out"),
        file(".next"),
        layout.buildDirectory
    )
}

tasks {
    named<com.github.gradle.node.npm.task.NpmTask>("npmInstall") {
        group = "build"
        description = "Installs npm dependencies"
        args.set(listOf("install"))
        val nodeModulesDir = layout.projectDirectory.dir("node_modules")
        onlyIf {
            !nodeModulesDir.asFile.exists()
        }
        outputs.dir(project.layout.projectDirectory.dir("node_modules"))
    }

    register<com.github.gradle.node.npm.task.NpmTask>("npmBuild") {
        group = "build"
        description = "Builds the Next.js static export (output: 'export' -> out/)"
        args.set(listOf("run", "build"))
        dependsOn("npmInstall")

        inputs.files(fileTree("app"))
        inputs.files(fileTree("components"))
        inputs.files(fileTree("hooks"))
        inputs.files(fileTree("lib"))
        inputs.files(fileTree("store"))
        inputs.files(fileTree("i18n"))
        inputs.files(fileTree("messages"))
        inputs.files(fileTree("public"))
        inputs.file("next.config.ts")
        inputs.file("package.json")
        outputs.dir("out")
    }

    register<Copy>("copyNextBuild") {
        duplicatesStrategy = DuplicatesStrategy.EXCLUDE
        group = "build"
        description = "Copies the Next.js static export to /static/next in the build resources"
        dependsOn("npmBuild")
        from(file("out"))
        into(layout.buildDirectory.dir("resources/main/static/next"))
        inputs.dir("out")
        outputs.dir(layout.buildDirectory.dir("resources/main/static/next"))
    }

    register<Jar>("nextAppJar") {
        group = "build"
        description = "Package the Next.js static export as a JAR (served from /static/next)"
        archiveBaseName.set("projectforge-next")
        archiveVersion.set(project.version.toString())
        destinationDirectory.set(layout.buildDirectory.dir("libs"))

        from(file("out")) {
            into("static/next")
        }
        dependsOn("copyNextBuild")
    }

    named("build") {
        dependsOn("copyNextBuild")
    }
}

description = "projectforge-next"
