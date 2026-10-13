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


package org.projectforge.rest.scripting

import de.micromata.merlin.utils.ReplaceUtils
import io.github.oshai.kotlinlogging.KotlinLogging
import jakarta.annotation.PostConstruct
import jakarta.servlet.http.HttpServletRequest
import org.projectforge.business.scripting.ExportZipArchive
import org.projectforge.business.scripting.ScriptDO
import org.projectforge.business.scripting.ScriptDao
import org.projectforge.business.scripting.ScriptExecutor
import org.projectforge.jcr.FileInfo
import org.projectforge.rest.config.Rest
import org.projectforge.rest.config.RestUtils
import org.projectforge.rest.core.AbstractDTOEntityRest
import org.projectforge.rest.core.AccessChecked
import org.projectforge.rest.dto.Group
import org.projectforge.rest.dto.PostData
import org.projectforge.rest.dto.Script
import org.projectforge.rest.dto.User
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

private val log = KotlinLogging.logger {}

/**
 * The layout-free REST endpoint of the scripts (list and edit form) for financial and controlling staff,
 * serving the hand-built projectforge-next pages (see components/features/script). The execution of a
 * script is served by [ScriptExecuteRest].
 *
 * The category (`script`) is also the attachment category of the scripts, so it must not change.
 */
@RestController
@RequestMapping("${Rest.URL}/script")
class ScriptEntityRest : AbstractDTOEntityRest<ScriptDO, Script, ScriptDao>(
    baseDaoClazz = ScriptDao::class.java,
    i18nKeyPrefix = "scripting.title",
    cloneSupport = CloneSupport.CLONE,
) {
    /** An example script offered by the list, loaded by [ScriptExecuteRest.load] via its index. */
    class Example(val index: Int, val title: String)

    @Autowired
    private lateinit var scriptExecution: ScriptExecution

    @Autowired
    private lateinit var scriptPageTargets: ScriptPageTargets

    @PostConstruct
    private fun postConstruct() {
        /**
         * Enable attachments for this entity.
         */
        enableJcr()
    }

    override fun newBaseDO(request: HttpServletRequest?): ScriptDO {
        val script = ScriptDO()
        script.type = ScriptDO.ScriptType.KOTLIN
        return script
    }

    override fun transformForDB(dto: Script): ScriptDO {
        val scriptDO = ScriptDO()
        dto.pageTargetIds = scriptPageTargets.sanitize(dto.pageTargetIds)
        dto.copyTo(scriptDO)
        scriptDO.scriptAsString = dto.script
        if (dto.id != null) {
            // Restore filename and file for older scripts, edited by classical Wicket-version:
            val origScript =
                baseDao.find(dto.id) ?: throw IllegalArgumentException("Script with id #${dto.id} not found.")
            scriptDO.filename = origScript.filename
            scriptDO.file = origScript.file
        }
        return scriptDO
    }

    override fun transformFromDB(obj: ScriptDO, editMode: Boolean): Script {
        val script = Script()
        script.filename = obj.filename
        script.copyFrom(obj)
        script.availableVariables =
            scriptExecution.getVariableNames(script, script.parameters, baseDao, this).joinToString()
        script.script = obj.scriptAsString
        // Display names needed by the autocompletion fields:
        Group.restoreDisplayNames(script.executableByGroups)
        User.restoreDisplayNames(script.executableByUsers)
        script.executableByUsersAsString = script.executableByUsers?.joinToString { it.displayName ?: "???" } ?: ""
        script.executableByGroupsAsString = script.executableByGroups?.joinToString { it.displayName ?: "???" } ?: ""

        ScriptExecutor.setIncludingScripts(obj, baseDao)
        obj.includesRecursive?.let { includes ->
            val scriptNames = mutableListOf<String>()
            includes.forEach { include ->
                scriptNames.add(include.name ?: include.id?.toString() ?: "???")
            }
            script.includes = scriptNames.joinToString(separator = "; ")
        }
        return script
    }

    /**
     * The example scripts the list offers for an ad-hoc execution.
     */
    @AccessChecked("No data: static titles of the bundled example scripts")
    @GetMapping("examples")
    fun getExamples(): List<Example> {
        return ExampleScripts.exampleFiles.mapIndexed { index, example -> Example(index, example.title) }
    }

    /**
     * The pages a script can show its button on, for the edit form (see [ScriptDO.pageTargets]).
     */
    @AccessChecked("DAO: select access of ScriptDao (FINANCE/CONTROLLING group); no data: page titles only")
    @GetMapping("pageTargets")
    fun getPageTargets(): List<ScriptPageTargets.PageTarget> {
        baseDao.hasLoggedInUserSelectAccess(true)
        return scriptPageTargets.getAll()
    }

    @AccessChecked("DAO: find of ScriptDao (FINANCE/CONTROLLING group)")
    @GetMapping("downloadBackupScripts/{id}")
    fun downloadBackupScripts(@PathVariable("id") id: Long?): ResponseEntity<*> {
        log.info { "Downloading backup script of script with id=$id" }
        val scriptDO = baseDao.find(id) ?: throw IllegalArgumentException("Script not found.")
        val zip = ExportZipArchive("${scriptDO.name}-backups.zip")
        zip.add(
            ReplaceUtils.encodeFilename("${scriptDO.name}-backup.${baseDao.getScriptSuffix(scriptDO)}"),
            scriptDO.scriptBackupAsString ?: "// empty"
        )
        baseDao.getBackupFiles(scriptDO)?.forEach { file ->
            zip.add(file.name, file.readBytes())
        }
        return RestUtils.downloadFile(zip.filename, zip.asByteArray())
    }

    @AccessChecked("DAO: find of ScriptDao (FINANCE/CONTROLLING group)")
    @GetMapping("downloadEffectiveScript/{id}")
    fun downloadEffectiveScript(@PathVariable("id") id: Long?): ResponseEntity<*> {
        log.info { "Downloading effective script of script with id=$id" }
        val scriptDO = baseDao.find(id) ?: throw IllegalArgumentException("Script not found.")
        val script = transformFromDB(scriptDO)
        val effectiveScript = scriptExecution.getEffectiveScript(script, script.parameters, baseDao, this)
        val filename = ReplaceUtils.encodeFilename("${scriptDO.name}-effective.${baseDao.getScriptSuffix(scriptDO)}")
        return RestUtils.downloadFile(filename, effectiveScript)
    }

    override fun onBeforeUpdate(request: HttpServletRequest, obj: ScriptDO, postData: PostData<Script>) {
        super.onBeforeUpdate(request, obj, postData)
        if (!obj.filename.isNullOrBlank()) {
            obj.file?.let { bytes ->
                // Migration of field script.file to DataTransfer
                val fileInfo = FileInfo(obj.filename, fileSize = bytes.size.toLong())
                attachmentsService.addAttachment(
                    jcrPath!!, fileInfo, bytes, baseDao, obj, attachmentsAccessChecker, allowDuplicateFiles = true
                )
                obj.file = null
                obj.filename = null
            }
        }
    }
}
