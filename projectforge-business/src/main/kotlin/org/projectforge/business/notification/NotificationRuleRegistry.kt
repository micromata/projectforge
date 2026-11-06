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

import org.springframework.beans.factory.ObjectProvider
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

/**
 * The rule types: every [NotificationRuleHandler] bean (of the core and of the plugins) registers its type by being a
 * bean, so nothing else knows the single types. Also reads, writes and checks the values of their params.
 */
@Service
class NotificationRuleRegistry {
    // A provider, not the list: a handler may depend on beans depending on this registry (e.g. the rule dao).
    @Autowired
    private lateinit var handlerProvider: ObjectProvider<NotificationRuleHandler>

    private val handlers: Map<String, NotificationRuleHandler> by lazy {
        val list = handlerProvider.orderedStream().toList()
        list.groupBy { it.ruleType }.filterValues { it.size > 1 }.keys.let { duplicates ->
            check(duplicates.isEmpty()) { "Notification rule types registered more than once: $duplicates" }
        }
        list.sortedBy { it.order }.associateBy { it.ruleType }
    }

    /** All available rule types, in the order of the editor. */
    val all: List<NotificationRuleHandler>
        get() = handlers.values.filter { it.isAvailable }

    /**
     * The handler of the type, null if no handler is registered (e.g. its plugin isn't installed any more) or it isn't
     * available ([NotificationRuleHandler.isAvailable]).
     */
    fun handler(ruleType: String?): NotificationRuleHandler? = ruleType?.let { handlers[it] }?.takeIf { it.isAvailable }

    fun handler(rule: NotificationRuleDO): NotificationRuleHandler? = handler(rule.ruleType)

    /** See [NotificationRuleHandler.isManualDoneForced]. */
    fun isManualDoneForced(rule: NotificationRuleDO): Boolean =
        handler(rule)?.isManualDoneForced(rule.display) == true

    /** The values of the params of the rule's type by key (defaults for missing ones), as written to the JSON. */
    fun values(rule: NotificationRuleDO): Map<String, Any?> {
        val params = rule.readParams()
        return handler(rule)?.params.orEmpty().associate { it.key to it.jsonValueOf(params) }
    }

    /**
     * Writes the given values (e.g. posted by the editor) into the params: those of the type only (left-overs of
     * another type are dropped), unreadable ones as null (refused by [errors]).
     */
    fun writeValues(rule: NotificationRuleDO, values: Map<String, Any?>) {
        val params = rule.readParams()
        params.values.clear()
        handler(rule)?.params.orEmpty().forEach { params.values[it.key] = it.jsonValueOfRaw(values[it.key]) }
        rule.writeParams(params)
    }

    /** The errors of the params of the rule by key: the checks of the params, then the handler's own ones. */
    fun errors(rule: NotificationRuleDO): Map<String, NotificationRuleParam.Error> {
        val handler = handler(rule) ?: return emptyMap()
        val params = rule.readParams()
        val errors = handler.params.mapNotNull { param -> param.errorOf(params)?.let { param.key to it } }.toMap()
        return errors + handler.paramErrors(rule).filterKeys { it !in errors }
            .mapValues { NotificationRuleParam.Error(it.value) }
    }
}

private fun <T : Any> NotificationRuleParam<T>.jsonValueOf(params: NotificationParams): Any? = toJson(valueOf(params))

private fun <T : Any> NotificationRuleParam<T>.jsonValueOfRaw(raw: Any?): Any? = toJson(parse(raw))

private fun <T : Any> NotificationRuleParam<T>.errorOf(params: NotificationParams): NotificationRuleParam.Error? =
    error(parse(params.values[key]))
