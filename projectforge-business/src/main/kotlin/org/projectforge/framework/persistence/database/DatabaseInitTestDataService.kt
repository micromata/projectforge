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
    scriptDao.internalSaveInTrans(scriptDO)

  }

  companion object {
    private const val exportJFreeChartScript = "import org.jfree.chart.*\n" +
        "import org.jfree.chart.plot.*\n" +
        "import org.jfree.data.general.*\n" +
        "import org.jfree.util.*\n" +
        "import org.projectforge.export.*\n" +
        "\n" +
        "DefaultPieDataset dataset = new DefaultPieDataset()\n" +
        "dataset.setValue(\"Linux\", 15)\n" +
        "dataset.setValue(\"Mac\", 8)\n" +
        "dataset.setValue(\"Windows\", 70)\n" +
        "dataset.setValue(\"Others\", 7)\n" +
        "JFreeChart chart = ChartFactory.createPieChart3D(\"Users on www.heise.de\", dataset, true, true, false)\n" +
        "PiePlot3D plot = (PiePlot3D) chart.getPlot()\n" +
        "\n" +
        "ExportJFreeChart export = new ExportJFreeChart(chart, 800, 600)\n" +
        "return export"

    private const val exportUsersScript = "import org.projectforge.framework.persistence.api.*\n" +
        "import org.projectforge.core.*\n" +
        "import org.projectforge.export.*\n" +
        "\n" +
        "QueryFilter filter = new QueryFilter()         // Defines new query\n" +
        "filter.addOrder(new SortProperty(\"username\"))// Sort\n" +
        "List userList = userDao.getList(filter)        // Gets the user list\n" +
        "\n" +
        "ExportWorkbook workbook = new ExportWorkbook();// Creates a new work book\n" +
        "ExportSheet sheet = workbook.addSheet(\"Users\") // Creates sheet\n" +
        "sheet.contentProvider.colWidths = [10, 20]     // Sets width of columns.\n" +
        "sheet.propertyNames = [\"username\", \"lastname\"] // Defines properties\n" +
        "sheet.addRow().setCapitalizedValues(sheet.propertyNames)  // Adds column heads\n" +
        "sheet.addRows(userList)                        // Fills the rows from the beans\n" +
        "\n" +
        "return workbook"

    private const val exportTimesheets = "import org.projectforge.export.*\n" +
        "import org.projectforge.fibu.*\n" +
        "import org.projectforge.task.*\n" +
        "import org.projectforge.common.*\n" +
        "import org.projectforge.timesheet.*\n" +
        "import org.projectforge.framework.time.*\n" +
        "import java.lang.reflect.*\n" +
        "import org.projectforge.calendar.*\n" +
        "\n" +
        "//Umrechnung Sekunden in Stunden\n" +
        "final BigDecimal coeff = 1/(60*60*1000)\n" +
        "\n" +
        "TimesheetFilter tf = new TimesheetFilter()\n" +
        "tf.setStartTime(timeperiod.fromDate)\n" +
        "tf.setStopTime(timeperiod.toDate)\n" +
        "tf.setTaskId(task.id)\n" +
        "tf.setRecursive(true)\n" +
        "\n" +
        "// Überführung in HashMaps mit relevanten Attributen\n" +
        "lom = timesheetDao.getList(tf).collect{ts -> \t\n" +
        "\n" +
        "\t\t\t\t\t\t\tdef map = new HashMap()\n" +
        "\t\t\t\t\t\t\t\n" +
        "\t\t\t\t\t\t\tPFDay date = PFDay.from(ts?.startTime)\n" +
        "\t\t\t\t\t\t\tmap['date'] = date\n" +
        "\t\t\t\t\t\t\t\n" +
        "\t\t\t\t\t\t\tmap['day'] = date.dayOfMonth\n" +
        "\t\t\t\t\t\t\tmap['month'] = date.month\n" +
        "\t\t\t\t\t\t\tmap['year'] = date.year\n" +
        "\t\t\t\t\t\t\t\n" +
        "\t\t\t\t\t\t\tWeekHolder week = new WeekHolder(date)\n" +
        "\t\t\t\t\t\t\tmap['week'] = week.weekOfYear\n" +
        "\t\t\t\t\t\t\t\n" +
        "\t\t\t\t\t\t\tmap['pd'] =  ts?.duration * coeff\n" +
        "\t\t\t\t\t\t\t\t\t\t\t\n" +
        "\t\t\t\t\t\t\treturn map\n" +
        "\t\t\t\t\t} \n" +
        "\n" +
        "Closure equiv = {x,y -> return x.date.isSameDay(y.date)}\n" +
        "\n" +
        "Closure agg = {x,a -> \ta.lt += x.lt\n" +
        "\t\t\t\t\t\treturn a}\n" +
        "\n" +
        "// Generiere Ergebnisliste\n" +
        "result = GeneralAggregation.aggregate(lom,equiv,{x -> x},agg,{x -> x})\n" +
        "\n" +
        "// Export\n" +
        "ExportWorkbook workbook = new ExportWorkbook();\n" +
        "ExportSheet sheet = workbook.addSheet(\"Working times\")\n" +
        "sheet.contentProvider.colWidths = [10, 10, 10,10, 30, 30, 10]\n" +
        "sheet.addRow().setValues(\"Date\",\"Year\",\"Month\",\"Week\",\"Day\",\"Person days\")\n" +
        "sheet.contentProvider.putFormat(\"date\",\"YYYY-MM-DD\")\n" +
        "sheet.contentProvider.putFormat(\"pd\",\"0.0\")\n" +
        "sheet.contentProvider.putFormat(\"year\",\"0\")\n" +
        "sheet.contentProvider.putFormat(\"month\",\"0\")\n" +
        "sheet.contentProvider.putFormat(\"day\",\"0\")\n" +
        "sheet.propertyNames = [\"date\",\"year\",\"month\",\"week\",\"day\",\"pd\"]\n" +
        "sheet.addRows(result)\n" +
        "\n" +
        "return workbook"
  }

}
