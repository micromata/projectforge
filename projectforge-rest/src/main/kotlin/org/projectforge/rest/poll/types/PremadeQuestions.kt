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


val PREMADE_QUESTIONS = mapOf(
    "HAS_FOOD" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Was willst du essen?",
        type = BaseType.SingleResponseQuestion,
        answers = mutableListOf("Fleisch", "Vegetarisch", "Vegan")
    ),
    "IS_REMOTE" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Nimmst du remote teil?",
        type = BaseType.SingleResponseQuestion,
        answers = mutableListOf("Ja", "Nein")
    ),
    "CAN_HAVE_COMPANIONS" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Nimmst du eine Begleitung mit? (Name der Begleitung)",
        type = BaseType.TextQuestion,
        answers = mutableListOf("")
    ),
    "CAN_HAVE_CHILDREN" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Nimmst du ein Kind mit? (Name des Kindes)",
        type = BaseType.TextQuestion,
        answers = mutableListOf("")
    ),
    "CAN_STAY_OVERNIGHT" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Willst du dort übernachten?",
        type = BaseType.SingleResponseQuestion,
        answers = mutableListOf("Ja", "Nein")
    ),
    "HAS_BREAKFAST" to Question(
        uid = UUID.randomUUID().toString(),
        question = "Willst du am nächsten Tag frühstücken?",
        type = BaseType.SingleResponseQuestion,
        answers = mutableListOf("Ja", "Nein")
    ),
)
