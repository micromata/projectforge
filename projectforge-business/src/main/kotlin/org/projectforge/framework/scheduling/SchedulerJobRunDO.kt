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

package org.projectforge.framework.scheduling

import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.NamedQueries
import jakarta.persistence.NamedQuery
import jakarta.persistence.Table
import org.projectforge.common.scheduling.SchedulerTrigger
import java.util.Date

/**
 * A single run of a scheduled job, stored only if it failed or was slow (see [SchedulerJobStatsService]).
 * The user of a manual run isn't stored (only logged).
 */
@Entity
@Table(
    name = "T_PF_SCHEDULER_JOB_RUN",
    indexes = [
        Index(name = "idx_t_pf_scheduler_job_run_job_start", columnList = "job_id, start_time"),
        Index(name = "idx_t_pf_scheduler_job_run_start", columnList = "start_time"),
    ],
)
@NamedQueries(
    NamedQuery(
        name = SchedulerJobRunDO.SELECT_BY_JOB,
        query = "from SchedulerJobRunDO r where r.jobId = :jobId order by r.startTime desc",
    ),
    NamedQuery(
        name = SchedulerJobRunDO.DELETE_BEFORE,
        query = "delete from SchedulerJobRunDO r where r.startTime < :before",
    ),
)
open class SchedulerJobRunDO {
    @get:Id
    @get:GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "hibernate_sequence")
    @get:Column(name = "pk")
    open var id: Long? = null

    @get:Column(name = "job_id", length = 100, nullable = false)
    open var jobId: String? = null

    @get:Column(name = "start_time", nullable = false)
    open var startTime: Date? = null

    @get:Column(name = "duration_ms", nullable = false)
    open var durationMs: Long = 0

    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20, nullable = false)
    open var status: SchedulerRunStatus? = null

    @get:Column(nullable = false)
    open var slow: Boolean = false

    @get:Enumerated(EnumType.STRING)
    @get:Column(name = "run_trigger", length = 10, nullable = false)
    open var trigger: SchedulerTrigger? = null

    @get:Column(name = "error_message", length = SchedulerJobStatsDO.MAX_MESSAGE)
    open var errorMessage: String? = null

    @get:Column(name = "stack_excerpt", length = MAX_STACK)
    open var stackExcerpt: String? = null

    companion object {
        internal const val SELECT_BY_JOB = "SchedulerJobRunDO_SelectByJob"
        internal const val DELETE_BEFORE = "SchedulerJobRunDO_DeleteBefore"

        internal const val MAX_STACK = 10000
    }
}
