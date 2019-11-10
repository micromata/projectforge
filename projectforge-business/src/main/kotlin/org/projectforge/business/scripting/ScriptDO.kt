/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2019 Micromata GmbH, Germany (www.micromata.com)
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

import de.micromata.genome.db.jpa.history.api.NoHistory
import org.apache.commons.lang3.StringUtils
import org.hibernate.annotations.Type
import org.hibernate.search.annotations.Field
import org.hibernate.search.annotations.Index
import org.hibernate.search.annotations.Indexed
import org.hibernate.search.annotations.Store
import org.projectforge.common.anots.PropertyInfo
import org.projectforge.framework.persistence.entities.DefaultBaseDO
import org.projectforge.framework.persistence.utils.ReflectionToString
import java.io.UnsupportedEncodingException
import javax.persistence.*

/**
 * Scripts can be stored and executed by authorized users.
 *
 * @author Kai Reinhard (k.reinhard@micromata.de)
 */
@Entity
@Indexed
@Table(name = "T_SCRIPT", indexes = [Index(name = "idx_fk_t_script_tenant_id", columnList = "tenant_id")])
open class ScriptDO : DefaultBaseDO() {
    private val log = org.slf4j.LoggerFactory.getLogger(ScriptDO::class.java)

    @PropertyInfo(i18nKey = "scripting.script.name")
    @Field
    @get:Column(length = 255, nullable = false)
    open var name: String? = null // 255 not null

    @PropertyInfo(i18nKey = "description")
    @Field
    @get:Column(length = 4000)
    open var description: String? = null // 4000;

    /**
     * Please note: script is not historizable. Therefore there is now history of scripts.
     */
    @NoHistory
    @get:Basic(fetch = FetchType.LAZY)
    @get:Type(type = "binary")
    @get:Column(length = 2000)
    open var script: ByteArray? = null

    /**
     * Instead of historizing the script the last version of the script after changing it will stored in this field.
     */
    @NoHistory
    @get:Basic(fetch = FetchType.LAZY)
    @get:Column(name = "script_backup", length = 2000)
    @get:Type(type = "binary")
    open var scriptBackup: ByteArray? = null

    @NoHistory
    @get:Basic(fetch = FetchType.LAZY)
    @get:Column
    @get:Type(type = "binary")
    open var file: ByteArray? = null

    @Field
    @get:Column(name = "file_name", length = 255)
    open var filename: String? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterName")
    @get:Field
    @get:Column(length = PARAMETER_NAME_MAX_LENGTH)
    open var parameter1Name: String? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterType")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var parameter1Type: ScriptParameterType? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterName")
    @get:Field
    @get:Column(length = PARAMETER_NAME_MAX_LENGTH)
    open var parameter2Name: String? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterType")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var parameter2Type: ScriptParameterType? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterName")
    @get:Field
    @get:Column(length = PARAMETER_NAME_MAX_LENGTH)
    open var parameter3Name: String? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterType")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var parameter3Type: ScriptParameterType? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterName")
    @get:Field
    @get:Column(length = PARAMETER_NAME_MAX_LENGTH)
    open var parameter4Name: String? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterType")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var parameter4Type: ScriptParameterType? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterName")
    @get:Field
    @get:Column(length = PARAMETER_NAME_MAX_LENGTH)
    open var parameter5Name: String? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterType")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var parameter5Type: ScriptParameterType? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterName")
    @get:Field
    @get:Column(length = PARAMETER_NAME_MAX_LENGTH)
    open var parameter6Name: String? = null

    @PropertyInfo(i18nKey = "scripting.script.parameterType")
    @get:Enumerated(EnumType.STRING)
    @get:Column(length = 20)
    open var parameter6Type: ScriptParameterType? = null

    open var scriptAsString: String?
        @Transient
        @Field(index = Index.YES, store = Store.NO)
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

    private fun appendParameterName(buf: StringBuffer, parameterName: String?, capitalize: Boolean,
                                    first: Boolean): Boolean {
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

    /**
     * Returns string containing all fields (except the file) of given object (via ReflectionToStringBuilder).
     */
    override fun toString(): String {
        return object : ReflectionToString(this) {
            override fun accept(f: java.lang.reflect.Field): Boolean {
                return (super.accept(f) && "file" != f.name && "script" != f.name
                        && "scriptBackup" != f.name)
            }
        }.toString()
    }

    companion object {
        const val PARAMETER_NAME_MAX_LENGTH = 100
    }
}
