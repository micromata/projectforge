package org.projectforge.business.poll.filter

import org.projectforge.common.i18n.I18nEnum

enum class PollAssignment(val key: String) : I18nEnum {
    OWNER("owner"), ACCESS("access"), ATTENDEE("attendee"), OTHER("other");

    override val i18nKey: String
        get() = ("pollAssignment.$key")
}