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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.projectforge.common.i18n.UserException
import org.projectforge.framework.access.AccessException
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.jobs.AbstractJob
import org.projectforge.framework.jobs.JobHandler
import org.projectforge.plugins.cveradar.rest.AreaRequest
import org.projectforge.plugins.cveradar.rest.CvrInventoryRest
import org.projectforge.plugins.cveradar.rest.CvrSbomImportRest
import org.projectforge.plugins.cveradar.rest.SbomImportApplyRequest
import org.projectforge.plugins.cveradar.rest.SbomImportSelection
import org.projectforge.plugins.cveradar.rest.SbomImportStatus
import org.springframework.beans.factory.annotation.Autowired
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * The bulk import of SBOMs into an area (INV-35): the group of the area imports, an outsider is refused and doesn't see
 * the plan of others; repositories become systems, images components of the images system; a second import of the
 * same zip changes nothing. The size of the zip is limited per area, set by the security officer only.
 */
class CvrSbomImportTest : CveRadarTestBase() {
    @Autowired
    private lateinit var catalogService: CvrCatalogService

    @Autowired
    private lateinit var inventory: CvrInventoryRest

    @Autowired
    private lateinit var rest: CvrSbomImportRest

    @Autowired
    private lateinit var service: CvrSbomImportService

    @Autowired
    private lateinit var jobHandler: JobHandler

    /** Done by the plugin when it starts. */
    @BeforeEach
    fun seedEcosystems() {
        logon(TEST_ADMIN_USER)
        catalogService.ensureEcosystems()
    }

