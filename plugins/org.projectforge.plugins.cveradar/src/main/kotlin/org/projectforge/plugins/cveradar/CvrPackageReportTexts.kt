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

import org.projectforge.framework.i18n.translate
import org.projectforge.framework.i18n.translateMsg
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.plugins.cveradar.rest.PackageReport
import org.projectforge.plugins.cveradar.rest.PackageVulnerability
import java.awt.Color
import java.math.BigDecimal
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Date

/** The texts, colours and formats the PDF and the Excel file of a [PackageReport] share, in the language of the user. */
internal object PackageReportTexts {
    private const val PREFIX = "plugins.cveradar.packageReport"

    fun text(key: String): String = translate("$PREFIX.$key")

    fun text(key: String, vararg params: Any?): String = translateMsg("$PREFIX.$key", *params)

    /** `Kritisch`, `Hoch`, …; `–` for a vulnerability without severity. */
    fun severity(severity: String?): String =
        severity?.takeIf { it in CvrFindingReportService.SEVERITIES && it != "NONE" }?.let { text("severity.$it") } ?: "–"

    fun score(score: BigDecimal?): String = score?.stripTrailingZeros()?.toPlainString()?.let {
        if (it.contains('.')) it else "$it.0"
    } ?: "–"

    /** The ad-hoc check, or the node of the inventory with its code and name (neutral ones if anonymized). */
    fun scope(report: PackageReport): String {
        val scope = report.scope
        if (scope.adHoc || scope.level == null) return text("scope.adHoc")
        return listOfNotNull(translate("plugins.cveradar.level.${scope.level}"), scope.code, scope.name)
            .joinToString(" ")
    }

    fun date(date: Date): String = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
        .withLocale(ThreadLocalUserContext.locale).format(date.toInstant().atZone(ThreadLocalUserContext.zoneId))

    fun dateTime(date: Date): String = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM, FormatStyle.SHORT)
        .withLocale(ThreadLocalUserContext.locale).format(date.toInstant().atZone(ThreadLocalUserContext.zoneId))

    /** The case of the security team in one line: code, priority, status and its fix version. */
    fun case(vulnerability: PackageVulnerability): String? {
        val case = vulnerability.case ?: return null
        val details = listOfNotNull(
            case.priority?.name,
            case.status?.let { translate(it) },
            case.fixVersion?.let { text("case.fixVersion", it) },
        ).joinToString(", ")
        return text("case", case.code ?: "", details)
    }

    /** The fix version of a package with the number of its vulnerabilities without a fix, e.g. `5.36.1 (+2 ohne Fix)`. */
    fun fixVersion(fixVersion: String?, withoutFixCount: Int): String = when {
        fixVersion == null -> "–"
        withoutFixCount > 0 -> "$fixVersion (${text("withoutFix", withoutFixCount)})"
        else -> fixVersion
    }

    /** The colour of a severity, as the KPI tiles and the score cells show it. */
    fun color(severity: String?): Color = when (severity) {
        "CRITICAL" -> Color(0x7B, 0x1F, 0x1F)
        "HIGH" -> Color(0xC0, 0x39, 0x2B)
        "MEDIUM" -> Color(0xE0, 0x8E, 0x0B)
        "LOW" -> Color(0x5B, 0x8D, 0xB8)
        else -> Color(0x9E, 0x9E, 0x9E)
    }

    /** The severities with a tile, the highest first. */
    val TILES = listOf("CRITICAL", "HIGH", "MEDIUM", "LOW")
}
