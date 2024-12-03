rootProject.name = "projectforge-parent"

gradle.rootProject {
    gradle.startParameter.isParallelProjectExecutionEnabled = true
}

include(":projectforge-application")
include(":projectforge-business")
include(":projectforge-common")
include(":projectforge-commons-test")
include(":projectforge-carddav")
include(":projectforge-jcr")
include(":projectforge-model")
include(":projectforge-rest")
include(":projectforge-webapp")
include(":projectforge-wicket")
include(":org.projectforge.plugins.banking")
include(":org.projectforge.plugins.datatransfer")
include(":org.projectforge.plugins.liquidityplanning")
include(":org.projectforge.plugins.ihk")
include(":org.projectforge.plugins.marketing")
include(":org.projectforge.plugins.memo")
include(":org.projectforge.plugins.merlin")
include(":org.projectforge.plugins.todo")
include(":org.projectforge.plugins.licensemanagement")
include(":org.projectforge.plugins.skillmatrix")

project(":org.projectforge.plugins.banking").projectDir = file("plugins/org.projectforge.plugins.banking")
project(":org.projectforge.plugins.datatransfer").projectDir = file("plugins/org.projectforge.plugins.datatransfer")
project(":org.projectforge.plugins.licensemanagement").projectDir = file("plugins/org.projectforge.plugins.licensemanagement")
project(":org.projectforge.plugins.liquidityplanning").projectDir = file("plugins/org.projectforge.plugins.liquidityplanning")
project(":org.projectforge.plugins.ihk").projectDir = file("plugins/org.projectforge.plugins.ihk")
project(":org.projectforge.plugins.marketing").projectDir = file("plugins/org.projectforge.plugins.marketing")
project(":org.projectforge.plugins.memo").projectDir = file("plugins/org.projectforge.plugins.memo")
project(":org.projectforge.plugins.merlin").projectDir = file("plugins/org.projectforge.plugins.merlin")
project(":org.projectforge.plugins.skillmatrix").projectDir = file("plugins/org.projectforge.plugins.skillmatrix")
project(":org.projectforge.plugins.todo").projectDir = file("plugins/org.projectforge.plugins.todo")
