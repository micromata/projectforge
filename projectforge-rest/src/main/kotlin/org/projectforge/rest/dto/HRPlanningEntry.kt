package org.projectforge.rest.dto

import org.projectforge.business.humanresources.HRPlanningEntryDO
import org.projectforge.common.i18n.Priority
import java.math.BigDecimal

class HRPlanningEntry(
        var projektNameOrStatus: String? = null,
        var priority: Priority? = null,
        var probability: Int? = null,
        var totalHours: BigDecimal? = null,
        var unassignedHours: BigDecimal? = null,
        var mondayHours: BigDecimal? = null,
        var tuesdayHours: BigDecimal? = null,
        var wednesdayHours: BigDecimal? = null,
        var thursdayHours: BigDecimal? = null,
        var fridayHours: BigDecimal? = null,
        var weekendHours: BigDecimal? = null,
        var description: String? = null
) : BaseDTO<HRPlanningEntryDO>() {
    var planning: HRPlanning? = null
    var projekt: Projekt? = null

    override fun copyFrom(src: HRPlanningEntryDO) {
        super.copyFrom(src)
        src.planning?.let {
            val planning = HRPlanning()
            planning.copyFrom(it)
            this.planning = planning
        }
        src.projekt?.let {
            val projekt = Projekt()
            projekt.copyFrom(it)
            this.projekt = projekt
        }
        projektNameOrStatus = src.projektNameOrStatus
        totalHours = src.totalHours
        unassignedHours = src.unassignedHours
    }
}
