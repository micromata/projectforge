import org.projectforge.framework.time.PFDay

// How to modify existing Excel file:
// val workbook = ExcelWorkbook(files.file, files.filename) // Only one file is uploaded
// val workbook = ExcelWorkbook(files.getFile("MyExcel_template.xlsx"), "MyExcel.xlsx") // Choose file by name

val users = userDao.getList().filter { it.hasSystemAccess() }

// Create new in this example:
val workbook = ExcelUtils.prepareWorkbook("Advanced.xlsx")
val sheet = workbook.createOrGetSheet("list of users")
// Define new number format under name 'integer':
workbook.createOrGetCellStyle("integer").dataFormat = workbook.createDataFormat().getFormat("#0")
sheet.registerColumns(
  "Username|20", // Column with length 20
  "Full name|fullname|30", // Column with text 'Full name' and user property fullname.
  "Email|30",
  "Gender|10",
  "System access|5",
  "Mobile phone|mobilePhone|20",
  "Id|10|:integer", // Use cell style 'integer' for whole column
  "JIRA username|JiraUsername|20",
  "Description|50",
)
sheet.createRow().fillHeadRow()
users.forEach { user ->
  val row = sheet.createRow()
  ExcelUtils.autoFill(row, user)
  row.getCell("System access")?.setCellValue(user.hasSystemAccess()) // hasSystemAccess not supported by auto-fill.
}
sheet.createFreezePane(2, 1) // Freeze first 2 columns and first row of sheet while scrolling
sheet.setAutoFilter() // Excel auto filter, a nice option

val date: LocalDate? = LocalDate.now() // Could be script input value.
val beginOfMonth = PFDay.fromOrNow(date).beginOfMonth // If not given, assume current month
val endOfMonth = beginOfMonth.endOfMonth

var counter = 0
users.forEach() { user ->
  val timesheetFilter = TimesheetFilter()
  timesheetFilter.startTime = beginOfMonth.utilDate
  timesheetFilter.stopTime = endOfMonth.utilDate
  //timesheetFilter.setRecursive(true)
  //timesheetFilter.taskId = taskId (could be input value)
  timesheetFilter.userId = user.id
  val timesheets = timesheetDao.getList(timesheetFilter)
  if (timesheets.isNotEmpty() && counter < 6) {
    ++counter // Get only max 10 time sheets, if used in heavy production systems ;-)
    // val tsSheet = workbook.cloneSheet(0, user.fullname) // Clone sheet of template with new name
    val tsSheet = workbook.createOrGetSheet(user.getFullname())
  }
}

// Further functionalities

// masterRow.copyAndInsert(sheet, currentRow) // Copy master-rows and insert them in the sheet, current row.

// workbook.removeSheetAt(0) // At the end you may remove template sheets

// sheet.setMergedRegion(0, 0, 0, 5) // Merge cells

workbook
