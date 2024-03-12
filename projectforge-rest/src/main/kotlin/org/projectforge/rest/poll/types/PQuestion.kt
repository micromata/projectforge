package org.projectforge.rest.poll.types

import com.fasterxml.jackson.databind.ObjectMapper

class PQuestion (
    val uid: String? = null,
    val question: String? = "",
    val pType: PreType? = PreType.Neujahrsfeier,
    var answers: MutableList<String>? = mutableListOf(""),
    var parent: String? = null,
    var isRequired: Boolean? = false,
    var numberOfSelect: Int? = 1,
) {
    fun toObject(string: String): PQuestion {
        return ObjectMapper().readValue(string, PQuestion::class.java)
    }

    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }
}

enum class PreType {
    Neujahrsfeier, Sommerfest, Teamessen,
}
