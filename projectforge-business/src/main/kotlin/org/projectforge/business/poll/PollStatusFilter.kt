package org.projectforge.business.poll

import org.projectforge.framework.persistence.api.impl.CustomResultFilter

class PollStatusFilter(val values: List<PollStatus>): CustomResultFilter<PollDO> {

    override fun match(list: MutableList<PollDO>, element: PollDO): Boolean {
        return values.contains(element.getPollStatus())
    }
}