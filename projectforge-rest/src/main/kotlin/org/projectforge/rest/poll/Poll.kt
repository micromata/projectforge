package org.projectforge.rest.poll

import net.sf.mpxj.LocaleData
import org.projectforge.business.poll.PollDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.rest.dto.BaseDTO
import org.projectforge.ui.UIDataType
import java.time.LocalDate
import java.util.*

class Poll(
    var title: String? = null,
    var description: String? = null,
    /*
        var owner: PFUserDO? = null,
    */
    var location: String? = null,
    var deadline: LocalDate? = null,
    var inputFields: List<InputField>? = null,
    var canSeeResultUsers: String? = null,
    var canEditPollUsers: String? = null,
    var canVoteInPoll: String? = null
) : BaseDTO<PollDO>() {
    class InputField(
        var type: UIDataType? = null,
        var name: String? = null,
        var value: Any? = null,
    )
}