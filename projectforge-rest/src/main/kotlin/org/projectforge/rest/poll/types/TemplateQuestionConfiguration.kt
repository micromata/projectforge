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
    "CAN_HAVE_COMPANIONS" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Erscheinst du in Begleitung?",
        type = BaseType.PollSingleResponseQuestion,
        answers = mutableListOf("Ohne Begleitung", "Mit Begleitung")
    ),
    "HAS_FOOD" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Gibt es Essenspräferenzen für die Party?",
        type = BaseType.PollSingleResponseQuestion,
        answers = mutableListOf("Vegetarisch", "Vegan", "Pescetarier (Fisch)", "Omnivor (Fleisch)")
    ),
    "SONG_REQUESTs" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Hast du Musikwünsche für die Party?",
        type = BaseType.PollTextQuestion,
        answers = mutableListOf("")
    ),
    "USER_COMMENT" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Hast du weitere Anmerkungen und/oder Wünsche?",
        type = BaseType.PollTextQuestion,
        answers = mutableListOf("")
    )
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
    ),
    "USER_COMMENT" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Hast du weitere Anmerkungen und/oder Wünsche?",
        type = BaseType.PollTextQuestion,
        answers = mutableListOf("")
    )
)

val Teamessen = mapOf(
    "IS_IN" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Nimmst du am gemeinsamen Team-Essen teil?",
        type = BaseType.PollSingleResponseQuestion,
        answers = mutableListOf("Ja", "Nein")
    ),
    "WHAT_TO_EAT" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Folgende Speisen stehen zur Auswahl. Was möchtest du essen?",
        type = BaseType.PollSingleResponseQuestion,
        answers = mutableListOf("Pizza", "Pasta", "Burger")
    ),
    "CAN_STAY_OVERNIGHT" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Welcher dieser Termine passt dir am besten ? (Du kannst auch mehrere ankreuzen)",
        type = BaseType.PollMultiResponseQuestion,
        answers = mutableListOf("", "")
    ),
    "USER_COMMENT" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Hast du weitere Anmerkungen und/oder Wünsche?",
        type = BaseType.PollTextQuestion,
        answers = mutableListOf("")
    )
)
