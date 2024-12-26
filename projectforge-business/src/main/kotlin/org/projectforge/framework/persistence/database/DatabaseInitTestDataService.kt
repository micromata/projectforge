/////////////////////////////////////////////////////////////////////////////
//
// Project ProjectForge Community Edition
//         www.projectforge.org
//
// Copyright (C) 2001-2024 Micromata GmbH, Germany (www.micromata.com)
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
        private  val exportJFreeChartScript = """
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
            |val filter = QueryFilter()         // Defines new query
            |filter.addOrder(SortProperty("username"))// Sort
            |val userList = userDao.select(filter)        // Gets the user list
            |
            |val workbook = ExcelUtils.prepareWorkbook("Users.xlsx")
            |
            |val sheet = workbook.createOrGetSheet("Overview")
            |sheet.registerColumns(
            |    "Username|10",
            |    "Lastname|20",
            |    "Firstname|20",
            |  )
            |sheet.createRow().fillHeadRow(sheet.createOrGetCellStyle("headStyle"))
            |
            |userList.forEach { user ->
            |  sheet.createRow().apply { autoFillFromObject(user) }
            |}
            |
            |workbook
        """.trimMargin()
    }
}
