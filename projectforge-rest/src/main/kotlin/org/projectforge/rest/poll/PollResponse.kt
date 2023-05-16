package org.projectforge.rest.poll

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