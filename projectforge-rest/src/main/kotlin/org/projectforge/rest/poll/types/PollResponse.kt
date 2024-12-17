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

import com.fasterxml.jackson.databind.ObjectMapper
import org.projectforge.business.poll.PollDO
import org.projectforge.business.poll.PollResponseDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.dto.BaseDTO

class PollResponse : BaseDTO<PollResponseDO>() {
    var poll: PollDO? = null
    var owner: PFUserDO? = null
    var responses: MutableList<QuestionAnswer>? = mutableListOf()
    override fun copyTo(dest: PollResponseDO) {
        if (!this.responses.isNullOrEmpty()) {
            dest.responses = ObjectMapper().writeValueAsString(this.responses)
        }
        super.copyTo(dest)
    }

    override fun copyFrom(src: PollResponseDO) {
        if (!src.responses.isNullOrEmpty()) {
            val a = ObjectMapper().readValue(src.responses, MutableList::class.java)
            this.responses = a.map { QuestionAnswer().toObject(ObjectMapper().writeValueAsString(it)) }.toMutableList()
        }
        super.copyFrom(src)
    }
}

class QuestionAnswer {
    var uid: String? = null
    var questionUid: String? = ""
    var answers: MutableList<Any>? = mutableListOf()

    fun toObject(string: String): QuestionAnswer {
        return ObjectMapper().readValue(string, QuestionAnswer::class.java)
    }
}