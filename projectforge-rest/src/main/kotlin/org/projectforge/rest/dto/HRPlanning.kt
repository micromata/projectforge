package org.projectforge.rest.dto

import org.jetbrains.kotlin.backend.common.onlyIf
import org.projectforge.business.humanresources.HRPlanningDO
import java.math.BigDecimal
import java.time.LocalDate


class HRPlanning(
        var week: LocalDate? = null,
        var formattedWeekOfYear: String? = null,
        var totalHours: BigDecimal? = null,
        var totalUnassignedHours: BigDecimal? = null
): BaseDTO<HRPlanningDO>() {
    var user: User? = User()

    fun initialize(obj: HRPlanningDO) {
        copyFrom(obj)

        formattedWeekOfYear = obj.formattedWeekOfYear
        totalHours = obj.totalHours
        totalUnassignedHours = obj.totalUnassignedHours

        if(obj.user != null){
            this.user!!.initialize(obj.user!!)
        }
    }
}