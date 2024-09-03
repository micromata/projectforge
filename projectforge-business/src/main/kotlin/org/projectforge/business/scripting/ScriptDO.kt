/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.business.scripting

import com.fasterxml.jackson.annotation.JsonIgnore
import jakarta.persistence.*
import org.apache.commons.lang3.StringUtils
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.search.mapper.pojo.automaticindexing.ReindexOnUpdate
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.*
import org.hibernate.type.SqlTypes
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.jcr.AttachmentsInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.history.NoHistory
import org.projectforge.framework.persistence.user.entities.PFUserDO
import java.io.UnsupportedEncodingException

/**
 * Scripts can be stored and executed by authorized users.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_SCRIPT")
@NamedQueries(
    NamedQuery(name = ScriptDO.SELECT_BY_NAME, query = "from ScriptDO where lower(name) like :name")
)
open class ScriptDO : DefaultBaseDO(), AttachmentsInfo {
    enum class ScriptType {
        KOTLIN, GROOVY,

        /**
         * Script sniplet may only be included by other and are not executable itself.
         */
        INCLUDE
    }

    @PropertyInfo(i18nKey = "scripting.script.name")
    @FullTextField
    @get:Column(length = 255, nullable = false)
    open var name: String? = null // 255 not null

    @PropertyInfo(i18nKey = "scripting.script.type")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var type: ScriptType? = null

    /**
     * If set, the script will be executed by [executableByUsers] or [executableByGroups] with full rights of this executeAsUser!!!
     */
    @PropertyInfo(i18nKey = "scripting.script.executeAsUser", tooltip = "scripting.script.executeAsUser.info")
    @IndexedEmbedded(includeDepth = 1, includeEmbeddedObjectId = true)
    @IndexingDependency(reindexOnUpdate = ReindexOnUpdate.SHALLOW)
    @get:ManyToOne(fetch = FetchType.LAZY)
    @get:JoinColumn(name = "execute_as_user_id", nullable = true)
    open var executeAsUser: PFUserDO? = null

    /**
     * If given, users assigned to at least one of these groups are able to execute this script.
     * CSV of group ids (coma separated)
     */
    @PropertyInfo(i18nKey = "scripting.script.executableByGroups", tooltip = "scripting.script.executableByGroups.info")
    @get:Column(name = "executable_by_group_ids", length = 10000)
    open var executableByGroupIds: String? = null

    /**
     * If given, these users are able to execute this script.
     * CSV of user-ids (coma separated)
     */
    @PropertyInfo(i18nKey = "scripting.script.executableByUsers", tooltip = "scripting.script.executableByUsers.info")
    @get:Column(name = "executable_by_user_ids", length = 10000)
    open var executableByUserIds: String? = null

    @PropertyInfo(i18nKey = "description", tooltip = "scripting.script.description.tooltip")
    @FullTextField
    @get:Column(length = 4000)
    open var description: String? = null

    /**
     * Please note: script is not historizable. Therefore there is now history of scripts.
     */
    @JsonIgnore
    @NoHistory
    @get:Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.BLOB)
    @get:Column
    open var script: ByteArray? = null

    /**
     * Instead of historizing the script the last version of the script after changing it will stored in this field.
     */
    @JsonIgnore
    @NoHistory
    @get:Basic(fetch = FetchType.LAZY)
    @get:Column(name = "script_backup")
    @JdbcTypeCode(SqlTypes.BLOB)
    open var scriptBackup: ByteArray? = null

    @JsonIgnore
    @NoHistory
    @get:Basic(fetch = FetchType.LAZY)
    @get:Column
    @JdbcTypeCode(SqlTypes.BLOB)
    open var file: ByteArray? = null

    @PropertyInfo(i18nKey = "file", tooltip = "scripting.script.editForm.file.tooltip")
    @FullTextField
    @get:Column(name = "file_name", length = 255)
    open var filename: String? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterName")
    @FullTextField
    @get:Column(length = PARAMETER_NAME_MAX_LENGTH)
    open var parameter1Name: String? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterType")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var parameter1Type: ScriptParameterType? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterName")
    @FullTextField
    @get:Column(length = PARAMETER_NAME_MAX_LENGTH)
    open var parameter2Name: String? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterType")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var parameter2Type: ScriptParameterType? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterName")
    @FullTextField
    @get:Column(length = PARAMETER_NAME_MAX_LENGTH)
    open var parameter3Name: String? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterType")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var parameter3Type: ScriptParameterType? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterName")
    @FullTextField
    @get:Column(length = PARAMETER_NAME_MAX_LENGTH)
    open var parameter4Name: String? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterType")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var parameter4Type: ScriptParameterType? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterName")
    @FullTextField
    @get:Column(length = PARAMETER_NAME_MAX_LENGTH)
    open var parameter5Name: String? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterType")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var parameter5Type: ScriptParameterType? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterName")
    @FullTextField
    @get:Column(length = PARAMETER_NAME_MAX_LENGTH)
    open var parameter6Name: String? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterType")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var parameter6Type: ScriptParameterType? = null

    open var scriptAsString: String?
        @Transient
        get() = convert(script)
        set(script) {
            this.script = convert(script)
        }

    open var scriptBackupAsString: String?
        @Transient
        get() = convert(scriptBackup)
        set(scriptBackup) {
            this.scriptBackup = convert(scriptBackup)
        }

    /**
     * Must be set manually.
     */
    @get:Transient
    @PropertyInfo(i18nKey = "scripting.script.includes")
    open var includes: MutableSet<ScriptDO>? = null

    /**
     * Requires, that includes was set before (also of the included script objects itself).
     */
    @get:Transient
    open val includesRecursive: MutableSet<ScriptDO>?
        get() {
            val set = mutableSetOf<ScriptDO>()
            buildIncludesRecursive(set)
            return if (set.isEmpty()) null else set
        }

    private fun buildIncludesRecursive(set: MutableSet<ScriptDO>) {
        includes?.forEach { include ->
            if (set.any { it.id == include.id }) {
                // Avoid circular references (endless loops)
                return // current script already processed...
            }
            set.add(include)
            include.buildIncludesRecursive(set)
        }
    }

    @JsonIgnore
    @FullTextField
    @NoHistory
    @get:Column(length = 10000, name = "attachments_names")
    override var attachmentsNames: String? = null

    @JsonIgnore
    @FullTextField
    @NoHistory
    @get:Column(length = 10000, name = "attachments_ids")
    override var attachmentsIds: String? = null

    @JsonIgnore
    @NoHistory
    @get:Column(length = 10000, name = "attachments_counter")
    override var attachmentsCounter: Int? = null

    @JsonIgnore
    @NoHistory
    @get:Column(length = 10000, name = "attachments_size")
    override var attachmentsSize: Long? = null

    @PropertyInfo(i18nKey = "attachment")
    @JsonIgnore
    @get:Column(length = 10000, name = "attachments_last_user_action")
    override var attachmentsLastUserAction: String? = null

    private fun convert(bytes: ByteArray?): String? {
        if (bytes == null) {
            return null
        }
        var str: String? = null
        try {
            str = String(bytes, Charsets.UTF_8)
        } catch (ex: UnsupportedEncodingException) {
            log.error("Exception encountered while convering byte[] to String: " + ex.message, ex)
        }

        return str
    }

    private fun convert(str: String?): ByteArray? {
        if (str == null) {
            return null
        }
        var bytes: ByteArray? = null
        try {
            bytes = str.toByteArray(charset("UTF-8"))
        } catch (ex: UnsupportedEncodingException) {
            log.error("Exception encountered while convering String to bytes: " + ex.message, ex)
        }

        return bytes
    }

    @Transient
    fun getParameterNames(capitalize: Boolean): String {
        val buf = StringBuffer()
        var first = appendParameterName(buf, parameter1Name, capitalize, true)
        first = appendParameterName(buf, parameter2Name, capitalize, first)
        first = appendParameterName(buf, parameter3Name, capitalize, first)
        first = appendParameterName(buf, parameter4Name, capitalize, first)
        first = appendParameterName(buf, parameter5Name, capitalize, first)
        appendParameterName(buf, parameter6Name, capitalize, first)
        return buf.toString()
    }

    @Transient
    fun getParameterList(allowNullParams: Boolean = false): List<ScriptParameter> {
        val scriptParameters = mutableListOf<ScriptParameter>()
        addParameter(scriptParameters, allowNullParams, parameter1Name, parameter1Type)
        addParameter(scriptParameters, allowNullParams, parameter2Name, parameter2Type)
        addParameter(scriptParameters, allowNullParams, parameter3Name, parameter3Type)
        addParameter(scriptParameters, allowNullParams, parameter4Name, parameter4Type)
        addParameter(scriptParameters, allowNullParams, parameter5Name, parameter5Type)
        addParameter(scriptParameters, allowNullParams, parameter6Name, parameter6Type)
        return scriptParameters
    }

    @Transient
    fun setParameterList(scriptParameters: List<ScriptParameter?>?) {
        scriptParameters ?: return
        scriptParameters.forEachIndexed { index, scriptParameter ->
            when (index) {
                0 -> {
                    parameter1Name = scriptParameter?.parameterName
                    parameter1Type = scriptParameter?.type
                }

                1 -> {
                    parameter2Name = scriptParameter?.parameterName
                    parameter2Type = scriptParameter?.type
                }

                2 -> {
                    parameter3Name = scriptParameter?.parameterName
                    parameter3Type = scriptParameter?.type
                }

                3 -> {
                    parameter4Name = scriptParameter?.parameterName
                    parameter4Type = scriptParameter?.type
                }

                4 -> {
                    parameter5Name = scriptParameter?.parameterName
                    parameter5Type = scriptParameter?.type
                }

                5 -> {
                    parameter6Name = scriptParameter?.parameterName
                    parameter6Type = scriptParameter?.type
                }
            }
        }
    }

    private fun addParameter(
        scriptParameters: MutableList<ScriptParameter>,
        allowNullParams: Boolean,
        parameterName: String?,
        type: ScriptParameterType?
    ) {
        if (allowNullParams || (type != null && !parameterName.isNullOrBlank())) {
            scriptParameters.add(ScriptParameter(parameterName, type))
        }
    }


    private fun appendParameterName(
        buf: StringBuffer, parameterName: String?, capitalize: Boolean,
        first: Boolean
    ): Boolean {
        var firstVar = first
        if (StringUtils.isNotBlank(parameterName)) {
            if (first) {
                firstVar = false
            } else {
                buf.append(", ")
            }
            if (capitalize) {
                buf.append(StringUtils.capitalize(parameterName))
            } else {
                buf.append(parameterName)
            }
        }
        return firstVar
    }

    companion object {
        const val PARAMETER_NAME_MAX_LENGTH = 100

        @JsonIgnore
        private val log = org.slf4j.LoggerFactory.getLogger(ScriptDO::class.java)

        internal const val SELECT_BY_NAME = "ScriptDO_SelectByName"
    }
}
