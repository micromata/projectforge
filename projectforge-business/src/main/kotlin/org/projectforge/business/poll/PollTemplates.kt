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

package org.projectforge.business.poll

import com.fasterxml.jackson.core.type.TypeReference
import org.projectforge.framework.json.JsonUtils
import java.util.*

/**
 * The built-in templates of polls (`poll/templates.json`): generic questions in English and German, offered to
 * every user. Templates of an organization (e.g. its new year's party) are polls marked as [PollDO.template].
 */
object PollTemplates {
    class Text(var en: String? = null, var de: String? = null) {
        fun get(locale: Locale?): String? = if (locale?.language == "de") de ?: en else en ?: de
    }

    class TemplateQuestion(
        var type: PollQuestionType = PollQuestionType.TEXT,
        var text: Text? = null,
        var options: List<Text> = emptyList(),
    )

    class Template(var key: String? = null, var title: Text? = null, var questions: List<TemplateQuestion> = emptyList())

    val templates: List<Template> by lazy {
        val json = PollTemplates::class.java.getResourceAsStream("/poll/templates.json")!!.use {
            it.readBytes().toString(Charsets.UTF_8)
        }
        JsonUtils.fromJson(json, object : TypeReference<List<Template>>() {}, failOnUnknownProps = false)!!
    }

    fun get(key: String?): Template? = templates.find { it.key == key }

    /** The questions of the template in the given language, with new uids. */
    fun questions(template: Template, locale: Locale?): MutableList<PollQuestion> =
        template.questions.map { question ->
            PollQuestion(
                uid = PollJson.newUid(),
                text = question.text?.get(locale),
                type = question.type,
                options = question.options.map { PollOption(PollJson.newUid(), it.get(locale)) }.toMutableList(),
            )
        }.toMutableList()
}
