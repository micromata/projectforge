/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2025 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.fibu

import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Configuration

/**
 * Configuration for calculating invoicing quota (Fakturaquote) in monthly employee reports.
 *
 * The invoicing quota is the percentage of work time that is billed to customers.
 *
 * Example configuration in application.properties:
 * ```
 * projectforge.invoicing-quota.enabled=true
 * projectforge.invoicing-quota.billed-patterns=5.*,!5.999.*,5.___.99.*
 * projectforge.invoicing-quota.ignored-patterns=6.3*
 * ```
 *
 * Pattern syntax:
 * - `5.*` = Starts with "5."
 * - `!5.999.*` = Exception: does NOT start with "5.999."
 * - `5.___.99.*` = Regex-like: 5 + 3 arbitrary digits + .99 + anything
 * - `6.3*` = Starts with "6.3"
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Configuration
@ConfigurationProperties(prefix = "projectforge.invoicing-quota")
open class InvoicingQuotaConfiguration {
    /**
     * Enable or disable the invoicing quota calculation.
     * If disabled, no invoicing quota will be shown in monthly employee reports.
     */
    var enabled: Boolean = false

    /**
     * List of patterns for Kost2 display names that count as billed.
     * Patterns starting with '!' are negations (exceptions).
     *
     * Example: `5.*,!5.999.*` = All Kost5 except 5.999.*
     */
    var billedPatterns: List<String> = emptyList()

    /**
     * List of patterns for Kost2 display names that should be ignored
     * in the calculation (e.g., vacation, absences).
     *
     * Example: `6.3*` = Urlaub, Elternzeit, Krankheit, Zeitausgleich, Abwesenheiten
     */
    var ignoredPatterns: List<String> = emptyList()
}
