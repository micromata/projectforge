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