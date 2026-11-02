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

package org.projectforge.business.user

/**
 * Which app versions work with this server. The app compares itself with [MIN_APP_VERSION] (published in the
 * system status) and asks its user for an update; the server only marks outdated devices in "My account".
 * Nothing is refused.
 *
 * The other direction, the server version an app needs, is declared in the app (`minServerVersion`): only the newer
 * side knows about a break.
 */
object AppCompatibility {
    /**
     * The oldest app version (`version` of the app's pubspec.yaml, without the build number) this server works with.
     * Raise it in the commit that removes or changes an api the older apps use. Prefer a feature flag in the system
     * status (e.g. `appLoginPath`) where the app can fall back.
     */
    const val MIN_APP_VERSION = "1.0.1"

    /**
     * True if [appVersion] (header [AppDeviceDao.APP_VERSION_HEADER], e.g. `1.0.1+3`) is older than
     * [MIN_APP_VERSION]. False for an unknown or unparseable version: no false alarm.
     */
    fun isOutdated(appVersion: String?): Boolean {
        val version = parse(appVersion) ?: return false
        val min = parse(MIN_APP_VERSION) ?: return false
        return compare(version, min) < 0
    }

    /** The numeric segments of `1.0.1`, `1.0.1+3` or `1.0.1-beta`; null if it doesn't start with a number. */
    internal fun parse(version: String?): List<Int>? {
        val match = VERSION_REGEX.find(version?.trim() ?: return null) ?: return null
        return match.value.split('.').map { it.toIntOrNull() ?: return null }
    }

    internal fun compare(a: List<Int>, b: List<Int>): Int {
        for (i in 0 until maxOf(a.size, b.size)) {
            val diff = a.getOrElse(i) { 0 }.compareTo(b.getOrElse(i) { 0 })
            if (diff != 0) {
                return diff
            }
        }
        return 0
    }

    private val VERSION_REGEX = Regex("""^\d{1,9}(\.\d{1,9})*""")
}
