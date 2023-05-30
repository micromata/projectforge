package org.projectforge.business.poll

import org.projectforge.framework.persistence.api.impl.CustomResultFilter

class PollAssignmentFilter(val values: List<PollAssignment>): CustomResultFilter<PollDO> {

    override fun match(list: MutableList<PollDO>, element: PollDO): Boolean {

        element.getPollAssignment().forEach { pollAssignment ->
            if (values.contains(pollAssignment)) {
                return true
            }
        }
        return false
    }
}