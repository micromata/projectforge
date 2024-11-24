plugins {
    id("buildlogic.pf-module-conventions")
}

dependencies {
    api(project(":projectforge-wicket"))
    api(project(":projectforge-rest"))
    testImplementation(testFixtures(project(":projectforge-business")))
}

description = "org.projectforge.plugins.todo"
