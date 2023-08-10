/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2023 Micromata GmbH, Germany (www.micromata.com)
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

import com.fasterxml.jackson.databind.ObjectMapper


class Question(
    val uid: String? = null,
    val question: String? = "",
    val type: BaseType? = BaseType.TextQuestion,
    var answers: MutableList<String>? = mutableListOf(""),
    var parent: String? = null,
    var isRequired: Boolean? = false,
    var numberOfSelect: Int? = 1,
) {
    fun toObject(string: String): Question {
        return ObjectMapper().readValue(string, Question::class.java)
    }

    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }
}

enum class BaseType {
    TextQuestion, SingleResponseQuestion, MultiResponseQuestion,
}
