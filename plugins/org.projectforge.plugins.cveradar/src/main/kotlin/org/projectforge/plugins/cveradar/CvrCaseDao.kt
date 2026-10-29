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

import org.projectforge.common.i18n.UserException
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.access.OperationType
import org.projectforge.plugins.cveradar.model.CvrCaseDO
import org.projectforge.plugins.cveradar.model.CvrCaseKind
import org.projectforge.plugins.cveradar.model.CvrCaseStatus
import org.projectforge.plugins.cveradar.model.CvrVulnerabilityDO
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service
import java.time.Year
import java.util.Date

/**
 * The cases (vulnerabilities and incidents) of the CVE-Radar: read by the readers, written by the security
 * team (see [CveRadarAccess]). Employees see released cases only through
 * [org.projectforge.plugins.cveradar.rest.CveRadarRest.my], without this dao's access.
 */
@Service
class CvrCaseDao : CvrBaseDao<CvrCaseDO>(CvrCaseDO::class.java) {
    @Autowired
    private lateinit var config: CveRadarConfig

    @Autowired
    private lateinit var vulnerabilityDao: CvrVulnerabilityDao

    override fun newInstance(): CvrCaseDO {
        return CvrCaseDO()
    }

    override val additionalSearchFields: Array<String>
        get() = ADDITIONAL_SEARCH_FIELDS

    /**
     * A CVE needs its CVE id as code; an incident without code gets the next free id of the year (IMP-14). Codes are
     * unique, the lists of the fix criteria are normalised.
     */
    override fun onInsertOrModify(obj: CvrCaseDO, operationType: OperationType) {
        obj.kind = obj.kind ?: CvrCaseKind.INCIDENT
        obj.status = obj.status ?: CvrCaseStatus.NEW
        obj.code = obj.code?.trim()?.uppercase()?.takeIf { it.isNotEmpty() }
        if (obj.kind == CvrCaseKind.CVE) {
            val code = obj.code
            if (code == null || !CVE_REGEX.matches(code)) {
                throw UserException("plugins.cveradar.case.error.cveId")
            }
        } else if (obj.code == null) {
            obj.code = nextIncidentCode()
        }
        persistenceService.selectSingleResult(
            "from CvrCaseDO c where c.code = :code",
            CvrCaseDO::class.java,
            Pair("code", obj.code),
        )?.let {
            if (it.id != obj.id) {
                throw UserException("plugins.cveradar.case.error.codeExists", obj.code)
            }
        }
        // Taken from the stored vulnerability, never from the request: a case of kind CVE belongs to the CVE of its code.
        obj.vulnerability = if (obj.kind == CvrCaseKind.CVE) obj.code?.let { vulnerabilityDao.findByCveId(it) } else null
        obj.priorityReason = obj.priorityReason?.trim()?.takeIf { it.isNotEmpty() }
        obj.fixVersion = obj.fixVersion?.trim()?.takeIf { it.isNotEmpty() }
        obj.referenceLinks = CvrReferenceLinks.normalize(obj.referenceLinks)
        obj.scanPluginIds = CvrCaseDO.splitList(obj.scanPluginIds).joinToString(",").takeIf { it.isNotEmpty() }
        obj.cveIds = CvrCaseDO.splitList(obj.cveIds).map { it.uppercase() }.joinToString(",").takeIf { it.isNotEmpty() }
        obj.minClientAgentVersion = obj.minClientAgentVersion?.trim()?.takeIf { it.isNotEmpty() }
        obj.clientAgentCheckId = obj.clientAgentCheckId?.trim()?.takeIf { it.isNotEmpty() }
    }

    /**
     * Serialised, so two new incidents saved at the same time don't get the same code (within this instance; the
     * unique constraint stays the last guard).
     */
    override fun insert(obj: CvrCaseDO, checkAccess: Boolean): Long {
        synchronized(INSERT_LOCK) {
            return super.insert(obj, checkAccess)
        }
    }

    /** The case of the vulnerability, null if none was opened yet (no access check: the caller decides). */
    fun findByVulnerabilityId(vulnerabilityId: Long): CvrCaseDO? {
        return persistenceService.selectSingleResult(
            "from CvrCaseDO c where c.vulnerability.id = :id and c.deleted = false",
            CvrCaseDO::class.java,
            Pair("id", vulnerabilityId),
        )
    }

    /** The case with the code, a deleted one included (codes are unique; no access check: the caller decides). */
    fun findByCode(code: String): CvrCaseDO? {
        return persistenceService.selectSingleResult(
            "from CvrCaseDO c where c.code = :code",
            CvrCaseDO::class.java,
            Pair("code", code.trim().uppercase()),
        )
    }

    /**
     * Links the case opened for a CVE before the NVD delivered it (IMP-14) to the imported vulnerability (no access
     * check: for the import).
     */
    fun linkToVulnerability(vulnerability: CvrVulnerabilityDO) {
        val case = persistenceService.selectSingleResult(
            "from CvrCaseDO c where c.code = :code and c.kind = :kind and c.vulnerability is null and c.deleted = false",
            CvrCaseDO::class.java,
            Pair("code", vulnerability.cveId),
            Pair("kind", CvrCaseKind.CVE),
        ) ?: return
        case.vulnerability = vulnerability
        update(case, checkAccess = false)
    }

    /** The cases of the vulnerabilities by vulnerability id, in one query per chunk (no access check). */
    fun selectByVulnerabilityIds(vulnerabilityIds: Collection<Long>): Map<Long, CvrCaseDO> {
        if (vulnerabilityIds.isEmpty()) {
            return emptyMap()
        }
        return persistenceService.executeQueryBatched(
            "from CvrCaseDO c where c.vulnerability.id in :ids and c.deleted = false",
            CvrCaseDO::class.java,
            "ids",
            vulnerabilityIds,
        ).associateBy { it.vulnerability!!.id!! }
    }

    /**
     * Conflict detection of a save (TRI-04): [loadedLastUpdate] is the `lastUpdate` the editor loaded. If the stored
     * case was changed since, nothing may be saved over it unseen.
     */
    fun checkNotChanged(caseId: Long, loadedLastUpdate: Date?) {
        val stored = persistenceService.selectSingleResult(
            "select c.lastUpdate from CvrCaseDO c where c.id = :id",
            Date::class.java,
            Pair("id", caseId),
        ) ?: return
        if (loadedLastUpdate == null || stored.time != loadedLastUpdate.time) {
            throw UserException("plugins.cveradar.case.error.conflict", PFDateTime.from(stored).isoStringSeconds)
        }
    }

    /** `<prefix>-<year>-<n>` with the next n of the year; the prefix has letters and digits only (no LIKE wildcard). */
    internal fun nextIncidentCode(year: Int = Year.now().value): String {
        val prefix = "${config.caseIdPrefix.uppercase().filter { it in 'A'..'Z' || it in '0'..'9' }.ifEmpty { "INC" }}-$year-"
        val codes = persistenceService.executeQuery(
            "select c.code from CvrCaseDO c where c.code like :prefix",
            String::class.java,
            Pair("prefix", "$prefix%"),
        )
        val max = codes.mapNotNull { it.removePrefix(prefix).toIntOrNull() }.maxOrNull() ?: 0
        return "$prefix${max + 1}"
    }

    companion object {
        private val INSERT_LOCK = Any()

        val CVE_REGEX = Regex("""^CVE-\d{4}-\d{4,}$""")

        private val ADDITIONAL_SEARCH_FIELDS = arrayOf("assignee.username", "assignee.firstname", "assignee.lastname")
    }
}
