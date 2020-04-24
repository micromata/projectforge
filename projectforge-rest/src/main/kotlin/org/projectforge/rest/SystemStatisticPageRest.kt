package org.projectforge.rest

import mu.KotlinLogging
import org.projectforge.business.task.TaskDO
import org.projectforge.business.tasktree.TaskTreeHelper
import org.projectforge.business.timesheet.TimesheetDO
import org.projectforge.framework.persistence.api.HibernateUtils
import org.projectforge.framework.persistence.history.entities.PfHistoryMasterDO
import org.projectforge.framework.persistence.user.api.ThreadLocalUserContext
import org.projectforge.framework.persistence.user.entities.PFUserDO
import org.projectforge.framework.time.DateHelper
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
    private val dataSource: DataSource? = null

    class SystemStatisticData(
            var systemLoadAverage: BigDecimal? = null,
            var memoryStatistics: MutableMap<String, String>? = HashMap(),
            var totalNumberOfTimesheets: String? = null,
            var totalNumberOfTimesheetDurations: String? = null,
            var totalNumberOfUsers: String? = null,
            var totalNumberOfTasks: String? = null,
            var totalNumberOfHistoryEntries: String? = null
    )

    fun getData(): SystemStatisticData {
        val data = SystemStatisticData()

        // First: Get the system load average (don't measure gc run ;-)
        val osBean = ManagementFactory.getOperatingSystemMXBean()
        val systemLoadAverage = BigDecimal(osBean.systemLoadAverage).setScale(2, RoundingMode.HALF_UP)
        data.systemLoadAverage = systemLoadAverage
        log.info("System load average: $systemLoadAverage")

        // Second: run GC and measure memory consumption before getting database statistics.
        System.gc()
        for (mpBean in ManagementFactory.getMemoryPoolMXBeans()) {
            if (mpBean.type == MemoryType.HEAP) {
                val usageBean = mpBean.usage
                val usage = StringBuilder()
                        .append("max=").append(NumberHelper.formatBytes(usageBean.max))
                        .append(", used=").append(NumberHelper.formatBytes(usageBean.used))
                        .append(", committed=").append(NumberHelper.formatBytes(usageBean.committed))
                        .append(", init=").append(NumberHelper.formatBytes(usageBean.init))
                        .toString()
                data.memoryStatistics!!["Memory " + mpBean.name] = usage
                log.info("Memory: $usage")
            }
        }

        // Finally, the database statistics.
        val jdbc = JdbcTemplate(dataSource)
        data.totalNumberOfTimesheets = NumberFormatter.format(getTableCount(jdbc, TimesheetDO::class.java))
        val taskTree = TaskTreeHelper.getTaskTree()
        val totalDuration = taskTree.rootTaskNode.getDuration(taskTree, true)
        var totalPersonDays: BigDecimal? = BigDecimal(totalDuration).divide(DateHelper.SECONDS_PER_WORKING_DAY, 2, RoundingMode.HALF_UP)
        totalPersonDays = NumberHelper.setDefaultScale(totalPersonDays)
        data.totalNumberOfTimesheetDurations = NumberHelper.getNumberFractionFormat(ThreadLocalUserContext.getLocale(), totalPersonDays!!.scale()).format(totalPersonDays)
        data.totalNumberOfUsers = NumberFormatter.format(getTableCount(jdbc, PFUserDO::class.java))
        data.totalNumberOfTasks = NumberFormatter.format(getTableCount(jdbc, TaskDO::class.java))
        val totalNumberOfHistoryEntries = getTableCount(jdbc, PfHistoryMasterDO::class.java) + getTableCount(jdbc, PfHistoryMasterDO::class.java)
        data.totalNumberOfHistoryEntries =  NumberFormatter.format(totalNumberOfHistoryEntries)

        return data
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
        val data = getData()

        val layout = UILayout("system.statistics.title")

        val col1 = UICol()
                .add(UILabel("Total number of time sheets"))
                .add(UILabel("Total duration over all time sheets (pd)"))
                .add(UILabel("Total number of users"))
                .add(UILabel("Total number of structure elements"))
                .add(UILabel("Total number of history entries"))

        val col2 = UICol()
                .add(UILabel(data.totalNumberOfTimesheets))
                .add(UILabel(data.totalNumberOfTimesheetDurations))
                .add(UILabel(data.totalNumberOfUsers))
                .add(UILabel(data.totalNumberOfTasks))
                .add(UILabel(data.totalNumberOfHistoryEntries))

        for ((key, value) in data.memoryStatistics!!) {
            col1.add(UILabel(key))
            col2.add(UILabel(value))
        }

        col1.add((UILabel("System load average")))
        col2.add(UILabel(data.systemLoadAverage.toString()))

        layout.add(UIRow()
                .add(col1)
                .add(col2))

        LayoutUtils.process(layout)

        layout.postProcessPageMenu()

        return FormLayoutData(data, layout, createServerData(request))
    }
}