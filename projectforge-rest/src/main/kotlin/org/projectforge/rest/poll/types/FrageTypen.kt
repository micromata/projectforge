package org.projectforge.rest.poll.types


class Frage(
    val uid: String,
    val question: String? = "",
    val type: BaseType = BaseType.FreiTextFrage,
    var antworten: List<String> = mutableListOf(),
    var parent: String? = null,
){

}
enum class BaseType {
    JaNeinFrage,
    DatumsAbfrage,
    MultipleChoices,
    FreiTextFrage,
    DropDownFrage
}