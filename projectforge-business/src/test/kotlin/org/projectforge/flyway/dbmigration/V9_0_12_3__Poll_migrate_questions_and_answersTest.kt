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

package org.projectforge.flyway.dbmigration

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import org.projectforge.flyway.dbmigration.V9_0_12_3__Poll_migrate_questions_and_answers.Companion.convertAnswers
import org.projectforge.flyway.dbmigration.V9_0_12_3__Poll_migrate_questions_and_answers.Companion.convertQuestions
import org.projectforge.flyway.dbmigration.V9_0_12_3__Poll_migrate_questions_and_answers.Companion.convertState
import org.projectforge.flyway.dbmigration.V9_0_12_3__Poll_migrate_questions_and_answers.Companion.normalizeIds

class V9_0_12_3__Poll_migrate_questions_and_answersTest {
    // As the old form stored them: a template taken twice repeats the uid "a".
    private val inputFields = """[
        {"uid":"a","question":"Coming?","type":"PollSingleResponseQuestion","answers":["Yes","No"],"parent":null},
        {"uid":"t","question":"Remarks","type":"PollTextQuestion","answers":[]},
        {"uid":"a","question":"Food?","type":"PollMultiResponseQuestion","answers":["Meat","Fish","Veggie"]}
    ]"""

    @Test
    fun `state by name, a finished poll with or without mail is finished`() {
        assertEquals("RUNNING", convertState("0"))
        assertEquals("FINISHED", convertState("1"))
        assertEquals("FINISHED", convertState("2"))
        assertEquals("FINISHED", convertState("FINISHED_AND_MAIL_SENT"))
        assertEquals("RUNNING", convertState("RUNNING"))
    }

    @Test
    fun `ids sorted, without duplicates, whatever the separator`() {
        assertEquals("1,3,7", normalizeIds("7, 3,1, 3"))
        assertNull(normalizeIds(" "))
        assertNull(normalizeIds(null))
    }

    @Test
    fun `questions get types, option uids and unique uids`() {
        val questions = convertQuestions(inputFields)
        assertEquals(listOf("SINGLE_CHOICE", "TEXT", "MULTIPLE_CHOICE"), questions.map { it["type"] })
        assertEquals("a", questions[0]["uid"])
        assertNotEquals("a", questions[2]["uid"])
        assertEquals(listOf("Meat", "Fish", "Veggie"), (questions[2]["options"] as List<*>).map { (it as Map<*, *>)["text"] })
        assertEquals(emptyList<Any>(), questions[1]["options"])
    }

    @Test
    fun `answers matched by position, true and 'true' both chosen`() {
        val questions = convertQuestions(inputFields)
        val responses = """[
            {"uid":"r1","questionUid":"a","answers":[false,"true"]},
            {"uid":"r2","questionUid":"t","answers":["See you"]},
            {"uid":"r3","questionUid":"a","answers":[true,false,"true"]}
        ]"""
        val answers = convertAnswers(responses, questions)
        fun optionUid(question: Int, option: Int) =
            ((questions[question]["options"] as List<*>)[option] as Map<*, *>)["uid"]
        assertEquals(3, answers.size)
        assertEquals(listOf(optionUid(0, 1)), answers[0]["optionUids"])
        assertEquals("See you", answers[1]["text"])
        assertEquals(questions[2]["uid"], answers[2]["questionUid"])
        assertEquals(listOf(optionUid(2, 0), optionUid(2, 2)), answers[2]["optionUids"])
    }

    @Test
    fun `unanswered questions are left out`() {
        val questions = convertQuestions(inputFields)
        val answers = convertAnswers("""[{"answers":[false,false]},{"answers":[""]}]""", questions)
        assertEquals(0, answers.size)
        assertEquals(0, convertAnswers(null, questions).size)
    }
}
