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

import com.fasterxml.jackson.databind.annotation.JsonSerialize
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.Transient
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.FullTextField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexedEmbedded
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.IndexingDependency
import org.projectforge.Constants
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.json.IdOnlySerializer
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO

/**
 * A vulnerability or an incident in the CVE-Radar (R0): what it is, its processing, the measures for techies and
 * non-techies and its fix criteria (CLS-01), from which the closure overview (CLS-02) is computed. Historised:
 * every change of status, priority and criteria is kept with who and when (SEC-06).
 */
@Entity
@Indexed
@Table(
    name = "T_PLUGIN_CVERADAR_CASE",
    indexes = [Index(name = "idx_fk_t_plugin_cveradar_case_assignee_fk", columnList = "assignee_fk")],
)
open class CvrCaseDO : DefaultBaseDO() {
    /**
     * CVE id (e.g. `CVE-2026-12345`) or the id of an incident (`INC-2026-1`), unique. Not null in the database, but
     * optional in the form: the dao gives an incident without id the next one ([CvrCaseDao.onInsertOrModify]).
     */
    @PropertyInfo(i18nKey = "plugins.cveradar.case.code")
    @FullTextField
    @get:Column(length = 50, unique = true)
    open var code: String? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.case.kind", required = true)
    @GenericField
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var kind: CvrCaseKind? = null

    @PropertyInfo(i18nKey = "title", required = true)
    @FullTextField
    @get:Column(length = Constants.LENGTH_TITLE, nullable = false)
    open var title: String? = null

    /** Markdown, rendered escaped (AI-09). */
    @PropertyInfo(i18nKey = "description")
    @FullTextField
    @get:Column(length = LENGTH_MARKDOWN)
    open var description: String? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.case.status", required = true)
    @GenericField
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var status: CvrCaseStatus? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.case.priority")
    @GenericField
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 5)
    open var priority: CvrPriority? = null

    @PropertyInfo(i18nKey = "plugins.cveradar.case.assignee")
    @IndexedEmbedded(includeDepth = 1)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "assignee_fk")
    @JsonSerialize(using = IdOnlySerializer::class)
    open var assignee: PFUserDO? = null

    /** Technical steps, commands, fix versions (ACT-01), Markdown. */
    @PropertyInfo(i18nKey = "plugins.cveradar.case.techMeasures")
    @get:Column(name = "tech_measures", length = LENGTH_MARKDOWN)
    open var techMeasures: String? = null

    /** Understandable step-by-step guide for the employees (ACT-01, ACT-03), Markdown. */
    @PropertyInfo(i18nKey = "plugins.cveradar.case.nonTechMeasures")
    @get:Column(name = "non_tech_measures", length = LENGTH_MARKDOWN)
    open var nonTechMeasures: String? = null

    /**
     * Released to all employees (ACT-02): they see the case with its non-techie measures and confirm them, unless
     * their devices prove the fix already (CLS-04).
     */
    @PropertyInfo(i18nKey = "plugins.cveradar.case.clientAdvisory")
    @get:Column(name = "client_advisory", nullable = false)
    open var clientAdvisory: Boolean = false

    /** Fix criterion: a device with this version of the client agent or higher is fixed (CLS-01). */
    @PropertyInfo(i18nKey = "plugins.cveradar.case.minClientAgentVersion")
    @get:Column(name = "min_client_agent_version", length = 50)
    open var minClientAgentVersion: String? = null

    /** Fix criterion: a device whose client agent reports this check as passed is fixed (CLS-01, EXT-05). */
    @PropertyInfo(i18nKey = "plugins.cveradar.case.clientAgentCheckId")
    @get:Column(name = "client_agent_check_id", length = 100)
    open var clientAgentCheckId: String? = null

    /** Fix criterion: ids of scanner plugins, comma separated; an open scan finding of one of them is open (CLS-01). */
    @PropertyInfo(i18nKey = "plugins.cveradar.case.scanPluginIds")
    @get:Column(name = "scan_plugin_ids", length = 1000)
    open var scanPluginIds: String? = null

    /** Fix criterion: CVE ids, comma separated; an open scan finding naming one of them is open (CLS-01). */
    @PropertyInfo(i18nKey = "plugins.cveradar.case.cveIds")
    @FullTextField
    @get:Column(name = "cve_ids", length = 1000)
    open var cveIds: String? = null

    /** Reason of closing (CLS-05). */
    @PropertyInfo(i18nKey = "plugins.cveradar.case.closeReason")
    @get:Column(name = "close_reason", length = Constants.LENGTH_TEXT)
    open var closeReason: String? = null

    val assigneeId: Long?
        @Transient
        get() = assignee?.id

    companion object {
        const val LENGTH_MARKDOWN = 20000

        /** Splits a comma (or blank) separated list, e.g. [scanPluginIds]. */
        fun splitList(value: String?): List<String> {
            return value?.split(',', ';', ' ', '\n')?.map { it.trim() }?.filter { it.isNotEmpty() }?.distinct()
                ?: emptyList()
        }
    }
}
