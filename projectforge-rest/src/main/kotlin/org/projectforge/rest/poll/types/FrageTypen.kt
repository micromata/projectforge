package org.projectforge.rest.poll.types

import com.fasterxml.jackson.annotation.JsonIgnore
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.time.PFDateTimeUtils
import org.projectforge.framework.time.PFDay
import org.projectforge.framework.time.PFDayUtils
import java.time.LocalDate
import java.time.LocalDateTime


class Frage(
    val uid: String?,
    val question: String? = "",
    val type: BaseType = BaseType.FreiTextFrage,
    var antworten: MutableList<String>? = mutableListOf(""),
    var parent: String? = null,
    var isRequired: Boolean? = false,
    var numberOfSelect: Int? = 1,

)

enum class BaseType {
    JaNeinFrage,
    DatumsAbfrage,
    MultipleChoices,
    FreiTextFrage,
    DropDownFrage
}