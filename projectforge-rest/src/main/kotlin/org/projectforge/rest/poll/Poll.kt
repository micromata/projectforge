package org.projectforge.rest.poll

import org.checkerframework.checker.guieffect.qual.UIType
import org.projectforge.business.fibu.EmployeeDO
import org.projectforge.business.fibu.EmployeeSalaryType
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.ui.UIDataType
import java.math.BigDecimal
import java.util.*

class Poll(
    var title: String? = null,
    var description: String? = null,
    var owner: EmployeeDO? = null, // EmployeeDO -> PFUserDO
    var location: String? = null,
    var date: Date? = null,
    var deadline: Date? = null,
    var inputFields: List<InputField>? = null,
    var canSeeResultUsers: List<EmployeeDO>? = null,
    var canEditPollUsers: List<EmployeeDO>? = null,
    var canVoteInPoll: List<EmployeeDO>? = null
) : BaseDTO<PollDO>() {
    class InputField(
        var type: UIDataType? = null,
        var name: String? = null,
        var value: Any? = null,
    )
}