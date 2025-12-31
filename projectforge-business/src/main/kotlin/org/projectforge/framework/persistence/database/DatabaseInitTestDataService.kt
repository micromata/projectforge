/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2026 Micromata GmbH, Germany (www.micromata.com)
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

package org.projectforge.framework.persistence.database

import org.projectforge.business.scripting.ScriptDO
import org.projectforge.business.scripting.ScriptDao
import org.projectforge.business.scripting.ScriptParameterType
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Service

@Service
open class DatabaseInitTestDataService {

    @Autowired
    private lateinit var scriptDao: ScriptDao

    open fun initAdditionalTestData() {
        /* insertScript(
          "Booked person days per day",
          "Return an Excel sheet containing all (sub) tasks with the booked days per day for the given time period\n" +
              "Use Yellow Logistics as task and 01/01/10 to 05/31/10 as time period for getting some test data.",
          exportTimesheets,
          "task", ScriptParameterType.TASK,
          "timeperiod", ScriptParameterType.TIME_PERIOD
        )*/

        insertScript("JFreeChart", "Demo for creating a simple JFreeChart diagram", exportJFreeChartScript)

        insertScript(
            "Excel export of all user",
            "This short demo shows how easy it is to filter and export data.",
            exportUsersScript
        )

        insertScript(
            "Excel export of time sheets",
            "This short demo shows how easy it is to format and export data as an Excel file.",
            exportTimesheetsScript,
            p1Name = "Period",
            p1Type = ScriptParameterType.TIME_PERIOD,
        )
    }

    private fun insertScript(
        name: String, description: String, script: String, p1Name: String? = null, p1Type: ScriptParameterType? = null,
        p2Name: String? = null, p2Type: ScriptParameterType? = null
    ) {
        val scriptDO = ScriptDO()
        scriptDO.description = description
        scriptDO.name = name
        scriptDO.parameter1Name = p1Name
        scriptDO.parameter1Type = p1Type
        scriptDO.parameter2Name = p2Name
        scriptDO.parameter2Type = p2Type
        scriptDO.script = script.toByteArray()
        scriptDO.type = ScriptDO.ScriptType.KOTLIN
        scriptDao.insert(scriptDO, checkAccess = false)

    }

    companion object {
        private val exportJFreeChartScript = """
            |import org.jfree.chart.*
            |import org.jfree.chart.plot.*
            |import org.jfree.data.general.*
            |import org.projectforge.export.*
            |
            |val dataset = DefaultPieDataset<String>()
            |dataset.setValue("Linux", 15)
            |dataset.setValue("Mac", 8)
            |dataset.setValue("Windows", 70)
            |dataset.setValue("Others", 7)
            |val chart = ChartFactory.createPieChart3D("Users on www.heise.de", dataset, true, true, false)
            |val plot = chart.getPlot() as PiePlot3D
            |
            |val export = ExportJFreeChart(chart, 800, 600)
            |export
        """.trimMargin()

        private val exportUsersScript = """
            |val users = userDao.getList().filter { it.hasSystemAccess() }.sortedBy { it.username }
            |val workbook = ExcelUtils.prepareWorkbook("Userlist.xlsx")
            |val sheet = workbook.createOrGetSheet("Users")
            |workbook.createOrGetCellStyle("integer").dataFormat = workbook.createDataFormat().getFormat("#0")
            |sheet.registerColumns(
            |  "Username|20",
            |  "Full name|fullname|30",
            |  "Email|30",
            |  "Gender|10",
            |  "System access|5",
            |  "Mobile phone|mobilePhone|20",
            |  "Id|10|:integer",
            |  "JIRA username|JiraUsername|20",
            |  "Description|50",
            |)
            |sheet.createRow().fillHeadRow()
            |users.forEach { user ->
            |  val row = sheet.createRow()
            |  ExcelUtils.autoFill(row, user)
            |  row.getCell("System access")?.setCellValue(user.hasSystemAccess())
            |}
            |sheet.setAutoFilter()
            |workbook
        """.trimMargin()

        private val exportTimesheetsScript = """
            |val from = period?.fromDate
            |val until = period?.toDate
            |
            |val filter = TimesheetFilter().also {
            |  it.startTime = from
            |  it.stopTime = until
            |}
            |
            |val users = timesheetDao.select(filter)
            |val workbook = ExcelUtils.prepareWorkbook("Timesheets.xlsx")
            |val sheet = workbook.createOrGetSheet("Timesheets")
            |workbook.createOrGetCellStyle("timeFormat").dataFormat = workbook.createDataFormat().getFormat("HH:mm")
            |workbook.createOrGetCellStyle("dateTimeFormat").dataFormat = workbook.createDataFormat().getFormat("yyyy-MM-dd HH:mm")
            |sheet.registerColumns(
            |  "User|20",
            |  "StartTime|20|:dateTimeFormat",
            |  "StopTime|10|:timeFormat",
            |  "Duration|20",
            |  "Location|30",
            |  "Description|50",
            |  "Task|30",
            |  "TaskPath|30",
            |)
            |sheet.createRow().fillHeadRow()
            |users.forEach { ts ->
            |  val row = sheet.createRow()
            |  ExcelUtils.autoFill(row, ts)
            |  row.getCell("Duration")?.setCellValue(ts.durationAsString)
            |  row.getCell("User")?.setCellValue(ts.user?.getFullname())
            |  row.getCell("Task")?.setCellValue(ts.task?.title)
            |  row.getCell("TaskPath")?.setCellValue(TaskFormatter.getTaskPath(ts.task?.id))
            |  //row.getCell("Duration")?.setCellValue(TaskFormatter.getTaskPath(ts.task))
            |}
            |sheet.setAutoFilter()
            |sheet.createFreezePane(1, 1)
            |
            |workbook
        """.trimMargin()
    }
}
