/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
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
 */
object ProjectForgeVersion {
    const val APP_ID = "ProjectForge"
    @JvmField
    val VERSION_STRING = ResourceBundle.getBundle("version").getString("version.number")
    @JvmField
    val VERSION = Version(VERSION_STRING)
    @JvmField
    val VERSION_NUMBER = VERSION.toString()
    @JvmField
    val BUILD_TIMESTAMP = ResourceBundle.getBundle("version").getString("version.buildDate")
    @JvmField
    val BUILD_DATE = BUILD_TIMESTAMP.split(" ".toRegex()).toTypedArray()[0]
    @JvmField
    val BUILD_YEAR = BUILD_TIMESTAMP.substring(0, 4)
    const val SCM = "git"
    @JvmField
    val SCM_BRANCH = ResourceBundle.getBundle("git").getString("git.branch") ?: "?develop?"
    @JvmField
    val SCM_COMMIT_ID = ResourceBundle.getBundle("git").getString("git.commit.id.abbrev") ?: "?abbrevId?"
    @JvmField
    val SCM_COMMIT_ID_FULL = ResourceBundle.getBundle("git").getString("git.commit.id.full") ?: "?fullId?"
    @JvmField
    val SCM_COMMIT_TIME = ResourceBundle.getBundle("git").getString("git.commit.time") ?: "2020-????"

    @JvmField
    val SCM_DIRTY = ResourceBundle.getBundle("git").getString("git.dirty") != "false"
    private val SCM_DIRTY_STRING = if (SCM_DIRTY) "*dirty*" else ""

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
}
