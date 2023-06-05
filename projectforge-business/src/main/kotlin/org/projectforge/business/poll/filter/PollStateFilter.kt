package org.projectforge.business.poll.filter;

import org.projectforge.business.poll.PollDO
import org.projectforge.framework.persistence.api.impl.CustomResultFilter

class PollStateFilter(val values: List<PollState>) : CustomResultFilter<PollDO> {

    override fun match(list: MutableList<PollDO>, element: PollDO): Boolean {
        return values.contains(element.getPollStatus())
    }
}
