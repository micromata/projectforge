// First, load all users (not deactivated):
val users = userDao.getList().filter { it.hasSystemAccess() }

val workbook = ExcelUtils.prepareWorkbook("Userlist.xlsx")
val sheet = workbook.createOrGetSheet("list of users")
sheet.registerColumns(
  "Username|20", // Column with length 20
  "Full name|fullname|30", // Column with text 'Full name' and user property fullname.
  "Email|30",
  "Gender|10",
  "Mobile phone|mobilePhone|20",
  "Id|10",
  "JIRA username|JiraUsername|20",
  "Description|50",
)
sheet.createRow().fillHeadRow()
users.forEach { user ->
  val row = sheet.createRow()
  ExcelUtils.autoFill(row, user)
}
sheet.setAutoFilter() // Excel auto filter, a nice option
workbook
