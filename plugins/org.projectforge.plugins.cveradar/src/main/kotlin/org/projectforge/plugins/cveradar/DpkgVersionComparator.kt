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
 * Compares the versions of Debian and Ubuntu packages as dpkg does (Debian Policy 5.6.12): `[epoch:]upstream[-revision]`,
 * the epoch as number, upstream and revision alternately by non-digits (letters before other characters, `~` before
 * everything, even the end) and digits (as numbers). So `2.9.14+dfsg-1.3~deb12u1` < `2.9.14+dfsg-1.3` and
 * `7.81.0-1ubuntu1.30` > `7.81.0-1ubuntu1.3`. Not for the versions of libraries: see [VersionComparator].
 */
object DpkgVersionComparator : Comparator<String> {
    override fun compare(a: String, b: String): Int {
        val va = parse(a)
        val vb = parse(b)
        return va.epoch.compareTo(vb.epoch).takeIf { it != 0 }
            ?: comparePart(va.upstream, vb.upstream).takeIf { it != 0 }
            ?: comparePart(va.revision, vb.revision)
    }

    private class Version(val epoch: Long, val upstream: String, val revision: String)

    private fun parse(version: String): Version {
        val v = version.trim()
        val colon = v.indexOf(':')
        val epoch = if (colon > 0) v.substring(0, colon).toLongOrNull() else null
        val rest = if (epoch != null) v.substring(colon + 1) else v
        val dash = rest.lastIndexOf('-')
        return if (dash > 0) Version(epoch ?: 0, rest.substring(0, dash), rest.substring(dash + 1)) else Version(epoch ?: 0, rest, "")
    }

    private fun comparePart(a: String, b: String): Int {
        var i = 0
        var j = 0
        while (i < a.length || j < b.length) {
            // The non-digits, character by character.
            while ((i < a.length && !a[i].isAsciiDigit()) || (j < b.length && !b[j].isAsciiDigit())) {
                val result = order(a.getOrNull(i)).compareTo(order(b.getOrNull(j)))
                if (result != 0) return result
                i++
                j++
            }
            // The digits as a number, without overflow: by length after the leading zeros, then as text.
            val startA = i
            while (i < a.length && a[i].isAsciiDigit()) i++
            val startB = j
            while (j < b.length && b[j].isAsciiDigit()) j++
            val numA = a.substring(startA, i).trimStart('0')
            val numB = b.substring(startB, j).trimStart('0')
            val result = numA.length.compareTo(numB.length).takeIf { it != 0 } ?: numA.compareTo(numB)
            if (result != 0) return result
        }
        return 0
    }

    /** The weight of a character of a non-digit part: `~` lowest, then the end (or a digit), letters, the others. */
    private fun order(c: Char?): Int = when {
        c == null || c.isAsciiDigit() -> 0
        c == '~' -> -1
        c.isLetter() -> c.code
        else -> c.code + 256
    }

    private fun Char.isAsciiDigit() = this in '0'..'9'
}
