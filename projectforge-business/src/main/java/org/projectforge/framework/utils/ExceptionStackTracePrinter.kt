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

package org.projectforge.framework.utils

/**
 * Prints stack-trace without foreign packages in much shorter form than log.error(ex.message, ex) does.
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
object ExceptionStackTracePrinter {

    /**
     * @param showExceptionMessage If true, the exception message itself will be prepended to stack trace.
     * @param stopBeforeForeignPackages If true, after showing stack trace elements of own package 'org.projectforge' any further element is hidden if foreign stack element is reached.
     * @param depth Maximum depth of stack trace elements to show (default is 10).
     * @param showPackagesOnly Only stack elements started with one of these string is classified as own package to print. If nothing given, all 'org.projectforge' classes are
     * classified as own packages.
     */
    @JvmStatic
    @JvmOverloads
    fun toString(ex: Exception, showExceptionMessage: Boolean = true, stopBeforeForeignPackages: Boolean = true, depth: Int = 10, vararg showPackagesOnly: String): String {
        val sb = StringBuilder()
        if (showExceptionMessage) {
            sb.append(ex::class.java.name).append(":").append(ex.message).append("\n")
        }
        var counter = 0
        val showPackages = if (showPackagesOnly.isEmpty()) PROJECTFORGE_PACKAGES else showPackagesOnly
        var placeHolderPrinted = false
        var ownStackelementsPrinted = false
        for (element in ex.stackTrace) {
            val ownPackage = showPackages.any { element.className.startsWith(it) }
            if (!ownPackage) {
                if (ownStackelementsPrinted) {
                    if (stopBeforeForeignPackages) {
                        sb.append("...following foreign packages are hidden...\n")
                        break
                    }
                    if (!placeHolderPrinted) {
                        sb.append("...(foreign packages are hidden)...\n")
                        placeHolderPrinted = true
                    }
                    continue // Don't show foreign class entries.
                }
            } else {
                ownStackelementsPrinted = true
            }
            sb.append("at ${element.className}.${element.methodName} (${element.fileName}:${element.lineNumber})\n")
            if (++counter >= depth) {
                break
            }
        }
        return sb.toString()
    }

    val PROJECTFORGE_PACKAGES = arrayOf("org.projectforge.")
}
