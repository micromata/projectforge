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

package org.projectforge.plugins.cveradar

/**
 * Compares versions like `2.6.34`, `2.10.0-beta1` or `1.2a` part by part: numeric parts as numbers, other parts as
 * text; a missing part is lower (`2.6` < `2.6.1`), except before a text part (`2.6.0` > `2.6.0-beta1`).
 */
object VersionComparator : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        val partsA = split(a)
        val partsB = split(b)
        for (i in 0 until maxOf(partsA.size, partsB.size)) {
            // A missing part is lower, but a release is higher than its pre-release (2.6.0 > 2.6.0-beta1).
            val partA = partsA.getOrNull(i) ?: return if (partsB[i].toLongOrNull() != null) -1 else 1
            val partB = partsB.getOrNull(i) ?: return if (partA.toLongOrNull() != null) 1 else -1
            val numA = partA.toLongOrNull()
            val numB = partB.toLongOrNull()
            val result = when {
                numA != null && numB != null -> numA.compareTo(numB)
                numA != null -> 1 // 2.6.0 > 2.6.beta
                numB != null -> -1
                else -> partA.compareTo(partB, ignoreCase = true)
            }
            if (result != 0) {
                return result
            }
        }
        return 0
    }

    /** Whether [version] is [minVersion] or higher. */
    fun isAtLeast(version: String?, minVersion: String?): Boolean {
        if (version.isNullOrBlank() || minVersion.isNullOrBlank()) {
            return false
        }
        return compare(version.trim(), minVersion.trim()) >= 0
    }

    private fun split(version: String): List<String> {
        return version.trim().removePrefix("v").removePrefix("V")
            .split(Regex("[^A-Za-z0-9]+|(?<=\\d)(?=[A-Za-z])|(?<=[A-Za-z])(?=\\d)"))
            .filter { it.isNotEmpty() }
    }
}
