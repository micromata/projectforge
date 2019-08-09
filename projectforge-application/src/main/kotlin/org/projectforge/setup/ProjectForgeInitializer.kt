/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.setup

import org.projectforge.ProjectForgeApp
import org.projectforge.common.CanonicalFileUtils
import org.projectforge.common.EmphasizedLogSupport
import org.projectforge.common.StringModifier
import org.projectforge.framework.configuration.ConfigXml
import org.projectforge.framework.persistence.attr.impl.AttrSchemaServiceSpringBeanImpl
import org.projectforge.start.ProjectForgeApplication
import org.projectforge.start.ProjectForgeApplication.giveUpAndSystemExit
import org.projectforge.start.ProjectForgeHomeFinder
import java.io.File
import java.util.regex.Matcher

object ProjectForgeInitializer {
    private val log = org.slf4j.LoggerFactory.getLogger(ProjectForgeInitializer::class.java)

    @JvmStatic
    fun initialize(setupData: SetupData?): File? {
        val applicationHomeDir = setupData?.applicationHomeDir
                ?: return giveUpAndSystemExit("No directory configured in wizard.")

        if (ProjectForgeHomeFinder.isProjectForgeSourceCodeRepository(applicationHomeDir)) {
            giveUpAndSystemExit("ProjectForge shouldn't use source code repository as home directory: $applicationHomeDir")
        }

        val emphasizedLog = EmphasizedLogSupport(log, EmphasizedLogSupport.Priority.NORMAL, EmphasizedLogSupport.Alignment.LEFT)
                .log("Checking ProjectForge installation...")

        var counter = 0
        if (!applicationHomeDir.exists()) {
            emphasizedLog.log("  ${++counter}. Creating directory: ${CanonicalFileUtils.absolutePath(applicationHomeDir)}...")
            applicationHomeDir.mkdirs()
            if (!applicationHomeDir.exists() && !applicationHomeDir.isDirectory) {
                emphasizedLog.log("    Error while creating directory: ${CanonicalFileUtils.absolutePath(applicationHomeDir)}").logEnd()
                giveUpAndSystemExit("Error while creating directory: ${CanonicalFileUtils.absolutePath(applicationHomeDir)}")
            }
        }

        val serverPort = if (setupData.serverPort in 1..65535) setupData.serverPort else 8080

        counter = ensureConfigFile(applicationHomeDir,
                ProjectForgeApplication.CLASSPATH_INITIAL_PROPERTIES_FILENAME, ProjectForgeApplication.PROPERTIES_FILENAME, counter, emphasizedLog,
                StringModifier {
                    var result = replace(it, "server.port", "$serverPort")
                    result = replace(result, "projectforge.domain", setupData.domain)
                    result = replace(result, "projectforge.currencySymbol", setupData.currencySymbol)
                    result = replace(result, "projectforge.defaultLocale", setupData.defaultLocale)
                    result = replace(result, "projectforge.defaultTimeNotation", setupData.defaultTimeNotation)
                    result = replace(result, "projectforge.defaultFirstDayOfWeek", setupData.defaultFirstDayOfWeek)
                    if (setupData.developmentMode) {
                        result = replace(result, "projectforge.web.development.enableCORSFilter", "true")
                    }
                    val jdbc = setupData.jdbcSettings
                    if (!setupData.useEmbeddedDatabase && jdbc != null) {
                        result = replace(result, "spring.datasource.url", jdbc.jdbcUrl)
                        result = replace(result, "spring.datasource.username", jdbc.user)
                        result = replace(result, "spring.datasource.password", jdbc.password)
                        result = replace(result, "spring.datasource.driver-class-name","org.postgresql.Driver")
                    }
                    result
                }
        )
        counter = ensureConfigFile(applicationHomeDir,
                ConfigXml.CLASSPATH_INITIAL_CONFIG_XML_FILE, ConfigXml.CONFIG_XML_FILE, counter, emphasizedLog)
        counter = ensureConfigFile(applicationHomeDir,
                AttrSchemaServiceSpringBeanImpl.CLASSPATH_INITIAL_ATTR_SCHEMA_CONFIG_FILE, AttrSchemaServiceSpringBeanImpl.ATTR_SCHEMA_CONFIG_FILE, counter, emphasizedLog)
        emphasizedLog.logEnd()
        if (!setupData.startServer) {
            giveUpAndSystemExit("Initialization of ProjectForge's home directory done. Autostart wasn't selected. Please restart the server manually.")
        }
        return setupData.applicationHomeDir
    }

    private fun replace(text: String, property: String, value: Any?): String {
        return text.replaceFirst("^#?+$property=.*$".toRegex(RegexOption.MULTILINE), Matcher.quoteReplacement("$property=$value"))
    }

    private fun ensureConfigFile(baseDir: File, initialClasspathFilename: String, filename: String, counter: Int,
                                 emphasizedLog: EmphasizedLogSupport, modifier: StringModifier? = null): Int {
        if (File(baseDir, filename).exists())
            return counter
        emphasizedLog.log("  ${counter}. Creating config file: $filename...")
        if (!ProjectForgeApp.ensureInitialConfigFile(baseDir, initialClasspathFilename, filename, false, modifier)) {
            emphasizedLog.logEnd()
            giveUpAndSystemExit("Error while creating config file '$filename'.")
        }
        return counter + 1
    }
}
