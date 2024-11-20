/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.scripting

import mu.KotlinLogging
import org.projectforge.common.FileUtils
import org.projectforge.framework.configuration.ConfigXml
import java.io.File
import java.net.URL
import java.net.URLClassLoader

private val log = KotlinLogging.logger {}

object KotlinClassLoaderWorkarround {
    class CustomClassLoader(externalJarUrls: Array<URL>) :
        URLClassLoader(externalJarUrls, Thread.currentThread().contextClassLoader) {

        init {
            log.debug { "CustomClassLoader: Created with classpath: ${externalJarUrls.joinToString()}" }
            log.debug { "CustomClassLoader: Search for META-INF/extensions/compiler.xml: ${this.getResource("META-INF/extensions/compiler.xml")}" }
        }

        override fun findResource(name: String): URL? {
            val url = super.findResource(name)
            log.debug { "CustomClassLoader: Looking for resource $name: $url" }
            return url
        }

        override fun findClass(name: String?): Class<*>? {
            log.debug { "CustomClassLoader: findClass $name" }
            return super.findClass(name)
        }

        override fun findClass(moduleName: String?, name: String?): Class<*>? {
            log.debug { "CustomClassLoader: findClass $moduleName: $name" }
            return super.findClass(moduleName, name)
        }

        override fun getResource(name: String): URL? {
            if (name == "META-INF/extensions/compiler.xml") {
                val url =
                    KotlinScriptJarExtractor.libDir.resolve("classes/META-INF/extensions/compiler.xml").toURI().toURL()
                log.debug { "CustomClassLoader: Loading resource $name: $url" }
                return url
            }
            val url = super.getResource(name)
            log.debug { "CustomClassLoader: Loading resource $name: $url" }
            return url
        }
    }

    val externalJars: List<File>
    val externalJarUrls: Array<URL>
    val classLoader: ClassLoader

    init {
        val baseDir = ConfigXml.getInstance().applicationHomeDir
        val libDir = FileUtils.createFile(baseDir, "resources", "kotlin-scripting")
        externalJars = listOf(
            File(libDir, "kotlin-compiler-embeddable-2.0.21.jar"),
            File(libDir, "kotlin-scripting-compiler-embeddable-2.0.21.jar"),
            File(libDir, "kotlin-scripting-jsr223-2.0.21.jar"),
            // File(libDir, "kotlin-scripting-jvm-2.0.21.jar"),
            // File(libDir, "kotlin-scripting-jvm-host-2.0.21.jar"),
            File(libDir, "kotlin-stdlib-2.0.21.jar"),
            File(libDir, "trove4j-1.0.20200330.jar"),
        )
        externalJars.forEach { jarFile ->
            if (!jarFile.exists()) {
                log.error { "JAR file doesn't exist (Kotlin scripts will not work): " + jarFile.getAbsolutePath() }
            }
        }
        val kotlinStdJar = externalJars.first { it.name.contains("kotlin-stdlib") }.absolutePath
        System.setProperty("kotlin.java.stdlib.jar", kotlinStdJar)
        log.info { "Setting system property kotlin.java.stdlib.jar=$kotlinStdJar" }
        externalJarUrls = externalJars.map { it.toURI().toURL() }.toTypedArray()
        log.info { "classpath=${externalJarUrls.joinToString(":")}" }
        classLoader = CustomClassLoader(externalJarUrls)
        Thread.currentThread().setContextClassLoader(classLoader)
        val clazz = Class.forName("kotlin.script.experimental.jvmhost.BasicJvmScriptingHost", true, classLoader)
        val host = clazz.getDeclaredConstructor().newInstance()
        log.debug { "ClassLoader of BasicJvmScriptingHost: ${host::class.java.classLoader}" }
    }
}
