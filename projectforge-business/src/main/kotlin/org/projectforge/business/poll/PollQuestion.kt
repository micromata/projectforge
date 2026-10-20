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

/** The kind of a [PollQuestion]: a free text, or one or several of its options. */
enum class PollQuestionType {
    TEXT, SINGLE_CHOICE, MULTIPLE_CHOICE;

    val isChoice: Boolean
        get() = this != TEXT
}

/** An option of a choice question. Answers name it by its [uid], never by its position. */
class PollOption(
    var uid: String? = null,
    var text: String? = null,
)

/** A question of a poll, stored as json in [PollDO.questions]. */
class PollQuestion(
    var uid: String? = null,
    var text: String? = null,
    var type: PollQuestionType = PollQuestionType.TEXT,
    var options: MutableList<PollOption> = mutableListOf(),
)

/**
 * The answer to one question, stored as json in [PollResponseDO.answers]: the [text] of a text question, the
 * [optionUids] chosen of a choice question.
 */
class PollAnswer(
    var questionUid: String? = null,
    var text: String? = null,
    var optionUids: MutableList<String> = mutableListOf(),
)

/** Reads and writes the json of [PollQuestion]s and [PollAnswer]s. */
object PollJson {
    private val questionsType = object : TypeReference<List<PollQuestion>>() {}
    private val answersType = object : TypeReference<List<PollAnswer>>() {}

    fun questions(json: String?): MutableList<PollQuestion> =
        if (json.isNullOrBlank()) mutableListOf()
        else JsonUtils.fromJson(json, questionsType, failOnUnknownProps = false)?.toMutableList() ?: mutableListOf()

    fun answers(json: String?): MutableList<PollAnswer> =
        if (json.isNullOrBlank()) mutableListOf()
        else JsonUtils.fromJson(json, answersType, failOnUnknownProps = false)?.toMutableList() ?: mutableListOf()

    fun toJson(list: List<*>): String = JsonUtils.toJson(list)

    fun newUid(): String = UUID.randomUUID().toString()

    /**
     * The questions with new uids (and new uids of their options): a copy taken into another poll must not share
     * them, an answer is found by them.
     */
    fun withNewUids(questions: List<PollQuestion>): MutableList<PollQuestion> =
        questions.map { question ->
            PollQuestion(
                uid = newUid(),
                text = question.text,
                type = question.type,
                options = question.options.map { PollOption(newUid(), it.text) }.toMutableList(),
            )
        }.toMutableList()
}
