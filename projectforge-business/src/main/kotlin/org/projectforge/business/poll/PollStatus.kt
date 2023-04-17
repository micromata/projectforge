package org.projectforge.business.poll

import org.projectforge.common.i18n.I18nEnum

enum class PollStatus(val key: String): I18nEnum {
    ACTIVE("active"), EXPIRED("expired");

    override val i18nKey: String
        get() = "poll.$key"
}