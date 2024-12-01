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

package org.projectforge

import java.util.*

/**
 * Version information of ProjectForge containing build date, version number as well as scm information.
 *
 * Create a new version in all pom.xml files by executing following commands in ProjectForge's main directory:
 *   export PF_NEW_VERSION=7.6-SNAPSHOT
 *   find . -name pom.xml | xargs perl -pi -e 's|<version>.*</version><!-- projectforge.version -->|<version>$ENV{PF_NEW_VERSION}</version><!-- projectforge.version -->|g'
 *
 */
object ProjectForgeVersion {
    const val APP_ID = "ProjectForge"
    @JvmField
    val VERSION_STRING = getBuildResource("gradle.version")
    @JvmField
    val VERSION = Version(VERSION_STRING)
    @JvmField
    val VERSION_NUMBER = VERSION.toString()
    @JvmField
    val BUILD_TIMESTAMP = getBuildResource("gradle.build.timestamp")
    @JvmField
    val BUILD_DATE = getBuildResource("gradle.build.date")
    @JvmField
    val BUILD_YEAR = BUILD_TIMESTAMP.substring(0, 4)
    const val SCM = "git"
    @JvmField
    val SCM_BRANCH = getBuildResource("git.branch")
    @JvmField
    val SCM_COMMIT_ID = getBuildResource("git.commit.id.abbrev")
    @JvmField
    val SCM_COMMIT_ID_FULL = getBuildResource("git.commit.id.full")
    @JvmField
    val SCM_COMMIT_TIME = getBuildResource("git.commit.time", "2021-?git.commit.time?")

    @JvmField
    val SCM_DIRTY = getBuildResource("git.dirty") != "false"
    private val SCM_DIRTY_STRING = if (SCM_DIRTY) "*" else ""

    @JvmField
    val SCM_ID = "$SCM_BRANCH@$SCM_COMMIT_ID$SCM_DIRTY_STRING"
    @JvmField
    val SCM_ID_FULL = "$SCM_BRANCH@$SCM_COMMIT_ID_FULL$SCM_DIRTY_STRING"

    /**
     * Year of scm commit.
     */
    @JvmField
    val YEAR = SCM_COMMIT_TIME.substring(0, 4)

    /**
     * From 2001 until year of scm commit timestamp.
     */
    val COPYRIGHT_YEARS = "2001-$YEAR"

    private fun getBuildResource(key: String, defaultString: String = "?$key?"): String {
        try {
            return ResourceBundle.getBundle("build").getString(key) ?: defaultString
        } catch (ex: Exception) {
            // Should only be OK in develop mode (maven didn't generate the git.properties file yet).
            return defaultString
        }
    }
}
