import org.projectforge.business.common.OutputType
import org.projectforge.common.extensions.millisAsFractionOf24h
import org.projectforge.common.extensions.millisAsHours

// Export aller Zeitberichte für ein Jahr, monatsweise in separaten Sheets
// Parameter: startdatum - Startdatum als java.util.Date

// Parse start date parameter (default: start of current year)
val startDate = PFDay.fromOrNow(startdatum).beginOfMonth

// Create workbook
val workbook = ExcelUtils.prepareWorkbook("Timesheets_${startDate.year}.xlsx")

// Create cell styles
val boldFont = ExcelUtils.createFont(workbook, "bold", bold = true)
val boldStyle = workbook.createOrGetCellStyle("hr", font = boldFont)
val wrapTextStyle = workbook.createOrGetCellStyle("wrap")
wrapTextStyle.wrapText = true
val timeFormat = workbook.ensureDateCellStyle("HH:mm")
val durationFormat = workbook.ensureDateCellStyle("[h]:mm")
val hoursFormat = workbook.createOrGetCellStyle("hours")
hoursFormat.dataFormat = workbook.createDataFormat().getFormat("#,##0.00")
val idFormat = workbook.createOrGetCellStyle("id")
idFormat.dataFormat = workbook.createDataFormat().getFormat("0")
val percentageCellStyle = workbook.createOrGetCellStyle("DataFormat.percentage")
percentageCellStyle.dataFormat = workbook.getDataFormat("0%")

