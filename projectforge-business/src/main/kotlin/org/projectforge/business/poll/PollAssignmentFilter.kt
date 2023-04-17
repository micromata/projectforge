package org.projectforge.business.poll

import org.projectforge.framework.persistence.api.impl.CustomResultFilter

class PollAssignmentFilter(val values: List<PollAssignment>): CustomResultFilter<PollDO> {

    override fun match(list: MutableList<PollDO>, element: PollDO): Boolean {
        return values.contains(element.getPollAssignment())
    }
}