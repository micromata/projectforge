package org.projectforge.rest.poll.types


class Frage(
    val uid: String,
    val question: String,
    val type: BaseType,
    var antworten: List<AntwortMöglichkeiten>,
    var perrent: String?
){

}
enum class BaseType {
    JaNeinFrage,
    DatumsAbfrage,
    MultipleChoices,
    FreiTextFrage,
    DropDownFrage
}