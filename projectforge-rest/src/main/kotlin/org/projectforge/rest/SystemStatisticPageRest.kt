/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2020 Micromata GmbH, Germany (www.micromata.com)
//
// ProjectForge is dual-licensed.
//
// This community edition is free software; you can redistribute it and/or
// modify it under the terms of the GNU General Public License as published
// by the Free Software Foundation; version 3 of the License.
//
// This community edition is distributed in the hope that it will be useful,
// but WITHOUT ANY WARRANTY; without even the implied warranty of
// MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
// Public License for more details.
//
// You should have received a copy of the GNU General Public License along
// with this program; if not, see http://www.gnu.org/licenses/.
//
/////////////////////////////////////////////////////////////////////////////

package org.projectforge.rest

import mu.KotlinLogging
import org.projectforge.business.task.TaskDO
import org.projectforge.business.tasktree.TaskTreeHelper
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.ToStringUtil
import org.projectforge.framework.calendar.DurationUtils
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateHelper
import org.projectforge.framework.time.PFDateTime
import org.projectforge.framework.utils.NumberFormatter
import org.projectforge.framework.utils.NumberHelper
import org.projectforge.rest.config.Rest
import org.projectforge.rest.core.AbstractDynamicPageRest
import org.projectforge.rest.dto.FormLayoutData
import org.projectforge.ui.*
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.lang.management.ManagementFactory
import java.lang.management.MemoryType
import java.math.BigDecimal
import java.math.RoundingMode
import javax.servlet.http.HttpServletRequest
import javax.sql.DataSource

private val log = KotlinLogging.logger {}

@RestController
@RequestMapping("${Rest.URL}/systemStatistics")
class SystemStatisticPageRest : AbstractDynamicPageRest() {

    @Autowired
    private lateinit var dataSource: DataSource

    class SystemStatisticData(
            val systemLoadAverage: BigDecimal,
            val processUptime: Long,
            val processStartTime: Long,
            val totalNumberOfTimesheets: Int,
            val totalNumberOfTimesheetDurations: BigDecimal,
            val totalNumberOfUsers: Int,
            val totalNumberOfTasks: Int,
            val totalNumberOfHistoryEntries: Int,
            val memoryStatistics: Map<String, MemoryStatistics>
    )

    class MemoryStatistics(
            val max: Long,
            val used: Long,
            val committed: Long,
            val init: Long
    )

    /**
     * Rest service for getting system statistics.
     * @return The system statistics for data base usage as well as for memory usage.
     */
    @GetMapping
    fun getSystemStatistics(): SystemStatisticData {
        // First: Get the system load average (don't measure gc run ;-)
        val osBean = ManagementFactory.getOperatingSystemMXBean()
        val systemLoadAverage = BigDecimal(osBean.systemLoadAverage).setScale(2, RoundingMode.HALF_UP)
        val processUptime = ManagementFactory.getRuntimeMXBean().uptime
        val processStartTime = ManagementFactory.getRuntimeMXBean().startTime
        log.info("System load average: $systemLoadAverage, process start time: ${PFDateTime.from(processStartTime).isoString}, process uptime: ${DurationUtils.getFormattedDaysHoursAndMinutes(processUptime)} [h:mm]")

        val memoriesStatistics = mutableMapOf<String, MemoryStatistics>()
        // Second: run GC and measure memory consumption before getting database statistics.
        System.gc()
        ManagementFactory.getMemoryPoolMXBeans().filter { it.type == MemoryType.HEAP }.forEach { mpBean ->
            val usageBean = mpBean.usage
            memoriesStatistics[mpBean.name] = MemoryStatistics(
                    max = usageBean.max,
                    used = usageBean.used,
                    committed = usageBean.committed,
                    init = usageBean.init
            )
        }

        // Finally, the database statistics.
        val jdbc = JdbcTemplate(dataSource)
        val taskTree = TaskTreeHelper.getTaskTree()
        val totalDuration = taskTree.rootTaskNode.getDuration(taskTree, true)
        var totalPersonDays = BigDecimal(totalDuration).divide(DateHelper.SECONDS_PER_WORKING_DAY, 2, RoundingMode.HALF_UP)
        totalPersonDays = NumberHelper.setDefaultScale(totalPersonDays)

        val statistics = SystemStatisticData(systemLoadAverage = systemLoadAverage,
                processUptime = processUptime,
                processStartTime = processStartTime,
                totalNumberOfTimesheets = getTableCount(jdbc, TimesheetDO::class.java),
                totalNumberOfTimesheetDurations = totalPersonDays,
                totalNumberOfUsers = getTableCount(jdbc, PFUserDO::class.java),
                totalNumberOfTasks = getTableCount(jdbc, TaskDO::class.java),
                totalNumberOfHistoryEntries = getTableCount(jdbc, PfHistoryMasterDO::class.java) + getTableCount(jdbc, PfHistoryMasterDO::class.java),
                memoryStatistics = memoriesStatistics)

        log.info("Statistics: ${ToStringUtil.toJsonString(statistics)}")
        return statistics
    }

