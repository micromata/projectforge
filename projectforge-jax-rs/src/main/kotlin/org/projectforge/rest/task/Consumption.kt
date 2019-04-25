package org.projectforge.rest.task

import com.google.gson.annotations.SerializedName
import org.projectforge.business.task.TaskNode
import org.projectforge.business.tasktree.TaskTreeHelper
import org.projectforge.framework.i18n.translate
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.utils.NumberHelper
import java.math.BigDecimal

class Consumption(
        /**
         * 350PT/188PT
         */
        val title: String,
        val status: Status,
        val width: String,
        val id: Int?) {

    enum class Status {
        @SerializedName("progress-done")
        DONE,
        @SerializedName("progress-none")
        NONE,
        @SerializedName("progress-80")
        PROGRESS_80,
        @SerializedName("progress-90")
        PROGRESS_90,
        @SerializedName("progress-overbooked")
        OVERBOOKED,
        @SerializedName("progress-overbooked-min")
        OVERBOOKED_MIN
    }

    companion object {
        fun create(node: TaskNode): Consumption? {
            var maxHours: Int? = null
            var finished = false
            maxHours = node.getTask().getMaxHours()
            finished = node.isFinished()
            val taskTree = TaskTreeHelper.getTaskTree()
            val maxDays: BigDecimal?
            if (maxHours != null && maxHours.toInt() == 0) {
                maxDays = null
            } else {
                maxDays = NumberHelper.setDefaultScale(taskTree.getPersonDays(node))
            }
            var usage = if (node != null)
                BigDecimal(node.getDuration(taskTree, true)).divide(DateHelper.SECONDS_PER_WORKING_DAY, 2,
                        BigDecimal.ROUND_HALF_UP)
            else
                BigDecimal.ZERO
            usage = NumberHelper.setDefaultScale(usage)

            val percentage = if (maxDays != null && maxDays.toDouble() > 0)
                usage.divide(maxDays, 2, BigDecimal.ROUND_HALF_UP).multiply(NumberHelper.HUNDRED).toInt()
            else
                0
            // TODO: What does 10000 / percentage mean?
            val width = if (percentage <= 100) percentage else 10000 / percentage
            //bar.add(AttributeModifier.replace("class", "progress"))
            val status =
                    if (percentage <= 80 || finished && percentage <= 100) {
                        if (percentage > 0) {
                            Status.DONE
                        } else {
                            Status.NONE
                            //progressLabel.setVisible(false)
                        }
                    } else if (percentage <= 90) {
                        Status.PROGRESS_80
                    } else if (percentage <= 100) {
                        Status.PROGRESS_90
                    } else if (finished == true && percentage <= 110) {
                        Status.OVERBOOKED_MIN
                    } else {
                        Status.OVERBOOKED
                    }
            if (maxDays == null && (usage == null || usage.compareTo(BigDecimal.ZERO) == 0)) {
                return null
            }
            val locale = ThreadLocalUserContext.getLocale()
            val usageStr = NumberHelper.getNumberFractionFormat(locale, usage.scale()).format(usage)
            val unitStr = translate("projectmanagement.personDays.short")
            val maxValueStr =
                    if (maxDays != null) {
                        "/${NumberHelper.getNumberFractionFormat(locale, maxDays.scale()).format(maxDays)}$unitStr ($percentage%)"
                    } else {
                        ""
                    }
            val title = "$usageStr$unitStr${maxValueStr}"
            return Consumption(title, status, "$width%", node.taskId)
        }
    }
}
