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

package org.projectforge.plugins.cveradar.model

import org.projectforge.common.i18n.I18nEnum

/** A vulnerability with CVE id, or an incident without one (IMP-14). */
enum class CvrCaseKind : I18nEnum {
    CVE, INCIDENT;

    override val i18nKey: String
        get() = "plugins.cveradar.case.kind.$name"
}

/** The processing status (TRI-05). */
enum class CvrCaseStatus : I18nEnum {
    NEW, IN_TRIAGE, RATED, IN_REMEDIATION, CLOSED, NOT_RELEVANT;

    val isClosed: Boolean
        get() = this == CLOSED || this == NOT_RELEVANT

    override val i18nKey: String
        get() = "plugins.cveradar.triageStatus.$name"
}

/** The internal priority P1 – P4 (PRIO-01). */
enum class CvrPriority : I18nEnum {
    P1, P2, P3, P4;

    override val i18nKey: String
        get() = "plugins.cveradar.priority.$name"
}

/** How likely a vulnerability is exploited in our own environment, as rated by a member of the security team (TRI-03). */
enum class CvrExploitability : I18nEnum {
    HIGH, MEDIUM, LOW, NONE;

    override val i18nKey: String
        get() = "plugins.cveradar.exploitability.$name"
}

/** The answer of a team (FB-03), also of a team check (CLS-03). */
enum class CvrTeamAnswer : I18nEnum {
    NOT_AFFECTED, FIXED, IN_PROGRESS, RISK_ACCEPTED;

    /**
     * Whether the check is done: not affected or fixed. A risk accepted needs the security officer, who closes a case
     * with open items (CLS-05).
     */
    val isDone: Boolean
        get() = this == NOT_AFFECTED || this == FIXED

    override val i18nKey: String
        get() = "plugins.cveradar.teamAnswer.$name"
}

/** The confirmation of an employee (FB-05, CLS-04). */
enum class CvrClientAnswer : I18nEnum {
    DONE, NOT_AFFECTED, QUESTION;

    /** Whether the answer confirms the measures: done or not affected, a question doesn't. */
    val isConfirmed: Boolean
        get() = this != QUESTION

    override val i18nKey: String
        get() = "plugins.cveradar.clientAnswer.$name"
}

/** The type of a system of the inventory (INV-03). */
enum class CvrSystemType : I18nEnum {
    APPLICATION, SERVER, SAAS, NETWORK, CLIENT;

    override val i18nKey: String
        get() = "plugins.cveradar.systemType.$name"
}

/** The criticality of a system (INV-03), inherited downwards (INV-07). */
enum class CvrCriticality : I18nEnum {
    LOW, MEDIUM, HIGH, VERY_HIGH;

    override val i18nKey: String
        get() = "plugins.cveradar.criticality.$name"
}

/** The confidentiality level of a system (VIS-01), inherited downwards, raised on an instance only (INV-07). */
enum class CvrConfidentiality : I18nEnum {
    INTERNAL, CONFIDENTIAL, STRICTLY_CONFIDENTIAL;

    override val i18nKey: String
        get() = "plugins.cveradar.confidentiality.$name"
}

/** A technology is a product or an ecosystem (INV-23). */
enum class CvrTechnologyKind : I18nEnum {
    PRODUCT, ECOSYSTEM;

    override val i18nKey: String
        get() = "plugins.cveradar.technologyKind.$name"
}

/** Where an entry of the inventory comes from (INV-16). */
enum class CvrOrigin : I18nEnum {
    MANUAL, SBOM, CI, LOCKFILE, BUILD_LIST, JAR_LIST;

    override val i18nKey: String
        get() = "plugins.cveradar.origin.$name"
}

/** The level of a node of the technology catalogue (INV-28). */
enum class CvrCatalogKind : I18nEnum {
    ECOSYSTEM, PRODUCT, LIBRARY;

    override val i18nKey: String
        get() = "plugins.cveradar.catalogKind.$name"
}

/** Where a node of the technology catalogue comes from (INV-29). */
enum class CvrCatalogOrigin : I18nEnum {
    MANUAL, OSV;

    override val i18nKey: String
        get() = "plugins.cveradar.catalogOrigin.$name"
}