// Process 12 months
for (monthOffset in 0..11) {
    val monthStart = startDate.plusMonths(monthOffset.toLong())
    val monthEnd = monthStart.endOfMonth
    val monthLabel = monthStart.format(java.time.format.DateTimeFormatter.ofPattern("MM/yyyy"))

    log.info("Schreibe Monat $monthLabel...")

    // Create filter for this month
    val filter = TimesheetFilter()
    filter.startTime = PFDateTime.from(monthStart.localDate).utilDate
    filter.stopTime = PFDateTime.from(monthEnd.localDate).endOfDay.utilDate
    filter.deleted = false

    // Load timesheets for all users in this month
    val timesheets = timesheetDao.select(filter)

    log.info("Monat $monthLabel mit ${"%,d".format(java.util.Locale.GERMAN, timesheets.size)} Zeitberichten.")

    // Create sheet for this month
    val monthName = monthStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM"))
    val sheet = workbook.createOrGetSheet(monthName)

    // Register columns
    sheet.registerColumn(translate("timesheet.user"), "user").withSize(ExcelUtils.Size.USER)
    sheet.registerColumn(translate("fibu.kunde"), "kunde").withSize(ExcelUtils.Size.STANDARD)
    sheet.registerColumn(translate("fibu.projekt"), "projekt").withSize(ExcelUtils.Size.STANDARD)
    sheet.registerColumn(translate("fibu.kost2"), "kost2").withSize(ExcelUtils.Size.KOSTENTRAEGER)
    sheet.registerColumn(translate("calendar.weekOfYearShortLabel"), "weekOfYearShortLabel").withSize(4)
    sheet.registerColumn(translate("calendar.dayOfWeekShortLabel"), "dayOfWeekShortLabel").withSize(4)
    sheet.registerColumn(translate("timesheet.startTime"), "startTime").withSize(ExcelUtils.Size.TIMESTAMP)
    val stopTimeColDef = sheet.registerColumn(translate("timesheet.stopTime"), "stopTime").withSize(ExcelUtils.Size.TIMESTAMP)
    sheet.registerColumn(translate("timesheet.duration"), "duration").withSize(ExcelUtils.Size.DURATION)
    sheet.registerColumn(translate("hours"), "hours").withSize(ExcelUtils.Size.DURATION)
    if (AITimeSavings.timeSavingsByAIEnabled) {
        sheet.registerColumn(translate("timesheet.ai.timeSavedByAI"), "timeSavedByAI")
            .withSize(ExcelUtils.Size.DURATION)
        sheet.registerColumn(translate("percent"), "timeSavedByAIPercent").withSize(8)
    }
    sheet.registerColumn(translate("timesheet.location"), "location").withSize(ExcelUtils.Size.STANDARD)
    sheet.registerColumn(translate("timesheet.reference"), "reference").withSize(ExcelUtils.Size.STANDARD)
    sheet.registerColumn(translate("task"), "task.title").withSize(ExcelUtils.Size.STANDARD)
    sheet.registerColumn(translate("timesheet.taskReference"), "taskReference")
        .withSize(ExcelUtils.Size.STANDARD)
    sheet.registerColumn(translate("shortDescription"), "shortDescription").withSize(ExcelUtils.Size.EXTRA_LONG)
    val descriptionColDef = sheet.registerColumn(translate("description"), "description").withSize(ExcelUtils.Size.EXTRA_LONG)
    sheet.registerColumn(translate("task.path"), "taskPath").withSize(ExcelUtils.Size.TASK_PATH)
    sheet.registerColumn(translate("id"), "id")
    sheet.registerColumn(translate("created"), "created").withSize(ExcelUtils.Size.TIMESTAMP)
    sheet.registerColumn(translate("lastUpdate"), "lastUpdate").withSize(ExcelUtils.Size.TIMESTAMP)

    // Create header row
    sheet.createRow().fillHeadRow(boldStyle)

    // Fill data rows
    timesheets.forEach { timesheet ->
        val row = sheet.createRow()
        val node = taskTree.getTaskNodeById(timesheet.taskId)
        val user = caches.getUser(timesheet.userId)

        row.getCell("user")?.setCellValue(user?.getFullname())
        row.getCell("kunde")?.setCellValue(timesheet.kost2?.projekt?.kunde?.name)
        row.getCell("projekt")?.setCellValue(timesheet.kost2?.projekt?.name)
        row.getCell("kost2")?.setCellValue(timesheet.kost2?.displayName)
        row.getCell("task.title")?.setCellValue(node?.task?.title)
        row.getCell("taskPath")?.setCellValue(TaskFormatter.getTaskPath(timesheet.taskId, null, true, OutputType.PLAIN))
        row.getCell("weekOfYearShortLabel")?.setCellValue(timesheet.getFormattedWeekOfYear())
        val dayOfWeek = PFDateTime.fromOrNull(timesheet.startTime)
        row.getCell("dayOfWeekShortLabel")?.setCellValue(
            dayOfWeek?.format(DateFormats.getDateTimeFormatter(DateFormatType.DAY_OF_WEEK_SHORT))
        )
        row.getCell("startTime")?.also {
            it.setCellValue(PFDateTime.fromOrNull(timesheet.startTime)?.localDateTime)
            it.setCellStyle(timeFormat)
        }
        row.getCell("stopTime")?.also {
            it.setCellValue(PFDateTime.fromOrNull(timesheet.stopTime)?.localDateTime)
            it.setCellStyle(timeFormat)
        }

        val duration = timesheet.duration
        val excelDuration = duration.millisAsFractionOf24h()
        row.getCell("duration")?.also {
            it.setCellValue(excelDuration.toDouble())
            it.setCellStyle(durationFormat)
        }
        val hours = duration.millisAsHours()
        row.getCell("hours")?.also {
            it.setCellValue(hours.toDouble())
            it.setCellStyle(hoursFormat)
        }

        if (AITimeSavings.timeSavingsByAIEnabled) {
            val aiSaving = AITimeSavings.getTimeSavedByAIMillis(timesheet, duration)
            val aiSavingPercent = AITimeSavings.getFraction(duration, aiSaving)
            row.getCell("timeSavedByAI")?.also {
                it.setCellValue(aiSaving.millisAsHours())
                it.setCellStyle(hoursFormat)
            }
            row.getCell("timeSavedByAIPercent")?.also {
                it.setCellValue(aiSavingPercent)
                it.setCellStyle(percentageCellStyle)
            }
        }

        row.getCell("taskReference")?.setCellValue(node?.reference)
        row.getCell("shortDescription")?.also {
            it.setCellValue(timesheet.getShortDescription())
            it.setCellStyle(wrapTextStyle)
        }
        row.getCell("location")?.setCellValue(timesheet.location)
        row.getCell("reference")?.setCellValue(timesheet.reference)
        row.getCell("description")?.also {
            it.setCellValue(timesheet.description)
            it.setCellStyle(wrapTextStyle)
        }
        row.getCell("id")?.also {
            it.setCellValue(timesheet.id?.toDouble())
            it.setCellStyle(idFormat)
        }
        row.getCell("created")?.setCellValue(timesheet.created)
        row.getCell("lastUpdate")?.setCellValue(timesheet.lastUpdate)
    }

    sheet.setAutoFilter()
    sheet.createFreezePane(stopTimeColDef.columnNumber + 1, 1)
}

workbook