    private fun zip(): ByteArray {
        val repo = """{"bomFormat":"CycloneDX","metadata":{"component":{"type":"application","name":"repo-a"}},"components":[
            {"purl":"pkg:maven/org.slf4j/slf4j-api@2.0.9"},{"purl":"pkg:maven/io.netty/netty-codec@4.1.100.Final"},
            {"type":"file","name":"README.md"}]}"""
        val image = """{"bomFormat":"CycloneDX","metadata":{"component":{"type":"container","name":"nginx","version":"1.27"}},
            "components":[{"purl":"pkg:deb/debian/openssl@3.0.11?distro=debian-12"}]}"""
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            listOf(
                "export/repositories/repo-a/bom.cdx.json" to repo,
                "export/repositories/repo-a/scan.log" to "done",
                "export/images/0815/syft.cdx.json" to image,
            ).forEach { (name, text) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(text.toByteArray())
                zip.closeEntry()
            }
        }
        return out.toByteArray()
    }

    private fun <J : AbstractJob> await(jobId: Int, status: AbstractJob.Status = AbstractJob.Status.FINISHED): J {
        @Suppress("UNCHECKED_CAST")
        val job = jobHandler.getJobById(jobId) as J
        val until = System.currentTimeMillis() + 30_000
        while (!job.terminated && System.currentTimeMillis() < until) {
            Thread.sleep(20)
        }
        assertEquals(status, job.status, job.errorMessage)
        return job
    }

    private fun analyze(areaId: Long, bytes: ByteArray = zip()): Int {
        val user = org.projectforge.framework.persistence.user.api.ThreadLocalUserContext.requiredLoggedInUser
        val jobId = service.startAnalysis(user, areaId, bytes.size.toLong(), ByteArrayInputStream(bytes)).jobId
        await<CvrSbomImportAnalyzeJob>(jobId)
        return jobId
    }

    private fun libraries(code: String) = inventory.tree().rows.single { it.code == code }.technologies.mapNotNull { it.purl }.toSet()

    @Test
    fun `repositories and images are imported into the area of the group, an outsider is refused`() {
        val owner = user("sbom-owner", null)
        val outsider = user("sbom-outsider", null)
        val groupId = (initTestDB.getGroup("cveradar-sbom-g") ?: initTestDB.addGroup("cveradar-sbom-g")).id!!
        addToGroup(owner, "cveradar-sbom-g")
        logon(owner)
        val area = inventory.createArea(AreaRequest(code = "SBI", name = "Sbom import", groupId = groupId))

        logon(outsider)
        val bytes = zip()
        assertThrows<AccessException> { service.startAnalysis(outsider, area, bytes.size.toLong(), ByteArrayInputStream(bytes)) }

        logon(owner)
        val importId = analyze(area)
        val plan = rest.plan(area, importId)
        assertEquals(2, plan.documentCount)
        assertEquals(1, plan.skipped.otherFiles)
        val targets = plan.targets.associateBy { it.key }
        val repo = targets["R:repositories/repo-a"]!!
        assertEquals("REPO-A", repo.systemCode)
        assertEquals(2, repo.libraryCount)
        assertEquals(1, repo.withoutPurlCount)
        assertEquals(SbomImportStatus.NEW, repo.status)
        val image = targets["C:nginx:1.27"]!!
        assertEquals("SBI-IMAGES", image.systemCode)
        assertEquals("NGINX-1.27", image.componentCode)

        // The plan is the starter's only.
        logon(outsider)
        assertThrows<UserException> { rest.plan(area, importId) }
        assertThrows<UserException> {
            rest.apply(area, importId, SbomImportApplyRequest(listOf(SbomImportSelection(repo.key))))
        }

        logon(owner)
        val selection = listOf(SbomImportSelection(repo.key, code = "sbi-repo-a", name = "Repo A"), SbomImportSelection(image.key))
        await<CvrSbomImportApplyJob>(rest.apply(area, importId, SbomImportApplyRequest(selection)).jobId)
        assertEquals(
            setOf("pkg:maven/org.slf4j/slf4j-api@2.0.9", "pkg:maven/io.netty/netty-codec@4.1.100.Final"),
            libraries("SBI-REPO-A-SBOM"),
        )
        assertEquals(setOf("pkg:deb/debian/openssl@3.0.11?distro=debian-12"), libraries("SBI-IMAGES-NGINX-1.27"))
        assertEquals("Repo A", inventory.tree().rows.single { it.code == "SBI-REPO-A" }.name)

        // Again: the stored ones are updated, nothing changes.
        val again = rest.plan(area, analyze(area)).targets.associateBy { it.key }
        assertEquals(SbomImportStatus.UPDATE, again["C:nginx:1.27"]!!.status)
        assertEquals(0, again["C:nginx:1.27"]!!.diff!!.added)
        assertEquals(1, again["C:nginx:1.27"]!!.diff!!.unchanged)
        // The repository was stored under another code than proposed: new under the proposed one.
        assertEquals(SbomImportStatus.NEW, again["R:repositories/repo-a"]!!.status)
        assertNull(again["R:repositories/repo-a"]!!.diff)
    }

    @Test
    fun `one analysis per user at a time, a plan is dropped when imported or replaced, errors are translated`() {
        val owner = user("sbom-busy-owner", null)
        val groupId = (initTestDB.getGroup("cveradar-sbom-busy-g") ?: initTestDB.addGroup("cveradar-sbom-busy-g")).id!!
        addToGroup(owner, "cveradar-sbom-busy-g")
        logon(owner)
        val area = inventory.createArea(AreaRequest(code = "SBB", name = "Busy", groupId = groupId))

        // A second upload while the first one is stored.
        val bytes = zip()
        var second: UserException? = null
        val input = object : ByteArrayInputStream(bytes) {
            override fun read(b: ByteArray, off: Int, len: Int): Int {
                if (second == null) {
                    second = assertThrows<UserException> {
                        service.startAnalysis(owner, area, bytes.size.toLong(), ByteArrayInputStream(bytes))
                    }
                }
                return super.read(b, off, len)
            }
        }
        val first = service.startAnalysis(owner, area, bytes.size.toLong(), input).jobId
        assertEquals(CvrSbomImportService.ERROR_BUSY, second!!.i18nKey)
        await<CvrSbomImportAnalyzeJob>(first)
        // The image only: the repository would take the code REPO-A from the other tests.
        val image = rest.plan(area, first).targets.single { it.key == "C:nginx:1.27" }

        // A newer analysis replaces the plan of the first.
        val newer = analyze(area)
        assertEquals(CvrSbomImportService.ERROR_EXPIRED, assertThrows<UserException> { rest.plan(area, first) }.i18nKey)
        // Imported: the plan is gone.
        await<CvrSbomImportApplyJob>(rest.apply(area, newer, SbomImportApplyRequest(listOf(SbomImportSelection(image.key)))).jobId)
        assertEquals(CvrSbomImportService.ERROR_EXPIRED, assertThrows<UserException> { rest.plan(area, newer) }.i18nKey)

        // Not a zip: the job fails with the message, not with its key.
        val broken = "no zip".toByteArray()
        val jobId = service.startAnalysis(owner, area, broken.size.toLong(), ByteArrayInputStream(broken)).jobId
        val job = await<CvrSbomImportAnalyzeJob>(jobId, AbstractJob.Status.FAILED)
        assertEquals(translate(SbomImportArchive.ERROR_INVALID), job.errorMessage)
    }

    @Test
    fun `the size of a zip is limited per area, set by the security officer only`() {
        val owner = user("sbom-limit-owner", null)
        val officer = user("sbom-limit-officer", CvrRole.SECURITY_OFFICER)
        val groupId = (initTestDB.getGroup("cveradar-sbom-limit-g") ?: initTestDB.addGroup("cveradar-sbom-limit-g")).id!!
        addToGroup(owner, "cveradar-sbom-limit-g")
        logon(owner)
        val area = inventory.createArea(AreaRequest(code = "SBL", name = "Limit", groupId = groupId, sbomImportMaxMb = 1))
        val row = { inventory.tree().rows.single { it.code == "SBL" } }
        // Not the officer: ignored.
        assertNull(row().sbomImportMaxMb)
        assertEquals(cveRadarConfig.sbomImport.maxSizeMb, row().sbomImportLimitMb)

        logon(officer)
        assertThrows<UserException> {
            inventory.updateArea(area, AreaRequest(code = "SBL", name = "Limit", groupId = groupId, sbomImportMaxMb = 100_000))
        }
        inventory.updateArea(area, AreaRequest(code = "SBL", name = "Limit", groupId = groupId, sbomImportMaxMb = 1))
        assertEquals(1, row().sbomImportLimitMb)

        logon(owner)
        val ex = assertThrows<UserException> {
            service.startAnalysis(owner, area, 2L * 1024 * 1024, ByteArrayInputStream(ByteArray(0)))
        }
        assertEquals(CvrSbomImportService.ERROR_TOO_LARGE, ex.i18nKey)
        // Without a length given: counted while storing.
        val big = ByteArray(1024 * 1024 + 1)
        assertThrows<UserException> { service.startAnalysis(owner, area, -1, ByteArrayInputStream(big)) }
        // The owner keeps the size the officer set when changing the area.
        inventory.updateArea(area, AreaRequest(code = "SBL", name = "Limit 2", groupId = groupId))
        assertEquals(1, row().sbomImportMaxMb)
    }
}
