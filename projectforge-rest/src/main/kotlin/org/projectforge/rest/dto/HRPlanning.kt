package org.projectforge.rest.dto

import org.projectforge.business.humanresources.HRPlanningDO
import java.math.BigDecimal
import java.time.LocalDate


class HRPlanning(
        var week: LocalDate? = null,
        var formattedWeekOfYear: String? = null,
        var totalHours: BigDecimal? = null,
        var totalUnassignedHours: BigDecimal? = null,
        var user: User? = null
) : BaseDTO<HRPlanningDO>() {

    /**
     * @see copyFrom
     */
    constructor(src: HRPlanningDO) : this() {
        copyFrom(src)
    }

    override fun copyFrom(src: HRPlanningDO) {
        super.copyFrom(src)
        formattedWeekOfYear = src.formattedWeekOfYear
        totalHours = src.totalHours
        totalUnassignedHours = src.totalUnassignedHours
        this.user = src.user?.let {
            User(it)
        }
    }
}
