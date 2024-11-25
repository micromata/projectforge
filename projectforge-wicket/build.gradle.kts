plugins {
    id("buildlogic.pf-module-conventions")
}

dependencies {
    api(project(":projectforge-common"))
    api(project(":projectforge-business"))
    api(project(":projectforge-rest"))
    api(libs.org.apache.wicket.myextensions)
    api(libs.org.apache.wicket.spring)
    api(libs.org.apache.wicket.tester)
    api(libs.jakarta.servlet.jsp.api)
    api(libs.org.jetbrains.kotlin.stdlib)
    api(libs.org.wicketstuff.html5)
    api(libs.org.wicketstuff.select2)
    api(libs.com.google.code.gson)
    api(libs.org.mozilla.rhino)
    testImplementation(project(":projectforge-commons-test"))
    testImplementation(testFixtures(project(":projectforge-business")))
    compileOnly(libs.jakarta.servlet.api)
}

sourceSets {
    main {
        java.srcDirs("src/main/java", "src/main/kotlin")
        resources.srcDirs("src/main/resources", "src/main/java", "src/main/webapp")
    }
    test {
        java.srcDirs("src/test/java", "src/test/kotlin")
    }
}

tasks.withType<ProcessResources> {
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    from("src/main/java") {
        include("**/*.css", "**/*.html", "**/*.js", "**/*.js.template", "**/*.tpl")
    }
    from("src/main/webapp") {
        into("static")
    }
}

description = "projectforge-wicket"
