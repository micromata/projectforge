/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

/**
 * One former version of a script's code (see [ScriptDao.getBackups]).
 *
 * @param key Names the version for [ScriptDao.readBackup]: the backup file's name, or [DB_KEY] for the
 *   version kept in [ScriptDO.scriptBackup].
 * @param timestamp When this version was replaced, in epoch millis; null where that is not known (the
 *   version in the database).
 */
class ScriptBackupInfo(val key: String, val timestamp: Long?) {
    companion object {
        const val DB_KEY = "db"
    }
}
