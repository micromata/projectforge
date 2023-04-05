package org.projectforge.rest.poll.types

enum class BaseType {
    JaNeinFrage,
    DatumsAbfrage,
    MultipleChoices,
    FreiTextFrage,
    DropDownFrage
}
class Frage(
    val uid: String,
    val question: String,
    val type: BaseType,
    var antworten: List<AntwortMöglichkeiten>,
    var perrent: String?
){

}
