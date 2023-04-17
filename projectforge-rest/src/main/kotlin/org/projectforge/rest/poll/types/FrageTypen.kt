package org.projectforge.rest.poll.types

import com.fasterxml.jackson.databind.ObjectMapper


class Frage(
    val uid: String? = null,
    val question: String? = "",
    val type: BaseType? = BaseType.FreiTextFrage,
    var antworten: MutableList<String>? = mutableListOf(""),
    var parent: String? = null,
    var isRequired: Boolean? = false,
    var numberOfSelect: Int? = 1,
){
    fun toObject(string:String): Frage {
        return ObjectMapper().readValue(string, Frage::class.java)
    }
    fun toJson(): String {
        return ObjectMapper().writeValueAsString(this)
    }
}

enum class BaseType {
    JaNeinFrage,
    DatumsAbfrage,
    MultipleChoices,
    FreiTextFrage,
    DropDownFrage
}