    private fun getTableCount(jdbc: JdbcTemplate, entity: Class<*>): Int {
        try {
            return jdbc.queryForObject("SELECT COUNT(*) FROM " + HibernateUtils.getDBTableName(entity), Int::class.java)!!
        } catch (ex: Exception) {
            log.error(ex.message, ex)
            return 0
        }

    }

    @GetMapping("dynamic")
    fun getForm(request: HttpServletRequest): FormLayoutData {
        val statistics = getSystemStatistics()

        val layout = UILayout("system.statistics.title")
                .add(createRow("system.statistics.totalNumberOfTimesheets", format(statistics.totalNumberOfTimesheets)))
                .add(createRow("system.statistics.totalNumberOfTimesheetDurations", format(statistics.totalNumberOfTimesheetDurations, 0)))
                .add(createRow("system.statistics.totalNumberOfUsers", format(statistics.totalNumberOfUsers)))
                .add(createRow("system.statistics.totalNumberOfTasks", format(statistics.totalNumberOfTasks)))
                .add(createRow("system.statistics.totalNumberOfHistoryEntries", format(statistics.totalNumberOfHistoryEntries)))

        statistics.memoryStatistics.forEach { key, value ->
            layout.add(createRow("'$key", format(value)))
        }

        layout.add(createRow("'System load average", format(statistics.systemLoadAverage, 2)))
        layout.add(createRow("'Process start time", "${PFDateTime.from(statistics.processStartTime).isoString} (UTC)"))
        layout.add(createRow("'Process uptime", "${DurationUtils.getFormattedDaysHoursAndMinutes(statistics.processUptime)} [h:mm]"))
        LayoutUtils.process(layout)
        return FormLayoutData(statistics, layout, createServerData(request))
    }

    private fun format(number: Number, scale: Int? = null): String {
        return NumberFormatter.format(number, scale)
    }

    private fun formatBytes(number: Long): String {
        return NumberHelper.formatBytes(number)
    }

    private fun format(memory: MemoryStatistics): String {
        val percent = if (memory.max > 0 && memory.used < memory.max) {
            " (${format(BigDecimal(memory.used).multiply(NumberHelper.HUNDRED).divide(BigDecimal(memory.max), 0, RoundingMode.HALF_UP), 0)}%)"
        } else {
            ""
        }
        val max = if (memory.max > 0) {
            " / ${formatBytes(memory.max)}"
        } else {
            ""
        }
        val used = if (memory.used > 0) {
            formatBytes(memory.used)
        } else {
            "0"
        }

        return "used=[$used$max]$percent, committed=[${formatBytes(memory.committed)}], init=[${formatBytes(memory.init)}]"
    }

    private fun createRow(label: String, value: String): UIRow {
        return UIRow()
                .add(UICol(UILength(12, 6, 6, 4, 3))
                        .add(UILabel(label)))
                .add(UICol(UILength(12, 6, 6, 8, 9))
                        .add(UILabel("'$value")))
    }
}
