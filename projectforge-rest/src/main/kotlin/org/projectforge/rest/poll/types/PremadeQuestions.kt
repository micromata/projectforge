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

package org.projectforge.rest.poll.types

import java.util.UUID


val Neujahrsfeier = mapOf(
    "HAS_FOOD" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Was willst du essen?",
        type = BaseType.PollSingleResponseQuestion,
        answers = mutableListOf("Fleisch", "Vegetarisch", "Vegan")
    ),
    "CAN_HAVE_COMPANIONS" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Nimmst du eine Begleitung mit? (Name der Begleitung)",
        type = BaseType.PollTextQuestion,
        answers = mutableListOf("")
    ),
    "CAN_HAVE_CHILDREN" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Nimmst du ein Kind mit? (Name des Kindes)",
        type = BaseType.PollTextQuestion,
        answers = mutableListOf("")
    ),
    "CAN_STAY_OVERNIGHT" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Willst du dort übernachten?",
        type = BaseType.PollSingleResponseQuestion,
        answers = mutableListOf("Ja", "Nein")
    ),
    "HAS_BREAKFAST" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Willst du am nächsten Tag frühstücken?",
        type = BaseType.PollSingleResponseQuestion,
        answers = mutableListOf("Ja", "Nein")
    ),
)

val Sommerfest = mapOf(
    "IS_IN" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Nimmst du teil ?",
        type = BaseType.PollSingleResponseQuestion,
        answers = mutableListOf("Ja", "Nein")
    ),
    "WITH_HOW_MANY" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Kommst du in Begleitung ? Wenn ja mit wie vielen ?",
        type = BaseType.PollTextQuestion,
        answers = mutableListOf("")
    ),
    "CHILDRENS" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Sind Kinder unter deiner Begleitpersonen ?",
        type = BaseType.PollSingleResponseQuestion,
        answers = mutableListOf("Ja", "Nein")
    ),
    "HOW_OLD_ARE_THE_CHILDS" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Wenn du Kinder unter deiner Begleitung hast wie alt sind diese ?",
        type = BaseType.PollTextQuestion,
        answers = mutableListOf("")
    ),
    "CAN_STAY_OVERNIGHT" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Willst du vor ort Übernachten ?",
        type = BaseType.PollSingleResponseQuestion,
        answers = mutableListOf("Ja", "Nein")
    ),
    "WHERE_DO_YOU_WANT_TO_STAY" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Wenn du übernachtest wo möchtest du übernachten ?",
        type = BaseType.PollSingleResponseQuestion,
        answers = mutableListOf("In meinem eigenen Van/Mobil", "Ich bräuchte eine Übernachtungsmöglichkeit", "Ich komme irgendwo anders unter")
    ),
    "WHAT_DO_YOU_EAT" to Question (
        uid = UUID.randomUUID().toString(),
        question = "Welche Art von Ernährung bevorzugst du ?",
        type = BaseType.PollSingleResponseQuestion,
        answers = mutableListOf("Omnivor (Fleisch)", "Pescetarier (Fisch)", "Vegetarisch", "Vegan")
    )
)

val Teamessen = mapOf(
    "IS_IN" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Hast du Zeit und Lust ?",
        type = BaseType.PollSingleResponseQuestion,
        answers = mutableListOf("Ja", "Nein")
    ),
    "WHAT_TO_EAT" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Zur Essens Auswahl gibt es",
        type = BaseType.PollSingleResponseQuestion,
        answers = mutableListOf("Pizza", "Pasta", "Burger")
    ),
    "CAN_STAY_OVERNIGHT" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Welcher dieser Termine passt dir am besten ? (Du kannst auch mehrere ankreuzen)",
        type = BaseType.PollMultiResponseQuestion,
        answers = mutableListOf("", "")
    ),
)
