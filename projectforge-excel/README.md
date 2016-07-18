projectforge-excel
==================

Excel export package (convenient usage of Apache POI) for exporting MS-Excel sheet with a few lines of code or modifiing existing MS-Excel sheets.

Under construction...

## Creating Excel sheets manually
```java
ExportWorkbook workbook = new ExportWorkbook();
ExportSheet sheet = workbook.addSheet("Test");
sheet.getContentProvider().setColWidths(20, 20);
sheet.addRow().setValues("Type", "result");
sheet.addRow().setValues("String", "This is a text.");
sheet.addRow().setValues("int", 1234);
sheet.addRow().setValues("BigDecimal", new BigDecimal("1042.38"));
sheet.addRow().setValues("Date", new Date());
final File file = new File("test-excel.xls");
workbook.write(new FileOutputStream(file));
```

## Creating Excel sheets from bean collections
To be documented...

## Modifiing existing Excel sheets
To be documented...

## Scripting (Groovy as example)
ProjectForge's excel wrapper was designed for a very convenient usage inside Groovy scripts.
To be documented...
```java
List userList = userDao.getList(filter)        // Get the user objects to export somewhere.
xls = reportScriptingStorage.getFile("MyReport.xls") // Get existing xls file for modification.
ExportWorkbook workbook = new ExportWorkbook(xls);// Use this xls file.
ExportSheet sheet = workbook.addSheet("Users") // Adds a new sheet.
sheet.contentProvider.colWidths = [10, 20]     // Sets column widths.
sheet.propertyNames = ["username", "lastname"] // Defines properties of user beans to use.
sheet.addRow().setCapitalizedValues(sheet.propertyNames)  // Add heading row.
sheet.addRows(userList)                        // Add all users to this sheet.
```

## Using POI API directly
You may use the POI API directly for accessing the whole functionality of POI. All wrapped POI elements are directly available:
```java
workbook.getPoiWorkbook();
sheet.getPoiSheet();
row.getPoiRow();
cell.getPoiCell();
```

## Using log4j or other logging frameworks
ProjectForge's excel module uses the Java standard logging as default. If you need log4j
you may initialize it before using continuous-db:
```java
Logger.setLoggerBridge(new LoggerBridgeLog4j()); // Before the first log message
```
You may use any other logging framework if you implement the LoggerBridge yourself.

## Using maven
### pom.xml (stable)
```xml
<dependency>
  <groupId>org.projectforge</groupId>
  <artifactId>projectforge-excel</artifactId>
  <version>5.2</version>
</dependency>

<repository>
  <id>org.projectforge.repo</id>
  <name>ProjectForge</name>
  <url>http://www.projectforge.org/nexus/content/repositories/ProjectForge</url>
</repository>
```

### pom.xml (SNAPSHOT)
```xml
<dependency>
  <groupId>org.projectforge</groupId>
  <artifactId>projectforge-excel</artifactId>
  <version>5.2.1-SNAPSHOT</version>
</dependency>

<repository>
  <id>org.projectforge.repo</id>
  <name>ProjectForge</name>
  <url>http://www.projectforge.org/nexus/content/repositories/ProjectForge-Snapshots</url>
</repository>
```

