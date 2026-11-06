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

package org.projectforge.business.notification

/**
 * A setting of a rule type, declared by its handler ([NotificationRuleHandler.params]): the editor shows it, the
 * service checks it ([error]) and the handler reads its value ([of]). Stored flat in the JSON of
 * [NotificationRuleDO.params] under [key] ([NotificationParams.values]), as the rules saved before.
 */
sealed class NotificationRuleParam<T : Any>(
    val key: String,
    /** The value of a new rule and of a rule without (or with an unreadable) value. */
    val default: T?,
    val i18nKey: String = "notification.params.$key",
    val hintKey: String? = null,
) {
    enum class Type { INT, BOOLEAN, SELECT, ENTITY }

    /** An error of a value: the i18n key with its arguments. */
    class Error(val i18nKey: String, vararg val args: Any)

    abstract val type: Type

    /** The stored value of the rule, the [default] if it has none (or an unreadable one). */
    fun valueOf(params: NotificationParams): T? = parse(params.values[key]) ?: default

    /** The value from the JSON (stored or posted by the editor), null if missing or not readable. */
    abstract fun parse(raw: Any?): T?

    /** The value as stored in the JSON. */
    open fun toJson(value: T?): Any? = value

    /** The error of a posted value (parsed), null if valid. */
    open fun error(value: T?): Error? = if (value == null) Error(REQUIRED) else null

    /** A param always having a value (its [default] at least), read by the handler with [of]. */
    sealed class WithDefault<T : Any>(key: String, default: T, i18nKey: String, hintKey: String?) :
        NotificationRuleParam<T>(key, default, i18nKey, hintKey) {
        private val defaultValue: T = default

        fun of(rule: NotificationRuleDO): T = of(rule.readParams())

        fun of(params: NotificationParams): T = parse(params.values[key]) ?: defaultValue
    }

    /** A whole number between [min] and [max] (the error [errorKey], or the generic range message). */
    class IntParam(
        key: String,
        default: Int,
        val min: Int,
        val max: Int,
        val errorKey: String? = null,
        i18nKey: String = "notification.params.$key",
        hintKey: String? = null,
    ) : WithDefault<Int>(key, default, i18nKey, hintKey) {
        override val type = Type.INT

        override fun parse(raw: Any?): Int? = when (raw) {
            is Number -> raw.toInt()
            is String -> raw.trim().toIntOrNull()
            else -> null
        }

        override fun error(value: Int?): Error? {
            value ?: return super.error(null)
            if (value !in min..max) {
                return errorKey?.let { Error(it) } ?: Error(RANGE, min, max)
            }
            return null
        }
    }

    class BooleanParam(
        key: String,
        default: Boolean,
        i18nKey: String = "notification.params.$key",
        hintKey: String? = null,
    ) : WithDefault<Boolean>(key, default, i18nKey, hintKey) {
        override val type = Type.BOOLEAN

        override fun parse(raw: Any?): Boolean? = when (raw) {
            is Boolean -> raw
            is String -> raw.toBooleanStrictOrNull()
            else -> null
        }
    }

    /**
     * One of the [options], stored by name. The labels are `<i18nKey>.<option>` if [translateOptions], the names
     * otherwise (e.g. `HIGH`).
     */
    class SelectParam<E : Any>(
        key: String,
        default: E,
        val options: List<E>,
        val translateOptions: Boolean = true,
        private val nameOf: (E) -> String = { "$it" },
        i18nKey: String = "notification.params.$key",
        hintKey: String? = null,
    ) : WithDefault<E>(key, default, i18nKey, hintKey) {
        override val type = Type.SELECT

        val optionNames: List<String>
            get() = options.map(nameOf)

        override fun parse(raw: Any?): E? = (raw as? String)?.let { name -> options.find { nameOf(it) == name } }

        override fun toJson(value: E?): Any? = value?.let(nameOf)

        companion object {
            /** All values of the enum, stored by their names (as Jackson writes an enum). */
            inline fun <reified E : Enum<E>> ofEnum(
                key: String,
                default: E,
                translateOptions: Boolean = true,
                hintKey: String? = null,
            ) = SelectParam(key, default, enumValues<E>().toList(), translateOptions, { it.name }, hintKey = hintKey)
        }
    }

    /**
     * A reference to an entity by id, picked by the autocompletion of the rest category [entity] (e.g. `cvrcase`). Its
     * name comes from [NotificationRuleHandler.paramDisplayNames].
     */
    class EntityParam(
        key: String,
        val entity: String,
        i18nKey: String = "notification.params.$key",
        hintKey: String? = null,
    ) : NotificationRuleParam<Long>(key, null, i18nKey, hintKey) {
        override val type = Type.ENTITY

        /** The id of the rule, null if none is chosen. */
        fun of(rule: NotificationRuleDO): Long? = valueOf(rule.readParams())

        /** An id, also as posted by the editor (`{id, displayName}`). */
        override fun parse(raw: Any?): Long? = when (raw) {
            is Number -> raw.toLong()
            is String -> raw.trim().toLongOrNull()
            is Map<*, *> -> parse(raw["id"])
            else -> null
        }
    }

    companion object {
        /** The value is required (the caller adds the translated label as argument). */
        const val REQUIRED = "validation.error.fieldRequired"

        /** The value is out of range (min and max are the arguments). */
        const val RANGE = "notification.rule.error.range"
    }
}
