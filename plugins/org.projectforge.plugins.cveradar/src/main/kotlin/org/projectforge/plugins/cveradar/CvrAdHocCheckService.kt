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
import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.plugins.cveradar.model.CvrTechnologyKind
import org.projectforge.plugins.cveradar.rest.AdHocFileStatus
import org.projectforge.plugins.cveradar.rest.AdHocRequest
import org.projectforge.plugins.cveradar.rest.AdHocStatus
import org.projectforge.plugins.cveradar.rest.FindingReport
import org.projectforge.plugins.cveradar.rest.ReportFinding
import org.projectforge.plugins.cveradar.rest.ReportTechnology
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * The ad-hoc check: lists of libraries (lockfiles, SBOMs, build tool output, single or in a zip, as for a component)
 * are read and checked against all vulnerabilities of the index with the rules of the inventory, without storing
 * anything. The files are read while the request waits, so a file that can't be read is refused right away; the check
 * runs as a job ([CvrAdHocJob]) that the client polls. Everybody with the reader role may check; a job is seen, cancelled
 * and reported by the person who started it only.
 */
@Service
class CvrAdHocCheckService {
    @Autowired
    private lateinit var access: CveRadarAccess

    @Autowired
    private lateinit var jobHandler: JobHandler

    @Autowired
    private lateinit var libraryService: CvrLibraryService

    @Autowired
    private lateinit var matchingService: CvrMatchingService

    @Autowired
    private lateinit var reportService: CvrFindingReportService

    fun start(user: PFUserDO, request: AdHocRequest): AdHocStatus {
        access.checkReader(user)
        if (request.files.isEmpty()) {
            throw UserException(ERROR_NO_FILES)
        }
        if (request.files.size > LibraryListParser.MAX_FILES) {
            throw UserException(LibraryListParser.ERROR_TOO_MANY_FILES, LibraryListParser.MAX_FILES.toString())
        }
        if (jobHandler.getJobsOfUser(null, true).any { it.area == CvrAdHocJob.AREA && !it.terminated }) {
            throw UserException(ERROR_RUNNING)
        }
        val catalog = libraryService.catalog()
        val expanded = LibraryArchive.expand(request.files.map { Triple((it.name ?: "").take(255), it.text, it.data) })
        val files = LibraryListParser.parseAll(expanded.map { (name, text) -> name.take(255) to text })
            .map { file -> AdHocFile(file.name, file.format, file.entries.map { libraryService.resolveGroup(it, catalog) }) }
        val job = CvrAdHocJob(files, matchingService, catalog, "CVE-Radar: ad-hoc check of ${files.size} file(s)")
        jobHandler.addJob(job)
        return statusOf(job)
    }

    fun status(user: PFUserDO, jobId: Int): AdHocStatus = statusOf(job(user, jobId))

    fun cancel(user: PFUserDO, jobId: Int) = jobHandler.cancelJob(job(user, jobId))

    /** The report of a finished check; [anonymize]: the file names are replaced by `F1`, `F2`, … (AI-03). */
    fun report(user: PFUserDO, jobId: Int, anonymize: Boolean, exclude: Set<String> = emptySet()): FindingReport {
        val job = job(user, jobId)
        if (job.status != AbstractJob.Status.FINISHED) {
            throw UserException(ERROR_NOT_FINISHED)
        }
        val findings = job.files.flatMapIndexed { index, file ->
            val name = if (anonymize) "F${index + 1}" else file.name
            val origin = CvrLibraryService.originOf(file.format)
            file.hits.distinctBy { listOf(it.key, it.entry.purl, it.certainty, it.matched) }.map { hit ->
                hit.key to ReportFinding(
                    system = null, instances = emptyList(), component = name, certainty = hit.certainty, matched = hit.matched,
                    technology = ReportTechnology(
                        CvrTechnologyKind.PRODUCT, null, hit.entry.product, hit.entry.version, hit.entry.ecosystem,
                        hit.entry.purl, null, origin,
                        // The paths inside an SBOM may name the project: not when anonymized.
                        source = if (anonymize) name else name + (hit.entry.location?.let { ": $it" } ?: ""),
                    ),
                )
            }
        }.groupBy({ it.first }, { it.second })
            .mapValues { (_, list) -> list.sortedWith(compareBy({ it.certainty }, { it.component }, { it.technology?.product })) }
        return reportService.adHocReport(
            findings.filterKeys { it is Long }.mapKeys { it.key as Long },
            findings.filterKeys { it is String }.mapKeys { it.key as String },
            anonymize,
            exclude,
        )
    }

    private fun job(user: PFUserDO, jobId: Int): CvrAdHocJob {
        access.checkReader(user)
        // getJobById answers only the owner (CvrAdHocJob.writeAccess): the job of somebody else is "not found".
        val job = jobHandler.getJobById(jobId) as? CvrAdHocJob
        if (job == null || job.ownerId != user.id) {
            throw UserException(ERROR_NOT_FOUND)
        }
        return job
    }

    private fun statusOf(job: CvrAdHocJob) = AdHocStatus(
        jobId = job.id,
        status = job.status,
        error = job.errorMessage,
        files = job.files.map { AdHocFileStatus(it.name, it.format, it.entries.size, it.checked, it.hits.size, it.done) },
        packageCount = job.files.sumOf { it.entries.size },
        checkedCount = job.files.sumOf { it.checked },
        vulnerabilityCount = job.files.flatMap { file -> file.hits.map { it.key } }.toSet().size,
        hitCount = job.files.sumOf { it.hits.size },
    )

    companion object {
        const val ERROR_NO_FILES = "plugins.cveradar.adhoc.error.noFiles"
        const val ERROR_RUNNING = "plugins.cveradar.adhoc.error.running"
        const val ERROR_NOT_FOUND = "plugins.cveradar.adhoc.error.notFound"
        const val ERROR_NOT_FINISHED = "plugins.cveradar.adhoc.error.notFinished"
    }
}
